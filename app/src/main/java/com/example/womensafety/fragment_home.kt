package com.example.womensafety

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var tvProfileName: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        // Initialize views
        tvProfileName = view.findViewById(R.id.tvProfileName)

        // Retrieve the user's name from SharedPreferences
        val userName = sharedPreferences.getString("name", "User") // Default to "User" if name is not found

        // Set the welcome message with the user's name
        tvProfileName.text = "Welcome, $userName!"

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Fetch Location Button
        val btnFetchLocation = view.findViewById<Button>(R.id.btnFetchLocation)
        btnFetchLocation.setOnClickListener {
            checkLocationEnabled()
            fetchCurrentLocation()
        }

        // SOS Button
        val btnSOS = view.findViewById<Button>(R.id.btnSOS)
        btnSOS.setOnClickListener {
            checkPermissionsAndSendSOS()
        }

        // Fake Call Button
        val btnFakeCall = view.findViewById<Button>(R.id.btnFakeCall)
        btnFakeCall.setOnClickListener {
            makeFakeCall()
        }

        // Emergency Contacts Button
        val btnEmergencyContacts = view.findViewById<Button>(R.id.btnEmergencyContacts)
        btnEmergencyContacts.setOnClickListener {
            val intent = Intent(requireContext(), ViewEmergencyContactsActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    private fun checkLocationEnabled() {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Prompt the user to enable location services
            Toast.makeText(requireContext(), "Please enable location services", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
    }

    private fun fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    openGoogleMaps(location)
                } else {
                    // Fallback to last known location
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                        if (lastLocation != null) {
                            openGoogleMaps(lastLocation)
                        } else {
                            Toast.makeText(requireContext(), "Location not available", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to get location: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openGoogleMaps(location: Location) {
        val uri = Uri.parse("geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps") // Ensure it opens in Google Maps
        startActivity(intent)
    }

    private fun checkPermissionsAndSendSOS() {
        // Check for location and SMS permissions
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permissions granted, fetch location and send SOS
            fetchLocationAndSendSOS()
        } else {
            // Request permissions
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.SEND_SMS
                ),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun fetchLocationAndSendSOS() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Fetch the current location
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    // Convert location to address
                    val address = getAddressFromLocation(location)
                    if (address != null) {
                        // Send SOS with detailed location and Google Maps link
                        val googleMapsLink = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
                        val message = "Help! I am in danger! My location:\n$address\nGoogle Maps: $googleMapsLink"
                        sendSMS(message)
                    } else {
                        Toast.makeText(requireContext(), "Unable to fetch address", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Unable to fetch location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to get location: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getAddressFromLocation(location: Location): String? {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        return try {
            val addresses: List<Address>? = geocoder.getFromLocation(
                location.latitude,
                location.longitude,
                1
            )
            if (!addresses.isNullOrEmpty()) {
                val address: Address = addresses[0]
                // Build a detailed address string
                val sb = StringBuilder()
                for (i in 0..address.maxAddressLineIndex) {
                    sb.append(address.getAddressLine(i)).append("\n")
                }
                sb.toString()
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun sendSMS(message: String) {
        // Retrieve emergency contacts from SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("EmergencyContactsPrefs", Context.MODE_PRIVATE)
        val contactsJson = sharedPreferences.getString("contacts", null)
        if (contactsJson != null) {
            val type = object : TypeToken<List<Contact>>() {}.type
            val contacts = Gson().fromJson<List<Contact>>(contactsJson, type)

            // Send SMS to each emergency contact
            val smsManager = SmsManager.getDefault()
            for (contact in contacts) {
                smsManager.sendTextMessage(contact.phone, null, message, null, null)
            }

            Toast.makeText(requireContext(), "SOS sent to emergency contacts!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "No emergency contacts found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun makeFakeCall() {
        val fakeNumber = "0123456789" // Replace with a customizable number
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:$fakeNumber")

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 2)
            return
        }

        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permissions granted, try fetching location again
                    fetchCurrentLocation()
                } else {
                    Toast.makeText(requireContext(), "Permissions denied", Toast.LENGTH_SHORT).show()
                }
            }
            2 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, try making fake call again
                    makeFakeCall()
                } else {
                    Toast.makeText(requireContext(), "Permissions denied", Toast.LENGTH_SHORT).show()
                }
            }
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED
                ) {
                    // Permissions granted, fetch location and send SOS
                    fetchLocationAndSendSOS()
                } else {
                    Toast.makeText(requireContext(), "Permissions denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
}