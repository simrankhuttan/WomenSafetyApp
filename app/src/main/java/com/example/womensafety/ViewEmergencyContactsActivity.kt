package com.example.womensafety

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.womensafety.models.Contact
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ViewEmergencyContactsActivity : AppCompatActivity() {

    private lateinit var tvContactList: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_emergency_contacts)

        tvContactList = findViewById(R.id.tvContactList)

        // Retrieve and display contacts
        displayContacts()
    }

    private fun displayContacts() {
        val sharedPreferences = getSharedPreferences("EmergencyContactsPrefs", Context.MODE_PRIVATE)
        val contactsJson = sharedPreferences.getString("contacts", null)

        if (contactsJson != null) {
            val type = object : TypeToken<List<Contact>>() {}.type
            val contacts = Gson().fromJson<List<Contact>>(contactsJson, type)

            val contactsText = StringBuilder()
            for (contact in contacts) {
                contactsText.append("Name: ${contact.name}\nPhone: ${contact.phone}\nEmail: ${contact.email}\n\n")
            }

            tvContactList.text = contactsText.toString()
        } else {
            tvContactList.text = "No emergency contacts saved."
        }
    }
}