package com.monomobile.bletower.monitor

data class DeviceInformation(
    val manufacturerName: String?,
    val modelNumber: String?,
    val serialNumber: String?,
    val hardwareRevision: String?,
    val firmwareRevision: String?,
    val softwareRevision: String?
)
