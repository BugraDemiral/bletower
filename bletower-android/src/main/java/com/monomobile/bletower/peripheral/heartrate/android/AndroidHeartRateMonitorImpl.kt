package com.monomobile.bletower.peripheral.heartrate.android

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import com.monomobile.bletower.BaseMonitorEvent
import com.monomobile.bletower.HeartRateMonitorEvent
import com.monomobile.bletower.monitor.android.AndroidBaseMonitorImpl
import com.monomobile.bletower.peripheral.heartrate.HeartRateMonitor
import com.monomobile.bletower.peripheral.heartrate.HeartRateUUID
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber
import java.util.UUID

@SuppressLint("MissingPermission")
class AndroidHeartRateMonitorImpl(
    context: Context,
    scope: CoroutineScope)
    :  AndroidBaseMonitorImpl(context, scope), HeartRateMonitor {
    companion object {
        const val TAG = "AndroidHeartRateMonitorImpl"
    }

    override fun onCustomCharacteristicRead(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ): BaseMonitorEvent {
        return try {
            when (characteristic.uuid) {
                UUID.fromString(HeartRateUUID.MEASUREMENT) -> {
                    val heartRate = parseData(value)
                    HeartRateMonitorEvent.HeartRateRead(Result.success(heartRate))
                }
                UUID.fromString(HeartRateUUID.BATTERY_LEVEL) -> {
                    if (value.isNotEmpty()) {
                        val batteryLevel = parseData(value)
                        BaseMonitorEvent.BatteryLevelRead(Result.success(batteryLevel))
                    } else {
                        BaseMonitorEvent.BatteryLevelRead(Result.failure(Exception("Invalid data")))
                    }
                }
                UUID.fromString(HeartRateUUID.BODY_SENSOR_LOCATION) -> {
                    val sensorLocation = parseSensorLocation(value)
                    HeartRateMonitorEvent.SensorLocationRead(Result.success(sensorLocation))
                }
                else -> BaseMonitorEvent.Unknown(characteristic.uuid.toString())
            }
        } catch (e: Exception) {
            // Log error and return an appropriate event
            Timber.tag(TAG).e("Error reading characteristic ${characteristic.uuid}")
            BaseMonitorEvent.Unknown(characteristic.uuid.toString())
        }
    }

    private fun parseData(data: ByteArray): Int {
        return if (data.isNotEmpty()) {
            data[0].toInt() and 0xFF
        } else {
            Timber.tag(TAG).w("Invalid heart rate data: ${data.joinToString()}")
            0
        }
    }

    private fun parseSensorLocation(data: ByteArray): String {
        return if (data.isNotEmpty()) {
            when (data[0].toInt()) {
                0 -> "Other"
                1 -> "Chest"
                2 -> "Wrist"
                3 -> "Finger"
                4 -> "Hand"
                5 -> "Ear Lobe"
                6 -> "Foot"
                else -> "Unknown"
            }
        } else {
            Timber.tag(TAG).w("Invalid sensor location data: ${data.joinToString()}")
            "Unknown"
        }
    }

    override fun startObservingMeasurement() {
        observeCharacteristic(
            UUID.fromString(HeartRateUUID.BATTERY_SERVICE),
            UUID.fromString(HeartRateUUID.MEASUREMENT)
        )
    }

    override fun startObservingBattery() {
        observeCharacteristic(
            UUID.fromString(HeartRateUUID.BATTERY_SERVICE),
            UUID.fromString(HeartRateUUID.BATTERY_LEVEL)
        )
    }

    override fun readMeasurement() {
        readCharacteristic(
            UUID.fromString(HeartRateUUID.SERVICE),
            UUID.fromString(HeartRateUUID.MEASUREMENT)
        )
    }

    override fun readBatteryLevel() {
        readCharacteristic(
            UUID.fromString(HeartRateUUID.SERVICE),
            UUID.fromString(HeartRateUUID.MEASUREMENT)
        )
    }

    override fun readSensorLocation() {
        readCharacteristic(
            UUID.fromString(HeartRateUUID.SERVICE),
            UUID.fromString(HeartRateUUID.MEASUREMENT)
        )
    }

    override fun resetEnergyExpended() {
        writeCharacteristic(
            UUID.fromString(HeartRateUUID.SERVICE),
            UUID.fromString(HeartRateUUID.CONTROL_POINT),
            byteArrayOf(0x01)
        )
    }
}