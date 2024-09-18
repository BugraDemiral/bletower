package com.monomobile.bletower.monitor.android

import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import com.monomobile.bletower.monitor.BaseMonitor

interface AndroidBaseMonitor: BaseMonitor {
    fun startMonitoring(
        scanFilters: List<ScanFilter>?,
        scanSettings: ScanSettings
    )
    fun stopMonitoring()
    fun connectToPeripheral(autoConnect: Boolean = false)
    fun disconnectFromPeripheral()
    fun readDeviceInformation()
}