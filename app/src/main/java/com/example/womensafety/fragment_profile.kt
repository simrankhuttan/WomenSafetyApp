package com.example.womensafety

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.io.IOException

class ProfileFragment : Fragment() {

    private lateinit var profileImage: ImageView
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var btnChangeImage: Button
    private lateinit var btnSave: Button
    private lateinit var btnLogout: Button
    private lateinit var btnFeedback: Button
    private lateinit var btnManageContacts: Button
    private lateinit var sharedPreferences: SharedPreferences

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.data?.let { uri ->
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
                    profileImage.setImageBitmap(bitmap)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val bitmap = data?.extras?.get("data") as? Bitmap
            bitmap?.let {
                profileImage.setImageBitmap(it)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Initialize views
        profileImage = view.findViewById(R.id.profile_image)
        etName = view.findViewById(R.id.et_name)
        etEmail = view.findViewById(R.id.et_email)
        etPhone = view.findViewById(R.id.et_phone)
        btnChangeImage = view.findViewById(R.id.btn_change_image)
        btnSave = view.findViewById(R.id.btn_save)
        btnLogout = view.findViewById(R.id.btn_logout)
        btnFeedback = view.findViewById(R.id.btnFeedback)
        btnManageContacts = view.findViewById(R.id.btn_manage_contacts)

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        // Load user data
        loadUserData()

        // Apply restrictions to EditText fields
        applyEditTextRestrictions()

        // Handle Change Profile Picture button
        btnChangeImage.setOnClickListener {
            checkPermissionsAndOpenImagePicker()
        }

        // Handle Save Changes button
        btnSave.setOnClickListener {
            saveUserData()
        }

        // Handle Logout button
        btnLogout.setOnClickListener {
            logoutUser()
        }

        // Handle Feedback button
        btnFeedback.setOnClickListener {
            sendFeedback()
        }

        // Handle Manage Emergency Contacts button
        btnManageContacts.setOnClickListener {
            val intent = Intent(requireContext(), EmergencyContactsActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    private fun loadUserData() {
        val name = sharedPreferences.getString("name", "")
        val email = sharedPreferences.getString("email", "")
        val phone = sharedPreferences.getString("phone", "")

        etName.setText(name)
        etEmail.setText(email)
        etPhone.setText(phone)
    }

    private fun saveUserData() {
        val name = etName.text.toString()
        val email = etEmail.text.toString()
        val phone = etPhone.text.toString()

        if (name.isNotEmpty() && email.isNotEmpty() && phone.isNotEmpty()) {
            val editor = sharedPreferences.edit()
            editor.putString("name", name)
            editor.putString("email", email)
            editor.putString("phone", phone)
            editor.apply()

            Toast.makeText(requireContext(), "Changes saved successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
        }
    }

    private fun logoutUser() {
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                val editor = sharedPreferences.edit()
                editor.clear()
                editor.apply()

                val intent = Intent(requireContext(), LoginActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.setOnShowListener {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(android.R.color.black))
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(resources.getColor(android.R.color.black))
        }

        alertDialog.show()
    }

    private fun sendFeedback() {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:youremail@example.com") // Replace with your email
            putExtra(Intent.EXTRA_SUBJECT, "Feedback / Suggestions for Women Safety App")
            putExtra(Intent.EXTRA_TEXT, "Dear Developer,\n\nI would like to suggest the following changes:\n\n")
        }

        try {
            startActivity(Intent.createChooser(emailIntent, "Send feedback via email"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissionsAndOpenImagePicker() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE),
                1
            )
        } else {
            openImagePicker()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openImagePicker()
        } else {
            Toast.makeText(requireContext(), "Permissions denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyEditTextRestrictions() {
        // Name: Restrict to 30 words
        etName.filters = arrayOf(WordLimitFilter(30))

        // Phone: Restrict to 10 digits
        etPhone.inputType = InputType.TYPE_CLASS_PHONE
        etPhone.filters = arrayOf(InputFilter.LengthFilter(10))
        etPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s?.length != 10) {
                    etPhone.error = "Phone number must be 10 digits"
                } else {
                    etPhone.error = null
                }
            }
        })

        // Email: Automatically append "@gmail.com"
        etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val email = s?.toString() ?: ""
                if (!email.endsWith("@gmail.com") && email.isNotEmpty() && !email.contains('@')) {
                    etEmail.removeTextChangedListener(this)
                    etEmail.setText("$email@gmail.com")
                    etEmail.setSelection(email.length)
                    etEmail.addTextChangedListener(this)
                }
            }
        })
    }

    // Custom InputFilter to limit words
    class WordLimitFilter(private val maxWords: Int) : InputFilter {
        override fun filter(
            source: CharSequence, start: Int, end: Int,
            dest: Spanned, dstart: Int, dend: Int
        ): CharSequence? {
            val currentWords = dest.split("\\s+".toRegex()).size
            val newWords = source.split("\\s+".toRegex()).size

            if (currentWords + newWords > maxWords) {
                return ""
            }
            return null
        }
    }
}