package com.example.womensafety

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.content.Context.MODE_PRIVATE

class SplashScreen : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        Handler(Looper.getMainLooper()).postDelayed({
            checkUserLoginStatus()
        }, 3000)
    }

    private fun checkUserLoginStatus() {
        val email = sharedPreferences.getString("email", "")
        val password = sharedPreferences.getString("password", "")

        if (email.isNullOrEmpty() || password.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}
