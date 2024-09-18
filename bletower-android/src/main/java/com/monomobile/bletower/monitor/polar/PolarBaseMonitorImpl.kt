package com.monomobile.bletower.monitor.polar

import android.annotation.SuppressLint
import android.bluetooth.BluetoothProfile
import com.monomobile.bletower.BaseMonitorEvent
import com.monomobile.bletower.monitor.BaseMonitorCallbackFlow
import com.monomobile.bletower.monitor.DeviceInformationProcessor
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.model.PolarDeviceInfo
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.jetbrains.annotations.VisibleForTesting
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

open class PolarBaseMonitorImpl(
    private val polarBleApi: PolarBleApi,
    private val scheduler: Scheduler
) : PolarBaseMonitor, BaseMonitorCallbackFlow() {
    companion object {
        const val TAG = "PolarBaseMonitorImpl"
    }

    protected var deviceId: String? = null
    private val deviceInformationProcessor = DeviceInformationProcessor()

    @VisibleForTesting
    fun getCallbackInstance(): PolarBleApiCallback? = polarBleApiCallback
    @VisibleForTesting
    fun setDevice(id: String) { deviceId = id }
    @VisibleForTesting
    fun setScanDisposable(disposable: Disposable) { scanDisposable = disposable }

    private var polarBleApiCallback: PolarBleApiCallback? = null

    private var scanDisposable: Disposable? = null

    final override val monitoringEventsFlow = callbackFlow {
        sendChannel = this

        polarBleApiCallback = object : PolarBleApiCallback() {
            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                deviceId = polarDeviceInfo.deviceId

                fireTrySend(
                    BaseMonitorEvent.ConnectionStateChanged(
                        BluetoothProfile.STATE_CONNECTED
                    )
                )
                Timber.tag(TAG).i(
                    "deviceConnected: ${polarDeviceInfo.deviceId}"
                )
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                deviceId = null

                fireTrySend(
                    BaseMonitorEvent.ConnectionStateChanged(
                        BluetoothProfile.STATE_DISCONNECTED
                    )
                )
                Timber.tag(TAG).i(
                    "deviceDisconnected: ${polarDeviceInfo.deviceId}"
                )
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                fireTrySend(
                    BaseMonitorEvent.BatteryLevelRead(
                        Result.success(level)
                    )
                )
                Timber.tag(TAG).i(
                    "batteryLevelReceived: $level"
                )
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                fireTrySend(
                    BaseMonitorEvent.ConnectionStateChanged(
                        BluetoothProfile.STATE_CONNECTING
                    )
                )
                Timber.tag(TAG).i(
                    "deviceConnecting: ${polarDeviceInfo.deviceId}"
                )
            }

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                if (deviceInformationProcessor.setDeviceInformation(
                        uuid, value.toByteArray()
                    )
                ) {
                    deviceInformationProcessor
                        .checkDeviceInformation()?.let { deviceInfo ->
                            fireTrySend(
                                BaseMonitorEvent.DeviceInformationReceived(
                                    Result.success(deviceInfo)
                                )
                            )
                            deviceInformationProcessor.resetDeviceInformation()
                        }
                }
                Timber.tag(TAG).i(
                    "disInformationReceived: uuid= ${uuid}, $value"
                )
            }
        }

        awaitClose {
            stopMonitoring()
        }
    }

    override val monitoringEvents: Flow<BaseMonitorEvent> = monitoringEventsFlow

    init {
        polarBleApiCallback?.let {
            polarBleApi.setApiCallback(it)
        }
    }

    override fun startMonitoring() {
        val isDisposed = scanDisposable?.isDisposed ?: true
        if (isDisposed) {
            scanDisposable = polarBleApi.searchForDevice()
                .observeOn(scheduler)
                .subscribe(
                    { polarDeviceInfo: PolarDeviceInfo ->
                        fireTrySend(
                            BaseMonitorEvent.DeviceFound(
                                polarDeviceInfo.name
                            )
                        )

                        val message = "polar device found id: " +
                                polarDeviceInfo.deviceId + " address: " +
                                polarDeviceInfo.address + " rssi: " +
                                polarDeviceInfo.rssi + " name: " +
                                polarDeviceInfo.name + " isConnectable: " +
                                polarDeviceInfo.isConnectable
                        Timber.tag(TAG).d(message)
                    },
                    { error: Throwable ->
                        Timber.tag(TAG).e( "Device scan failed. Reason $error")
                    },
                    {
                        Timber.tag(TAG).d( "complete")
                    }
                )
            Timber.tag(TAG).i("startMonitoring:")
        } else {
            fireTrySend(
                BaseMonitorEvent.MonitoringFailed(
                RuntimeException("Scan object is not disposed"))
            )
        }
    }

    override fun connectToPeripheral(peripheralName: String) {
        polarBleApi.connectToDevice(peripheralName)
    }

    override fun stopMonitoring() {
        disconnectAndCleanUp()
        Timber.tag(TAG).i("stopMonitoring:")
    }

    @SuppressLint("CheckResult")
    private fun disconnectAndCleanUp() {
        deviceId?.let {
            polarBleApi.disconnectFromDevice(it)
        }

        Single.just("Delay to give time for disconnect")
            .delay(500, TimeUnit.MILLISECONDS, scheduler)

        scanDisposable?.dispose()
        scanDisposable = null

        sendChannel?.close()
        sendChannel = null
    }
}
