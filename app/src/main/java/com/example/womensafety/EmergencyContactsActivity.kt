package com.example.womensafety

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.reflect.TypeToken

import com.example.womensafety.models.Contact
import com.google.gson.Gson

class EmergencyContactsActivity : AppCompatActivity() {

    private lateinit var etEmergencyName1: EditText
    private lateinit var etEmergencyPhone1: EditText
    private lateinit var etEmergencyEmail1: EditText

    private lateinit var etEmergencyName2: EditText
    private lateinit var etEmergencyPhone2: EditText
    private lateinit var etEmergencyEmail2: EditText

    private lateinit var etEmergencyName3: EditText
    private lateinit var etEmergencyPhone3: EditText
    private lateinit var etEmergencyEmail3: EditText

    private lateinit var btnSaveContacts: Button
    private lateinit var btnEditContacts: Button

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_contacts)

        // Initialize views
        etEmergencyName1 = findViewById(R.id.et_emergency_name1)
        etEmergencyPhone1 = findViewById(R.id.et_emergency_phone1)
        etEmergencyEmail1 = findViewById(R.id.et_emergency_email1)

        etEmergencyName2 = findViewById(R.id.et_emergency_name2)
        etEmergencyPhone2 = findViewById(R.id.et_emergency_phone2)
        etEmergencyEmail2 = findViewById(R.id.et_emergency_email2)

        etEmergencyName3 = findViewById(R.id.et_emergency_name3)
        etEmergencyPhone3 = findViewById(R.id.et_emergency_phone3)
        etEmergencyEmail3 = findViewById(R.id.et_emergency_email3)

        btnSaveContacts = findViewById(R.id.btn_save_contacts)
        btnEditContacts = findViewById(R.id.btn_edt_contacts)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("EmergencyContactsPrefs", Context.MODE_PRIVATE)

        // Enable EditText fields by default
        setEditTextEnabled(true)

        // Load saved contacts
        loadEmergencyContacts()

        // Handle Save Contacts button
        btnSaveContacts.setOnClickListener {
            if (validateContacts()) {
                saveEmergencyContacts()
                Toast.makeText(this, "Contacts saved successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please fill all fields correctly!", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle Edit Contacts button
        btnEditContacts.setOnClickListener {
            setEditTextEnabled(true) // Enable editing
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload saved contacts
        loadEmergencyContacts()
    }

    private fun loadEmergencyContacts() {
        val sharedPreferences = getSharedPreferences("EmergencyContactsPrefs", Context.MODE_PRIVATE)
        val contactsJson = sharedPreferences.getString("contacts", null)

        if (contactsJson != null) {
            val type = object : TypeToken<List<Contact>>() {}.type
            val contacts = Gson().fromJson<List<Contact>>(contactsJson, type)

            // Update EditText fields with the loaded contacts
            if (contacts.isNotEmpty()) {
                etEmergencyName1.setText(contacts[0].name)
                etEmergencyPhone1.setText(contacts[0].phone)
                etEmergencyEmail1.setText(contacts[0].email)

                if (contacts.size > 1) {
                    etEmergencyName2.setText(contacts[1].name)
                    etEmergencyPhone2.setText(contacts[1].phone)
                    etEmergencyEmail2.setText(contacts[1].email)
                }

                if (contacts.size > 2) {
                    etEmergencyName3.setText(contacts[2].name)
                    etEmergencyPhone3.setText(contacts[2].phone)
                    etEmergencyEmail3.setText(contacts[2].email)
                }
            }
        }
    }

    private fun saveEmergencyContacts() {
        val editor = sharedPreferences.edit()

        // Create a list of contacts
        val contacts = listOf(
            Contact(etEmergencyName1.text.toString(), etEmergencyPhone1.text.toString(), etEmergencyEmail1.text.toString()),
            Contact(etEmergencyName2.text.toString(), etEmergencyPhone2.text.toString(), etEmergencyEmail2.text.toString()),
            Contact(etEmergencyName3.text.toString(), etEmergencyPhone3.text.toString(), etEmergencyEmail3.text.toString())
        )

        // Convert the list to JSON
        val contactsJson = Gson().toJson(contacts)

        // Save the JSON string
        editor.putString("contacts", contactsJson)
        editor.apply()
    }

    private fun setEditTextEnabled(enabled: Boolean) {
        etEmergencyName1.isEnabled = enabled
        etEmergencyPhone1.isEnabled = enabled
        etEmergencyEmail1.isEnabled = enabled

        etEmergencyName2.isEnabled = enabled
        etEmergencyPhone2.isEnabled = enabled
        etEmergencyEmail2.isEnabled = enabled

        etEmergencyName3.isEnabled = enabled
        etEmergencyPhone3.isEnabled = enabled
        etEmergencyEmail3.isEnabled = enabled
    }

    private fun validateContacts(): Boolean {
        return etEmergencyName1.text.isNotBlank() && etEmergencyPhone1.text.isNotBlank() && etEmergencyEmail1.text.isNotBlank() &&
                etEmergencyName2.text.isNotBlank() && etEmergencyPhone2.text.isNotBlank() && etEmergencyEmail2.text.isNotBlank() &&
                etEmergencyName3.text.isNotBlank() && etEmergencyPhone3.text.isNotBlank() && etEmergencyEmail3.text.isNotBlank()
    }
}