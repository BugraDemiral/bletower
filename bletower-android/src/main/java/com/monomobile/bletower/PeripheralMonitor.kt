package com.monomobile.bletower

import com.monomobile.bletower.monitor.BaseMonitor

interface PeripheralMonitor: BaseMonitor {
    fun readSensorLocation()
    fun readMeasurement()
    fun readBatteryLevel()
    fun resetEnergyExpended()
    fun startObservingMeasurement()
    fun startObservingBattery()
}