package com.monomobile.bletower.monitor.kable

import com.juul.kable.PeripheralBuilder
import com.juul.kable.PlatformScanner
import com.monomobile.bletower.monitor.BaseMonitor

interface KableBaseMonitor: BaseMonitor {
    fun startMonitoring(
        scanner: PlatformScanner,
        builderAction: PeripheralBuilder.() -> Unit
    )
    fun stopMonitoring()
    fun connectToPeripheral()
    fun readDeviceInformation()
}