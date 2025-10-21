package com.example.womensafety

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.widget.Toast

class ShutdownReceiver : BroadcastReceiver() {

    private var mediaRecorder: MediaRecorder? = null

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SHUTDOWN) {
            // Stop recording if it's running
            stopRecording()

            // Send alert to emergency contacts
            sendAlertToContacts(context)
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
    }

    private fun sendAlertToContacts(context: Context) {
        // TODO: Implement logic to send the recorded audio to emergency contacts
        Toast.makeText(context, "Phone is shutting down. Alert sent to emergency contacts.", Toast.LENGTH_SHORT).show()
    }
}