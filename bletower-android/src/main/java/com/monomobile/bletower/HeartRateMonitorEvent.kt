package com.monomobile.bletower

sealed class HeartRateMonitorEvent: BaseMonitorEvent() {
    data class HeartRateRead(val heartRate: Result<Int?>) : HeartRateMonitorEvent()
    data class SensorLocationRead(val sensorLocation: Result<String?>) : HeartRateMonitorEvent()
    data class EnergyExpendedReset(val result: Result<Boolean?>) : HeartRateMonitorEvent()
}