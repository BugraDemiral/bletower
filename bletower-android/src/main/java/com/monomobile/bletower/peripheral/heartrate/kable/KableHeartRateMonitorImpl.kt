package com.monomobile.bletower.peripheral.heartrate.kable

import com.juul.kable.characteristicOf
import com.monomobile.bletower.BaseMonitorEvent
import com.monomobile.bletower.HeartRateMonitorEvent
import com.monomobile.bletower.monitor.kable.KableBaseMonitorImpl
import com.monomobile.bletower.peripheral.heartrate.HeartRateMonitor
import com.monomobile.bletower.peripheral.heartrate.HeartRateUUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class KableHeartRateMonitorImpl(private val scope: CoroutineScope)
    :  KableBaseMonitorImpl(scope), HeartRateMonitor {
    companion object {
        const val TAG = "KableHeartRateMonitorImpl"
    }

    override fun startObservingMeasurement() {
        val heartRateCharacteristic = characteristicOf(
            service = HeartRateUUID.SERVICE,
            characteristic = HeartRateUUID.MEASUREMENT
        )

        peripheral?.observe(heartRateCharacteristic)
            ?.onEach { data ->
                val heartRate = parseData(data)
                monitoringEventsFlow.emit(
                    HeartRateMonitorEvent.HeartRateRead(
                        Result.success(heartRate)
                    )
                )
            }
            ?.launchIn(scope)
    }

    override fun startObservingBattery() {
        val batteryCharacteristic = characteristicOf(
            service = HeartRateUUID.BATTERY_SERVICE,
            characteristic = HeartRateUUID.BATTERY_LEVEL
        )

        peripheral?.observe(batteryCharacteristic)
            ?.onEach { data ->
                val batteryLevel = parseData(data)
                monitoringEventsFlow.emit(
                    BaseMonitorEvent.BatteryLevelRead(
                        Result.success(batteryLevel)
                    )
                )
            }
            ?.launchIn(scope)
    }

    private fun parseData(data: ByteArray): Int {
        return if (data.isNotEmpty()) {
            data[0].toInt() and 0xFF
        } else {
            0
        }
    }

    override fun readMeasurement() {
        scope.launch {
            peripheral?.let {
                try {
                    val heartRateCharacteristic = characteristicOf(
                        service = HeartRateUUID.SERVICE,
                        characteristic = HeartRateUUID.MEASUREMENT
                    )
                    val data = it.read(heartRateCharacteristic)
                    val heartRate = parseData(data)

                    monitoringEventsFlow.emit(
                        HeartRateMonitorEvent.HeartRateRead(
                            Result.success(heartRate)
                        )
                    )
                } catch (e: Exception) {
                    monitoringEventsFlow.emit(
                        HeartRateMonitorEvent.HeartRateRead(
                            Result.failure(e)
                        )
                    )
                }
            } ?: run {
                monitoringEventsFlow.emit(
                    HeartRateMonitorEvent.HeartRateRead(
                            Result.failure(
                                IllegalStateException("Peripheral is not connected")
                            )
                        )
                )
            }
        }
    }

    override fun readBatteryLevel() {
        scope.launch {
            peripheral?.let {
                try {
                    val batteryCharacteristic = characteristicOf(
                        service = HeartRateUUID.BATTERY_SERVICE,
                        characteristic = HeartRateUUID.BATTERY_LEVEL
                    )
                    val data = it.read(batteryCharacteristic)
                    val batteryLevel = parseData(data)

                    monitoringEventsFlow.emit(
                        BaseMonitorEvent.BatteryLevelRead(
                            Result.success(batteryLevel)
                        )
                    )
                } catch (e: Exception) {
                    monitoringEventsFlow.emit(
                        BaseMonitorEvent.BatteryLevelRead(
                            Result.failure(e)
                        )
                    )
                }
            } ?: run {
                monitoringEventsFlow.emit(
                    BaseMonitorEvent
                        .BatteryLevelRead(
                            Result.failure(
                                IllegalStateException("Peripheral is not connected")
                            )
                        )
                )
            }
        }
    }

    override fun readSensorLocation() {
        scope.launch {
            peripheral?.let {
                try {
                    val sensorLocationCharacteristic = characteristicOf(
                        service = HeartRateUUID.SERVICE,
                        characteristic = HeartRateUUID.BODY_SENSOR_LOCATION
                    )
                    val data = it.read(sensorLocationCharacteristic)

                    monitoringEventsFlow.emit(
                        HeartRateMonitorEvent
                            .SensorLocationRead(
                                Result.success(
                                    parseSensorLocation(data)
                                )
                            )
                    )
                } catch (e: Exception) {
                    monitoringEventsFlow.emit(
                        HeartRateMonitorEvent.SensorLocationRead(
                            Result.failure(e)
                        )
                    )
                }
            } ?: run {
                monitoringEventsFlow.emit(
                    HeartRateMonitorEvent
                        .SensorLocationRead(
                            Result.failure(
                                IllegalStateException("Peripheral is not connected")
                            )
                        )
                )
            }
        }
    }

    private fun parseSensorLocation(data: ByteArray): String {
        return when (data[0].toInt()) {
            0 -> "Other"
            1 -> "Chest"
            2 -> "Wrist"
            3 -> "Finger"
            4 -> "Hand"
            5 -> "Ear Lobe"
            6 -> "Foot"
            else -> "Unknown"
        }
    }

    override fun resetEnergyExpended() {
        scope.launch {
            peripheral?.let {
                try {
                    val controlPointCharacteristic = characteristicOf(
                        service = HeartRateUUID.SERVICE,
                        characteristic = HeartRateUUID.CONTROL_POINT
                    )
                    val resetCommand = byteArrayOf(0x01)
                    it.write(controlPointCharacteristic, resetCommand)

                    monitoringEventsFlow.emit(
                        HeartRateMonitorEvent.EnergyExpendedReset(Result
                                .success(true)
                            )
                    )
                } catch (e: Exception) {
                    monitoringEventsFlow.emit(
                        HeartRateMonitorEvent.EnergyExpendedReset(
                            Result.failure(e)
                        )
                    )
                }
            }  ?: run {
                monitoringEventsFlow.emit(
                    HeartRateMonitorEvent
                        .EnergyExpendedReset(
                            Result.failure(
                                IllegalStateException("Peripheral is not connected")
                            )
                        )
                )
            }
        }
    }
}