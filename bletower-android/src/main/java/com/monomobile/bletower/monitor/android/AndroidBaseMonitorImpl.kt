package com.monomobile.bletower.monitor.android

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import com.monomobile.bletower.BTError
import com.monomobile.bletower.BaseMonitorEvent
import com.monomobile.bletower.monitor.BaseMonitorCallbackFlow
import com.monomobile.bletower.monitor.DeviceInformationProcessor
import com.monomobile.bletower.monitor.DeviceInformationUUID
import com.monomobile.bletower.peripheral.heartrate.HeartRateUUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.annotations.VisibleForTesting
import timber.log.Timber
import java.util.*

@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
abstract class AndroidBaseMonitorImpl(
    private val context: Context,
    private val scope: CoroutineScope,
    private val deviceInformationProcessor: DeviceInformationProcessor =
        DeviceInformationProcessor(),
    private val isProd: Boolean = true
) : AndroidBaseMonitor, BaseMonitorCallbackFlow() {
    companion object {
        const val TAG = "AndroidBaseMonitorImpl"
        const val SCAN_TIMEOUT = 10000L // 10 seconds
    }

    private val connectionMutex = Mutex()
    private val characteristicMutex = Mutex()
    private val servicesMutex = Mutex()
    private val scanMutex = Mutex()

    private var job: Job? = null
    private var bluetoothGatt: BluetoothGatt? = null
    protected var bluetoothDevice: BluetoothDevice? = null

    @VisibleForTesting
    open fun getScanCallbackInstance(): ScanCallback? = scanCallback
    @VisibleForTesting
    open fun getGattCallbackInstance(): BluetoothGattCallback? = gattCallback

    private var scanCallback: ScanCallback? = null
    private var gattCallback: BluetoothGattCallback? = null

    private var timeoutJob: Job? = null

    abstract fun onCustomCharacteristicRead(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ): BaseMonitorEvent

    final override val monitoringEventsFlow = callbackFlow {
        sendChannel = this

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (isProd) {
                    super.onScanResult(callbackType, result)
                }

                handleDeviceFound(result)
            }

            override fun onBatchScanResults(results: List<ScanResult>) {
                if (isProd) {
                    super.onBatchScanResults(results)
                }

                handleDeviceFound(results.first())
            }

            private fun handleDeviceFound(result: ScanResult) {
                scope.launch {
                    scanMutex.withLock {
                        stopScan()

                        bluetoothDevice = result.device

                        bluetoothDevice?.let { device ->
                            fireTrySend(
                                BaseMonitorEvent.DeviceFound(
                                    device.name
                                )
                            )
                        }

                        Timber.tag(TAG).i(
                            "handleDeviceFound: device=${result.device.name}" +
                                    ", rssi=${result.rssi}"
                        )
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                if (isProd) {
                    super.onScanFailed(errorCode)
                }

                scope.launch {
                    scanMutex.withLock {
                        fireTrySend(
                            BaseMonitorEvent.ScanFailed(
                                BTError.SCAN_FAILED, errorCode)
                        )
                        Timber.tag(TAG).i(
                            "onScanFailed: err=${errorCode}"
                        )
                    }
                }
            }
        }

        gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (isProd) {
                    super.onConnectionStateChange(gatt, status, newState)
                }

                scope.launch {
                    connectionMutex.withLock {
                        when(newState) {
                            BluetoothProfile.STATE_CONNECTING -> {
                                fireTrySend(
                                    BaseMonitorEvent.ConnectionStateChanged(
                                        BluetoothProfile.STATE_CONNECTING
                                    )
                                )

                                Timber.tag(TAG).i(
                                    "onConnectionStateChange: STATE_CONNECTING"
                                )
                            }

                            BluetoothProfile.STATE_CONNECTED -> {
                                gatt.discoverServices()

                                fireTrySend(
                                    BaseMonitorEvent.ConnectionStateChanged(
                                        BluetoothProfile.STATE_CONNECTED
                                    )
                                )

                                Timber.tag(TAG).i(
                                    "onConnectionStateChange: STATE_CONNECTED"
                                )
                            }

                            BluetoothProfile.STATE_DISCONNECTED -> {
                                handlePeripheralDisconnect()

                                fireTrySend(
                                    BaseMonitorEvent.ConnectionStateChanged(
                                        BluetoothProfile.STATE_DISCONNECTED
                                    )
                                )

                                Timber.tag(TAG).i(
                                    "onConnectionStateChange: STATE_DISCONNECTED"
                                )
                            }
                        }


                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (isProd) {
                    super.onServicesDiscovered(gatt, status)
                }

                scope.launch {
                    servicesMutex.withLock {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            fireTrySend(BaseMonitorEvent.ServiceDiscovered(status))
                        } else {
                            fireTrySend(
                                BaseMonitorEvent.ServiceDiscoveryFailed(
                                BTError.SERVICE_DISCOVERY, status)
                            )
                        }
                        Timber.tag(TAG).i("onConnectionStateChange: onServicesDiscovered")
                    }
                }
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) {
                if (isProd) {
                    super.onCharacteristicRead(gatt, characteristic, value, status)
                }

                scope.launch {
                    try {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            characteristicMutex.withLock {
                                if (deviceInformationProcessor.setDeviceInformation(
                                        characteristic.uuid, value
                                )) {
                                    deviceInformationProcessor
                                        .checkDeviceInformation()?.let { deviceInfo ->
                                        fireTrySend(
                                            BaseMonitorEvent.DeviceInformationReceived(
                                                Result.success(deviceInfo)
                                            )
                                        )
                                        deviceInformationProcessor.resetDeviceInformation()
                                    }
                                } else {
                                    fireTrySend(onCustomCharacteristicRead(characteristic, value))
                                }
                                Timber.tag(TAG).i(
                                    "onCharacteristicRead: chr=$characteristic, " +
                                            "value=$value"
                                )
                            }
                        } else {
                            fireTrySend(
                                BaseMonitorEvent.ReadFailed(
                                BTError.CHARACTERISTIC_READ, status)
                            )
                        }
                    } catch (e: Exception) {
                        fireTrySend(BaseMonitorEvent.MonitoringFailed(e))
                    }
                }
            }
        }

        awaitClose {
            stopMonitoring()
        }
    }

    override val monitoringEvents: Flow<BaseMonitorEvent> = monitoringEventsFlow

    private fun getBluetoothAdapter(): BluetoothAdapter? {
        val bluetoothManager = context
            .getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter
    }

    override fun connectToPeripheral(autoConnect: Boolean) {
        scope.launch {
            bluetoothDevice?.let {
                bluetoothGatt = bluetoothDevice?.connectGatt(
                    context,
                    autoConnect,
                    gattCallback
                )

                Timber.tag(TAG).d(
                    "Connection initiated to Peripheral"
                )
            } ?: run {
                fireTrySend(
                    BaseMonitorEvent.ConnectFailed()
                )
            }
        }
    }

    override fun disconnectFromPeripheral() {
        scope.launch {
            bluetoothGatt?.let { gatt ->
                gatt.disconnect()
                gatt.close()

                bluetoothGatt = null

                Timber.tag(TAG).d(
                    "Disconnected from GATT server and resources cleaned up"
                )
            } ?: run {
                Timber.tag(TAG).d(
                    "Disconnected failed. GATT is null"
                )
            }
        }
    }

    override fun startMonitoring(
        scanFilters: List<ScanFilter>?,
        scanSettings: ScanSettings
    ) {
        job?.cancel()

        job = scope.launch {
            val bluetoothAdapter = getBluetoothAdapter()

            Timber.tag(TAG).i(
                "startMonitoring: scanFilters=${scanFilters.toString()}, " +
                        "scanSettings=$scanSettings"
            )

            if (bluetoothAdapter?.isEnabled == true) {
                scanCallback?.let {
                    bluetoothAdapter.bluetoothLeScanner.startScan(
                        scanFilters, scanSettings, scanCallback
                    )

                    timeoutJob = scope.launch {
                        delay(SCAN_TIMEOUT)
                        Timber.tag(TAG).d("Stopping scanning after timeout...")
                        stopScan()

                        fireTrySend(
                            BaseMonitorEvent.MonitoringFailed(
                                RuntimeException("Monitoring TIMEOUT!")
                            )
                        )
                    }
                } ?: run {
                    throw RuntimeException("ScanCallback is null")
                }
            } else {
                throw RuntimeException("Bluetooth is DISABLED")
            }
        }
    }

    private fun stopScan() {
        timeoutJob?.cancel()
        timeoutJob = null

        getBluetoothAdapter()
            ?.bluetoothLeScanner
            ?.stopScan(scanCallback)

        Timber.tag(TAG).d("Stopped scanning!")
    }

    override fun readDeviceInformation() {
        try {
            readCharacteristic(
                UUID.fromString(DeviceInformationUUID.SERVICE),
                UUID.fromString(DeviceInformationUUID.MANUFACTURER_NAME)
            )
            readCharacteristic(
                UUID.fromString(DeviceInformationUUID.SERVICE),
                UUID.fromString(DeviceInformationUUID.MODEL_NUMBER)
            )
            readCharacteristic(
                UUID.fromString(DeviceInformationUUID.SERVICE),
                UUID.fromString(DeviceInformationUUID.SERIAL_NUMBER)
            )
            readCharacteristic(
                UUID.fromString(DeviceInformationUUID.SERVICE),
                UUID.fromString(DeviceInformationUUID.HARDWARE_REVISION)
            )
            readCharacteristic(
                UUID.fromString(DeviceInformationUUID.SERVICE),
                UUID.fromString(DeviceInformationUUID.FIRMWARE_REVISION)
            )
            readCharacteristic(
                UUID.fromString(DeviceInformationUUID.SERVICE),
                UUID.fromString(DeviceInformationUUID.SOFTWARE_REVISION)
            )
        } catch (e: Exception) {
            fireTrySend(BaseMonitorEvent.MonitoringFailed(e))
        }
    }

    protected fun writeCharacteristic(
        serviceUUID: UUID,
        characteristicUUID: UUID,
        value: ByteArray) {
        scope.launch {
            bluetoothGatt?.let { gatt ->
                gatt.getService(
                    serviceUUID
                ) ?.let { service ->
                    service.getCharacteristic(
                        characteristicUUID
                    ) ?.let { characteristic ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (bluetoothGatt?.writeCharacteristic(
                                    characteristic,
                                    value,
                                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                                ) != BluetoothStatusCodes.SUCCESS
                            ) {
                                fireTrySend(BaseMonitorEvent.WriteFailed())
                            }
                        } else {
                            characteristic.value = value
                            if (bluetoothGatt?.writeCharacteristic(characteristic) == false) {
                                fireTrySend(BaseMonitorEvent.WriteFailed(error = String(value)))
                            }
                        }
                    } ?: run {
                        fireTrySend(
                            BaseMonitorEvent.WriteFailed(
                                BTError.CHARACTERISTIC_NOT_AVAIL(characteristicUUID.toString())
                            )
                        )
                    }
                } ?: run {
                    fireTrySend(
                        BaseMonitorEvent.MonitoringFailed(
                            Exception(BTError.SERVICE_NOT_AVAIL(serviceUUID.toString()))
                        )
                    )
                }
            } ?: run {
                fireTrySend(
                    BaseMonitorEvent.MonitoringFailed(
                        Exception(BTError.PERIPHERAL_NOT_CONNECTED)
                    )
                )
            }
        }
    }

    protected fun readCharacteristic(
        serviceUUID: UUID,
        characteristicUUID: UUID) {
        scope.launch {
            bluetoothGatt?.let { gatt ->
                gatt.getService(
                    serviceUUID
                ) ?. let { service ->
                    service.getCharacteristic(
                        characteristicUUID
                    ) ?.let { characteristic ->
                        if (!gatt.readCharacteristic(characteristic)) {
                            fireTrySend(BaseMonitorEvent.ReadFailed())
                        }
                    } ?: run {
                        fireTrySend(
                            BaseMonitorEvent.ReadFailed(
                                BTError.CHARACTERISTIC_NOT_AVAIL(characteristicUUID.toString())
                            )
                        )
                    }
                } ?: run {
                    fireTrySend(
                        BaseMonitorEvent.MonitoringFailed(
                            Exception(BTError.SERVICE_NOT_AVAIL(serviceUUID.toString()))
                        )
                    )
                }
            } ?: run {
                fireTrySend(
                    BaseMonitorEvent.MonitoringFailed(
                        Exception(BTError.PERIPHERAL_NOT_CONNECTED)
                    )
                )
            }
        }
    }

    protected fun observeCharacteristic(
        serviceUUID: UUID,
        characteristicUUID: UUID) {
        scope.launch {
            bluetoothGatt?.let { gatt ->
                gatt.getService(
                    serviceUUID
                ) ?. let { service ->
                    service.getCharacteristic(
                        characteristicUUID
                    ) ?. let { characteristic ->
                        gatt.setCharacteristicNotification(characteristic, true)
                        characteristic.getDescriptor(
                            UUID.fromString(
                                HeartRateUUID.CLIENT_CHARACTERISTIC_CONFIG_UUID
                            )
                        ) ?. let { descriptor ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (gatt.writeDescriptor(
                                        descriptor,
                                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                    ) != BluetoothStatusCodes.SUCCESS
                                ) {
                                    fireTrySend(BaseMonitorEvent.WriteFailed())
                                }
                            } else {
                                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                if (!gatt.writeDescriptor(descriptor)) {
                                    fireTrySend(BaseMonitorEvent.WriteFailed())
                                }
                            }
                        } ?: run {
                            fireTrySend(
                                BaseMonitorEvent.WriteFailed(
                                    BTError.DESCRIPTOR_NOT_AVAIL(
                                        HeartRateUUID.CLIENT_CHARACTERISTIC_CONFIG_UUID
                                    )
                                )
                            )
                        }
                    } ?: run {
                        fireTrySend(
                            BaseMonitorEvent.MonitoringFailed(
                                Exception(
                                    BTError.CHARACTERISTIC_NOT_AVAIL(
                                    characteristicUUID.toString())
                                )
                            )
                        )
                    }
                } ?: run {
                    fireTrySend(
                        BaseMonitorEvent.MonitoringFailed(
                            Exception(
                                BTError.SERVICE_NOT_AVAIL(
                                serviceUUID.toString())
                            )
                        )
                    )
                }
            } ?: run {
                fireTrySend(
                    BaseMonitorEvent.MonitoringFailed(
                        Exception(BTError.PERIPHERAL_NOT_CONNECTED)
                    )
                )
            }
        }
    }

    private suspend fun handlePeripheralDisconnect() {
        bluetoothGatt?.disconnect()

        // give BLE stack a time to disconnect gracefully
        delay(200)

        bluetoothGatt?.close()
    }

    override fun stopMonitoring() {
        scope.launch {
            job?.cancel()
            job = null

            handlePeripheralDisconnect()

            Timber.tag(TAG).i(
                "stopMonitoring:"
            )
        }
    }
}
