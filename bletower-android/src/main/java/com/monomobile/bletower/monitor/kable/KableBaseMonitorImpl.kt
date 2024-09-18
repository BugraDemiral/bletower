package com.monomobile.bletower.monitor.kable

import android.bluetooth.BluetoothProfile
import com.juul.kable.Peripheral
import com.juul.kable.PeripheralBuilder
import com.juul.kable.PlatformScanner
import com.juul.kable.State
import com.juul.kable.characteristicOf
import com.juul.kable.peripheral
import com.monomobile.bletower.BaseMonitorEvent
import com.monomobile.bletower.monitor.BaseMonitorFlow
import com.monomobile.bletower.monitor.DeviceInformation
import com.monomobile.bletower.monitor.DeviceInformationUUID
import com.monomobile.bletower.monitor.android.AndroidBaseMonitorImpl
import com.monomobile.bletower.monitor.android.AndroidBaseMonitorImpl.Companion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

open class KableBaseMonitorImpl(private val scope: CoroutineScope)
    : KableBaseMonitor, BaseMonitorFlow() {
    companion object {
        const val TAG = "KableBaseMonitorImpl"
    }

    private var _peripheral: Peripheral? = null
    val peripheral: Peripheral?
        get() {
            return _peripheral
        }

    final override val monitoringEventsFlow:
            MutableSharedFlow<BaseMonitorEvent> = MutableSharedFlow()
    override val monitoringEvents:
            SharedFlow<BaseMonitorEvent> = monitoringEventsFlow.asSharedFlow()

    override fun startMonitoring(
        scanner: PlatformScanner,
        builderAction: PeripheralBuilder.() -> Unit
    ) {
        scanner.advertisements
            .onEach { advertisement ->
                _peripheral = scope.peripheral(advertisement, builderAction)

                monitoringEventsFlow.emit(
                    BaseMonitorEvent.DeviceFound(
                        _peripheral?.name ?: ""
                    )
                )

                Timber.tag(TAG).i(
                    "advertisement: device=${_peripheral?.name}" +
                            ", rssi=${_peripheral?.rssi()}"
                )
            }
            .catch { e ->
                monitoringEventsFlow.emit(
                    BaseMonitorEvent.MonitoringFailed(e)
                )
            }
            .launchIn(scope)

        Timber.tag(AndroidBaseMonitorImpl.TAG).i("startMonitoring:")
    }

    override fun connectToPeripheral() {
        scope.launch {
            _peripheral?.connect()

            _peripheral?.state
                ?.onEach { state ->
                    when(state) {
                        State.Connected -> monitoringEventsFlow.emit(
                            BaseMonitorEvent.ConnectionStateChanged(
                                BluetoothProfile.STATE_CONNECTED
                            )
                        )

                        State.Connecting.Bluetooth,
                        State.Connecting.Observes,
                        State.Connecting.Services -> monitoringEventsFlow.emit(
                            BaseMonitorEvent.ConnectionStateChanged(
                                BluetoothProfile.STATE_CONNECTING)
                        )

                        is State.Disconnected,
                        State.Disconnecting -> monitoringEventsFlow.emit(
                            BaseMonitorEvent.ConnectionStateChanged(
                                BluetoothProfile.STATE_DISCONNECTED
                            )
                        )
                    }
                    Timber.tag(AndroidBaseMonitorImpl.TAG).i(
                        "State: $state")
                }?.catch { e ->
                    monitoringEventsFlow.emit(BaseMonitorEvent.MonitoringFailed(e))
                }
        }
    }

    override fun readDeviceInformation() {
        scope.launch {
            _peripheral?.let {
                try {
                    val manufacturerName = readDeviceInfoCharacteristic(
                        DeviceInformationUUID.MANUFACTURER_NAME
                    )
                    val modelNumber = readDeviceInfoCharacteristic(
                        DeviceInformationUUID.MODEL_NUMBER
                    )
                    val serialNumber = readDeviceInfoCharacteristic(
                        DeviceInformationUUID.SERIAL_NUMBER
                    )
                    val hardwareRevision = readDeviceInfoCharacteristic(
                        DeviceInformationUUID.HARDWARE_REVISION
                    )
                    val firmwareRevision = readDeviceInfoCharacteristic(
                        DeviceInformationUUID.FIRMWARE_REVISION
                    )
                    val softwareRevision = readDeviceInfoCharacteristic(
                        DeviceInformationUUID.SOFTWARE_REVISION
                    )

                    val deviceInformation = DeviceInformation(
                        manufacturerName,
                        modelNumber,
                        serialNumber,
                        hardwareRevision,
                        firmwareRevision,
                        softwareRevision
                    )
                    monitoringEventsFlow.emit(
                        BaseMonitorEvent.DeviceInformationReceived(
                            Result.success(deviceInformation)
                        )
                    )
                } catch (e: Exception) {
                    monitoringEventsFlow.emit(
                        BaseMonitorEvent.DeviceInformationReceived(
                            Result.failure(e)
                        )
                    )
                }
            } ?: run {
                monitoringEventsFlow.emit(
                    BaseMonitorEvent
                        .DeviceInformationReceived(
                            Result.failure(
                                IllegalStateException("Peripheral is not connected")
                            )
                        )
                )
            }
        }
    }

    private suspend fun readDeviceInfoCharacteristic(characteristicUuid: String): String? {
        return try {
            val characteristic = characteristicOf(
                service = DeviceInformationUUID.SERVICE,
                characteristic = characteristicUuid
            )
            val data = _peripheral?.read(characteristic)
            if (data != null) {
                String(data)
            } else {
                null
            }
        } catch (e: Exception) {
            return null
        }
    }

    override fun stopMonitoring() {
        scope.launch {
            _peripheral?.disconnect()
            Timber.tag(AndroidBaseMonitorImpl.TAG).i("stopMonitoring:")
        }
    }
}
