package com.monomobile.bletower

object BTError {
    const val PERIPHERAL_NOT_CONNECTED = "Peripheral is not connected"
    const val SCAN_FAILED = "Scan failed with error"
    const val CHARACTERISTIC_READ = "Characteristic read failed"
    const val SERVICE_DISCOVERY = "Service discovery failed"
    val SERVICE_NOT_AVAIL: (String) -> String = { type -> "Service not available: $type" }
    val DESCRIPTOR_NOT_AVAIL: (String) -> String = { type -> "Descriptor not available: $type" }
    val CHARACTERISTIC_NOT_AVAIL: (String) -> String = { type -> "Characteristic not available: $type" }
}