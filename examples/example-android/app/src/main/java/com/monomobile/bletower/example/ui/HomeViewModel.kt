package com.monomobile.bletower.example.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.juul.kable.Filter
import com.juul.kable.Scanner
import com.juul.kable.logs.Logging
import com.monomobile.bletower.BaseMonitorEvent
import com.monomobile.bletower.HeartRateMonitorEvent
import com.monomobile.bletower.example.MainActivity.Companion.TAG
import com.monomobile.bletower.monitor.BaseMonitorFlow
import com.monomobile.bletower.peripheral.heartrate.HeartRateMonitor
import com.monomobile.bletower.peripheral.heartrate.android.AndroidHeartRateMonitorImpl
import com.monomobile.bletower.peripheral.heartrate.kable.KableHeartRateMonitorImpl
import com.monomobile.bletower.peripheral.heartrate.polar.PolarHeartRateMonitorImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val heartRateMonitor: HeartRateMonitor
) : ViewModel() {

    private val _permissionsGranted = MutableLiveData<Boolean>()
    val permissionsGranted: LiveData<Boolean> = _permissionsGranted

    fun setPermissionsGranted(enable: Boolean) {
        _permissionsGranted.value = enable
    }

    private val _handleEvent = MutableLiveData<BaseMonitorEvent>()
    val handleEvent: LiveData<BaseMonitorEvent> = _handleEvent

    private val scope = CoroutineScope(Dispatchers.Main)
    private var deviceFound: Boolean = false

    init {
        initializeHeartRateMonitor()
    }

    @SuppressLint("MissingPermission")
    fun initializeHeartRateMonitor() {
        scope.launch {
            val monitorFlow = heartRateMonitor as BaseMonitorFlow
            monitorFlow.monitoringEvents.collect { event ->
                when (event) {
                    is BaseMonitorEvent.ConnectionStateChanged -> {
                        when(event.connectionState)  {
                            BluetoothProfile.STATE_CONNECTED -> {
                                Timber.tag(TAG).d( "Device STATE_CONNECTED")
                            }

                            BluetoothProfile.STATE_CONNECTING ->
                                Timber.tag(TAG).d( "Device STATE_CONNECTING")

                            BluetoothProfile.STATE_DISCONNECTED ->
                                Timber.tag(TAG).d( "Device STATE_DISCONNECTED")
                        }
                    }

                    is BaseMonitorEvent.DeviceInformationReceived -> {
                        event.deviceInfo.onSuccess { deviceInfo ->
                            Timber.tag(TAG).d( "Device Information: $deviceInfo")
                        }.onFailure { exception ->
                            Timber.tag(TAG).d("Device Information Error: ${exception.message}")
                        }
                    }

                    is BaseMonitorEvent.DeviceFound -> {
                        deviceFound = true
                        _handleEvent.postValue(event)
                        Timber.tag(TAG).d("DeviceFound: ${event.deviceName}")
                    }

                    is BaseMonitorEvent.BatteryLevelRead -> {
                        _handleEvent.postValue(event)
                        Timber.tag(TAG).d("BatteryLevelRead: ${event.batteryLevel}")
                    }

                    is HeartRateMonitorEvent.EnergyExpendedReset -> {
                        Timber.tag(TAG).d("EnergyExpendedReset: ${event.result}")
                    }

                    is HeartRateMonitorEvent.HeartRateRead -> {
                        _handleEvent.postValue(event)
                        Timber.tag(TAG).d("HeartRateRead: ${event.heartRate}")
                    }

                    is HeartRateMonitorEvent.SensorLocationRead -> {
                        _handleEvent.postValue(event)
                        Timber.tag(TAG).d("SensorLocationRead: ${event.sensorLocation}")
                    }

                    is BaseMonitorEvent.ServiceDiscovered -> {
                        _handleEvent.postValue(event)
                        Timber.tag(TAG).d("ServiceDiscovered: ${event.status}")
                    }

                    is BaseMonitorEvent.Unknown,
                    is BaseMonitorEvent.ReadFailed,
                    is BaseMonitorEvent.ScanFailed,
                    is BaseMonitorEvent.ServiceDiscoveryFailed,
                    is BaseMonitorEvent.WriteFailed,
                    is BaseMonitorEvent.MonitoringFailed,
                    is BaseMonitorEvent.ConnectFailed -> {
                        _handleEvent.postValue(event)
                        Timber.tag(TAG).d("Error: $event")
                    }
                }
            }
        }
    }

    fun startHeartRateMonitor() {
        when (heartRateMonitor) {
            is AndroidHeartRateMonitorImpl -> {
                startAndroidHeartRateMonitor(heartRateMonitor)
            }

            is KableHeartRateMonitorImpl -> {
                startKableHeartRateMonitor(heartRateMonitor)
            }

            is PolarHeartRateMonitorImpl -> {
                startPolarApiHeartRateMonitor(heartRateMonitor)
            }
        }
    }

    private fun startAndroidHeartRateMonitor(
        androidHeartRateMonitor: AndroidHeartRateMonitorImpl
    ) {
        val scanFilters = listOf(
            ScanFilter.Builder()
                .setDeviceName("Heart Rate")
                .build()
        )

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .build()

        try {
            androidHeartRateMonitor.startMonitoring(scanFilters, scanSettings)
        } catch(e: Exception) {
            Timber.tag(TAG).e(e)
        }
    }

    private fun startKableHeartRateMonitor(
        kableHeartRateMonitor: KableHeartRateMonitorImpl
    ) {
        val serviceList = listOf<UUID>()
        val peripheralName = "Heart Rate"
        val scanner = Scanner {
            filters {
                match {
                    serviceList.let {
                        services = it
                    }
                    name = Filter.Name.Exact(peripheralName)
                }
            }
            logging {
                level = Logging.Level.Data
            }
        }

        try {
            kableHeartRateMonitor.startMonitoring(scanner) {
                autoConnectIf { false }
                onServicesDiscovered {
                    _handleEvent.postValue(
                        BaseMonitorEvent.ServiceDiscovered(GATT_SUCCESS)
                    )
                }
            }
        } catch(e: Exception) {
            Timber.tag(TAG).e(e)
        }
    }

    private fun startPolarApiHeartRateMonitor(
        polarHeartRateMonitor: PolarHeartRateMonitorImpl,
    ) {
        try {
            polarHeartRateMonitor.startMonitoring()
        } catch(e: Exception) {
            Timber.tag(TAG).e(e)
        }
    }

    fun connectToPeripheral() {
        when (heartRateMonitor) {
            is AndroidHeartRateMonitorImpl -> {
                heartRateMonitor.connectToPeripheral()
            }

            is KableHeartRateMonitorImpl -> {
                heartRateMonitor.connectToPeripheral()
            }

            is PolarHeartRateMonitorImpl -> {
                val peripheralName = "Polar HR Sensor"
                heartRateMonitor.connectToPeripheral(peripheralName)
            }
        }
    }

    fun readData() {
        if(deviceFound) {
            when (heartRateMonitor) {
                is AndroidHeartRateMonitorImpl,
                is KableHeartRateMonitorImpl -> {
                    heartRateMonitor.readMeasurement()
                }

                is PolarHeartRateMonitorImpl -> {
                    heartRateMonitor.startObservingMeasurement()
                }
            }

        }
    }
}