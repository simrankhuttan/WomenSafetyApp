package com.example.womensafety
import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val PREFS_NAME = "EmergencyContactsPrefs"
        private const val CONTACTS_KEY = "contacts"
        private const val PERMISSION_REQUEST_CODE = 101
        private const val MIN_PHONE_LENGTH = 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        Log.d("MainActivity", "SharedPreferences initialized at: ${getSharedPreferencesPath()}")

        // Check & Request Permissions
        checkAndRequestPermissions()

        // Initialize Location Services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize Shake Detection
        initShakeDetection()

        // Initialize Speech Recognition
        initVoiceRecognition()

        // Bottom Navigation
        setupBottomNavigation()
    }

    private fun getSharedPreferencesPath(): String {
        return filesDir.parent + "/shared_prefs/$PREFS_NAME.xml"
    }

    private fun initShakeDetection() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector(3) { showEmergencyPopup() }

        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer != null) {
            sensorManager.registerListener(
                shakeDetector,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
            )
            Log.d("ShakeDetection", "Shake detector initialized successfully")
        } else {
            Log.e("ShakeDetection", "Accelerometer not available")
            Toast.makeText(this, "Shake detection not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initVoiceRecognition() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        Log.d("VoiceRecognition", "SpeechRecognizer initialized")
        startVoiceRecognition()
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
            replaceFragment(HomeFragment())
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment())
                R.id.nav_map -> replaceFragment(MapFragment())
                R.id.nav_camera -> replaceFragment(CameraFragment())
                R.id.nav_profile -> replaceFragment(ProfileFragment())
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun startVoiceRecognition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w("VoiceRecognition", "Audio permission not granted")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("VoiceRecognition", "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d("VoiceRecognition", "Speech beginning detected")
            }

            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                    Log.d("VoiceRecognition", "Heard: ${matches.joinToString()}")
                    matches.firstOrNull { word ->
                        word.equals("help me", ignoreCase = true) || word.equals("sos", ignoreCase = true)
                    }?.let {
                        Log.d("VoiceRecognition", "Trigger word detected: $it")
                        triggerEmergencyProtocol()
                    }
                }
                startVoiceRecognition()
            }

            override fun onError(error: Int) {
                Log.e("VoiceRecognition", "Recognition error: $error")
                startVoiceRecognition()
            }

            // Other required empty overrides
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            speechRecognizer.startListening(intent)
            Log.d("VoiceRecognition", "Started listening")
        } catch (e: Exception) {
            Log.e("VoiceRecognition", "Failed to start listening: ${e.message}")
        }
    }

    private fun checkAndRequestPermissions() {
        if (!hasRequiredPermissions()) {
            Log.d("Permissions", "Requesting missing permissions")
            requestPermissions()
        } else {
            Log.d("Permissions", "All permissions already granted")
        }
    }

    private fun triggerEmergencyProtocol() {
        Log.d("Emergency", "Emergency protocol triggered")
        if (hasRequiredPermissions()) {
            getLocationAndSendAlert()
        } else {
            Log.w("Emergency", "Missing permissions for emergency protocol")
            requestPermissions()
        }
    }

    private fun getLocationAndSendAlert() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            Log.w("Location", "Location permission not granted")
            return
        }

        Log.d("Location", "Attempting to get location")
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                Log.d("Location", "Location obtained: ${location.latitude}, ${location.longitude}")
                val address = getAddressFromLocation(location.latitude, location.longitude)
                val mapLink = "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
                val message = """
                    ⚠ EMERGENCY! I need help!
                    My current location: $address
                    Map Link: $mapLink
                    Latitude: ${location.latitude}
                    Longitude: ${location.longitude}
                    """.trimIndent()
                sendEmergencySMS(message)
            } else {
                Log.e("Location", "Could not get location")
                Toast.makeText(this, "Could not get location", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Log.e("Location", "Location error: ${e.message}")
            Toast.makeText(this, "Location error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendEmergencySMS(message: String) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED) {
            Log.w("SMS", "SMS permission not granted")
            Toast.makeText(this, "SMS permission required", Toast.LENGTH_SHORT).show()
            return
        }

        val contacts = getEmergencyContacts()
        if (contacts.isEmpty()) {
            Log.w("SMS", "No valid emergency contacts available")
            Toast.makeText(this, "No valid emergency contacts found", Toast.LENGTH_LONG).show()
            return
        }

        Log.d("SMS", "Attempting to send to ${contacts.size} contacts")
        val smsManager = SmsManager.getDefault()
        var successCount = 0

        contacts.forEach { phone ->
            try {
                smsManager.sendTextMessage(phone, null, message, null, null)
                successCount++
                Log.d("SMS", "Successfully sent to $phone")
            } catch (e: Exception) {
                Log.e("SMS", "Failed to send to $phone: ${e.message}")
            }
        }

        val resultMessage = when {
            successCount == contacts.size -> "SOS sent to all $successCount contacts"
            successCount > 0 -> "SOS sent to $successCount of ${contacts.size} contacts"
            else -> "Failed to send to any contacts"
        }

        Log.d("SMS", resultMessage)
        Toast.makeText(this, resultMessage, Toast.LENGTH_LONG).show()
    }

    private fun getEmergencyContacts(): List<String> {
        return try {
            val contactsJson = sharedPreferences.getString(CONTACTS_KEY, null)

            if (contactsJson.isNullOrEmpty()) {
                Log.d("Contacts", "No contacts found in SharedPreferences")
                Toast.makeText(this, "No emergency contacts saved", Toast.LENGTH_SHORT).show()
                return emptyList()
            }

            val type = object : TypeToken<List<Contact>>() {}.type
            val contacts = Gson().fromJson<List<Contact>>(contactsJson, type) ?: run {
                Log.e("Contacts", "Parsed contacts list is null")
                return emptyList()
            }

            Log.d("Contacts", "Retrieved ${contacts.size} contacts from storage")

            contacts
                .filter { contact ->
                    contact.phone.isNotBlank() && contact.isValidPhone()
                }
                .map { contact ->
                    contact.getFormattedPhone().also { phone ->
                        Log.d("Contacts", "Formatted phone: $phone")
                    }
                }
                .filter { formattedPhone ->
                    formattedPhone.length >= MIN_PHONE_LENGTH
                }
                .also { validContacts ->
                    Log.d("Contacts", "Found ${validContacts.size} valid contacts")
                }
        } catch (e: Exception) {
            Log.e("Contacts", "Error parsing contacts: ${e.message}", e)
            Toast.makeText(
                this,
                "Error loading contacts: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
            emptyList()
        }
    }

    private fun getAddressFromLocation(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.let { address ->
                "${address.getAddressLine(0)}, ${address.locality}, ${address.adminArea}, ${address.countryName}"
            } ?: "Unknown Location".also {
                Log.w("Geocoder", "No address found for coordinates")
            }
        } catch (e: Exception) {
            Log.e("Geocoder", "Geocoding error: ${e.message}")
            "Location (${"%.4f".format(latitude)}, ${"%.4f".format(longitude)})"
        }
    }

    private fun showEmergencyPopup() {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle("Emergency Alert!")
            .setMessage("You shook your phone forcefully. Do you need help?")
            .setPositiveButton("Yes, Send SOS") { _, _ ->
                Log.d("Emergency", "User confirmed emergency")
                triggerEmergencyProtocol()
            }
            .setNegativeButton("No") { dialog, _ ->
                Log.d("Emergency", "User canceled emergency")
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                        ContextCompat.getColor(this@MainActivity, android.R.color.black))
                    getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                        ContextCompat.getColor(this@MainActivity, android.R.color.black))
                }
                show()
            }
    }

    private fun hasRequiredPermissions(): Boolean {
        val hasSms = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val hasLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        Log.d("Permissions", "SMS: $hasSms, Location: $hasLocation, Audio: $hasAudio")
        return hasSms && hasLocation && hasAudio
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS
        )
        Log.d("Permissions", "Requesting permissions: ${permissions.joinToString()}")
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            Log.d("Permissions", "Permission result: ${if (allGranted) "GRANTED" else "DENIED"}")

            if (allGranted) {
                initVoiceRecognition()
                initShakeDetection()
            } else {
                Toast.makeText(this,
                    "Some features may not work without permissions",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        sensorManager.unregisterListener(shakeDetector)
        Log.d("MainActivity", "Activity destroyed and resources cleaned up")
    }
}

data class Contact(
    val name: String = "",
    val phone: String = "",
    val email: String = ""
) {
    fun getFormattedPhone(): String {
        return when {
            phone.isBlank() -> ""
            phone.startsWith("+") -> phone
            else -> "+91$phone" // Default to India code
        }
    }

    fun isValidPhone(): Boolean {
        val digitsOnly = phone.filter { it.isDigit() }
        return digitsOnly.length in 10..15 && digitsOnly.toLongOrNull() != null
    }
}