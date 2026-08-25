package com.rork.ghostdetectorspiritbox.services

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

/** Availability of the device magnetometer, reported honestly on the idle panel. */
enum class SensorStatus { ONLINE, UNAVAILABLE }

/**
 * Real magnetometer access. Emits the magnitude of the ambient magnetic field in µT.
 * No values are ever synthesised: if the hardware is missing, nothing is emitted.
 */
class MagnetometerSource(context: Context) {

    private val appContext = context.applicationContext
    private val sensorManager: SensorManager? =
        appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val sensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    val status: SensorStatus
        get() = if (sensor != null) SensorStatus.ONLINE else SensorStatus.UNAVAILABLE

    /** Cold flow of field magnitude readings in µT. */
    fun readings(): Flow<Double> = callbackFlow {
        val manager = sensorManager
        val device = sensor
        if (manager == null || device == null) {
            awaitClose { }
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.values.size < 3) return
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt((x * x + y * y + z * z).toDouble())
                trySend(magnitude)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, device, SensorManager.SENSOR_DELAY_UI)
        awaitClose { manager.unregisterListener(listener) }
    }
}
