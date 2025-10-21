package com.example.womensafety

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class ShakeDetector(
    private val shakeThreshold: Int,
    private val onShake: () -> Unit
) : SensorEventListener {

    private var lastUpdate: Long = 0
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastZ: Float = 0f
    private var shakeCount = 0
    private var lastShakeTime: Long = 0

    override fun onSensorChanged(event: SensorEvent) {
        val currentTime = System.currentTimeMillis()
        val timeDifference = currentTime - lastUpdate

        if (timeDifference > 100) { // 100ms
            val diffTime = currentTime - lastShakeTime
            if (diffTime > 1000) { // Reset if last shake was more than 1 second ago
                shakeCount = 0
            }

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val speed = Math.abs(x + y + z - lastX - lastY - lastZ) / timeDifference * 10000

            if (speed > 800) { // Adjust this value for sensitivity
                shakeCount++
                if (shakeCount >= shakeThreshold) {
                    onShake()
                    shakeCount = 0
                }
                lastShakeTime = currentTime
            }

            lastX = x
            lastY = y
            lastZ = z
            lastUpdate = currentTime
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}