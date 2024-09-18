package com.monomobile.bletower

import android.bluetooth.BluetoothDevice
import com.monomobile.bletower.monitor.DeviceInformation

sealed class BaseMonitorEvent {
    data class MonitoringFailed(val error: Throwable) : BaseMonitorEvent()
    data class ServiceDiscoveryFailed(val error: String, val status: Int) : BaseMonitorEvent()
    data class WriteFailed(val error: String? = null, val status: Int? = null) : BaseMonitorEvent()
    data class ScanFailed(val error: String, val status: Int) : BaseMonitorEvent()
    data class ReadFailed(val error: String? = null, val status: Int? = null) : BaseMonitorEvent()
    data class ConnectFailed(val error: String? = null) : BaseMonitorEvent()
    data class ConnectionStateChanged(val connectionState: Int) : BaseMonitorEvent()
    data class DeviceFound(val deviceName: String): BaseMonitorEvent()
    data class ServiceDiscovered(val status: Int): BaseMonitorEvent()
    data class DeviceInformationReceived(
        val deviceInfo: Result<DeviceInformation?>
    ) : BaseMonitorEvent()
    data class Unknown(val message: String? = null): BaseMonitorEvent()
    data class BatteryLevelRead(val batteryLevel: Result<Int?>) : BaseMonitorEvent()
}