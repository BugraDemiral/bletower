package com.monomobile.bletower.monitor

import java.util.UUID

class DeviceInformationProcessor {
    private var manufacturerName: String? = null
    private var modelNumber: String? = null
    private var serialNumber: String? = null
    private var hardwareRevision: String? = null
    private var firmwareRevision: String? = null
    private var softwareRevision: String? = null

    fun resetDeviceInformation() {
        manufacturerName = null
        modelNumber = null
        serialNumber = null
        hardwareRevision = null
        firmwareRevision = null
        softwareRevision = null
    }

    fun setDeviceInformation(uuid: UUID, value: ByteArray): Boolean {
        var result = true

        when (uuid) {
            UUID.fromString(DeviceInformationUUID.MANUFACTURER_NAME) -> {
                manufacturerName = String(value)
            }
            UUID.fromString(DeviceInformationUUID.MODEL_NUMBER) -> {
                modelNumber = String(value)
            }
            UUID.fromString(DeviceInformationUUID.SERIAL_NUMBER) -> {
                serialNumber = String(value)
            }
            UUID.fromString(DeviceInformationUUID.HARDWARE_REVISION) -> {
                hardwareRevision = String(value)
            }
            UUID.fromString(DeviceInformationUUID.FIRMWARE_REVISION) -> {
                firmwareRevision = String(value)
            }
            UUID.fromString(DeviceInformationUUID.SOFTWARE_REVISION) -> {
                softwareRevision = String(value)
            }
            else -> {
                result = false
            }
        }

        return result
    }

    fun checkDeviceInformation(): DeviceInformation? {
        return if (manufacturerName != null && modelNumber != null
            && serialNumber != null && hardwareRevision != null
            && firmwareRevision != null && softwareRevision != null
        ) {
            DeviceInformation(
                manufacturerName = manufacturerName,
                modelNumber = modelNumber,
                serialNumber = serialNumber,
                hardwareRevision = hardwareRevision,
                firmwareRevision = firmwareRevision,
                softwareRevision = softwareRevision
            )
        } else {
            null
        }
    }
}