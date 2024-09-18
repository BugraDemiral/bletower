package com.monomobile.bletower.monitor.polar

import com.monomobile.bletower.monitor.BaseMonitor

interface PolarBaseMonitor: BaseMonitor {
    fun startMonitoring()
    fun connectToPeripheral(peripheralName: String)
    fun stopMonitoring()
}