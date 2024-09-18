package com.monomobile.bletower.monitor

import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import com.monomobile.bletower.BTError
import com.monomobile.bletower.BaseMonitorEvent
import com.monomobile.bletower.monitor.android.AndroidBaseMonitorImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidBaseMonitorImplTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var bluetoothManager: BluetoothManager

    @Mock
    private lateinit var bluetoothAdapter: BluetoothAdapter

    @Mock
    private lateinit var bluetoothGatt: BluetoothGatt

    @Mock
    private lateinit var bluetoothDevice: BluetoothDevice

    @Mock
    private lateinit var testScope: TestScope

    @Mock
    private lateinit var testMonitor: AndroidBaseMonitorImpl

    @Mock
    private lateinit var characteristic: BluetoothGattCharacteristic

    @Mock
    lateinit var scanResult: ScanResult

    @Mock
    lateinit var service: BluetoothGattService

    @Mock
    lateinit var deviceInformationProcessor: DeviceInformationProcessor

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        `when`(context.getSystemService(Context.BLUETOOTH_SERVICE)).thenReturn(bluetoothManager)
        `when`(bluetoothManager.adapter).thenReturn(bluetoothAdapter)

        `when`(scanResult.device).thenReturn(bluetoothDevice)
        `when`(bluetoothDevice.connectGatt(any(), eq(false), any())).thenReturn(bluetoothGatt)

        Dispatchers.setMain(StandardTestDispatcher())
        testScope = TestScope()

        testMonitor = object : AndroidBaseMonitorImpl(
            context = context,
            scope = testScope,
            deviceInformationProcessor = deviceInformationProcessor,
            isProd = false
        ) {
            override fun onCustomCharacteristicRead(
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ): BaseMonitorEvent {
                return BaseMonitorEvent.Unknown()
            }
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startMonitoring should trigger scanAndConnect`() = runTest {
        val scanFilters = emptyList<ScanFilter>()
        val scanSettings = mock(ScanSettings::class.java)

        val bluetoothLeScanner = mock(BluetoothLeScanner::class.java)
        `when`(bluetoothAdapter.bluetoothLeScanner).thenReturn(bluetoothLeScanner)
        `when`(bluetoothAdapter.isEnabled).thenReturn(true)

        val eventChannel = testMonitor.monitoringEvents
        val events = mutableListOf<BaseMonitorEvent>()

        launch {
            eventChannel
                .take(1)
                .collect { event ->
                    events.add(event)
                }
        }
        advanceUntilIdle()

        testMonitor.startMonitoring(scanFilters, scanSettings)

        advanceUntilIdle()

        verify(bluetoothAdapter.bluetoothLeScanner).startScan(
            scanFilters,
            scanSettings,
            testMonitor.getScanCallbackInstance()
        )
    }

    @Test
    fun `onScanFailed should emit onScanFailed event`() = runTest {
        val eventChannel = testMonitor.monitoringEvents
        val events = mutableListOf<BaseMonitorEvent>()

        launch {
            eventChannel
                .take(1)
                .collect { event ->
                    events.add(event)
                }
        }
        advanceUntilIdle()

        val scanCallbackInst = testMonitor.getScanCallbackInstance()
        scanCallbackInst?.onScanFailed(BluetoothAdapter.ERROR)

        advanceUntilIdle()

        assertTrue(events.first() is BaseMonitorEvent.ScanFailed)
        val event =  events.first() as BaseMonitorEvent.ScanFailed
        assertEquals(event.error, BTError.SCAN_FAILED)
    }

    @Test
    fun `onConnectionStateChange should emit ConnectionStateChanged event`() = runTest {
        val eventChannel = testMonitor.monitoringEvents
        val events = mutableListOf<BaseMonitorEvent>()

        launch {
            eventChannel.take(1).collect { event ->
                events.add(event)
            }
        }
        advanceUntilIdle()

        testMonitor.getGattCallbackInstance()?.onConnectionStateChange(
            bluetoothGatt,
            BluetoothGatt.GATT_SUCCESS,
            BluetoothProfile.STATE_CONNECTED
        )

        advanceUntilIdle()

        verify(bluetoothGatt).discoverServices()

        assertTrue(events[0] is BaseMonitorEvent.ConnectionStateChanged)
    }

    @Test
    fun `onServicesDiscovered should emit MonitoringFailed on failure`() = runTest {
        val eventChannel = testMonitor.monitoringEvents
        val events = mutableListOf<BaseMonitorEvent>()

        launch {
            eventChannel.take(1).collect { event ->
                events.add(event)
            }
        }
        advanceUntilIdle()

        testMonitor.getGattCallbackInstance()?.onServicesDiscovered(
            bluetoothGatt,
            BluetoothGatt.GATT_FAILURE
        )

        advanceUntilIdle()

        assertTrue(events.first() is BaseMonitorEvent.ServiceDiscoveryFailed)
        val event =  events.first() as BaseMonitorEvent.ServiceDiscoveryFailed
        assertEquals(event.error, BTError.SERVICE_DISCOVERY)
    }

    @Test
    fun `onCharacteristicRead should call onCustomCharacteristicRead on success`() = runTest {
        val message = "Test message"
        val localTestMonitor = object : AndroidBaseMonitorImpl(context, testScope, isProd = false) {
            override fun onCustomCharacteristicRead(
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ): BaseMonitorEvent {
                return BaseMonitorEvent.Unknown(message)
            }
        }

        `when`(characteristic.uuid).thenReturn(
            UUID.fromString("00000000-0000-0000-8000-000000000000")
        )

        val eventChannel = localTestMonitor.monitoringEvents
        val events = mutableListOf<BaseMonitorEvent>()

        launch {
            eventChannel.take(1).collect { event ->
                events.add(event)
            }
        }
        advanceUntilIdle()

        localTestMonitor.getGattCallbackInstance()?.onCharacteristicRead(
            bluetoothGatt,
            characteristic,
            message.toByteArray(),
            BluetoothGatt.GATT_SUCCESS
        )

        advanceUntilIdle()

        assertTrue(events[0] is BaseMonitorEvent.Unknown)
        assertEquals(message, (events[0] as BaseMonitorEvent.Unknown).message)
    }

    @Test
    fun `readDeviceInformation should emit failure when disconnected`() = runTest {
        testMonitor.stopMonitoring()

        advanceUntilIdle()

        val eventChannel = testMonitor.monitoringEvents
        val events = mutableListOf<BaseMonitorEvent>()

        val job = launch {
            eventChannel.collect { event ->
                //if (event is BaseMonitorEvent.MonitoringFailed) {
                    events.add(event)
                    cancel()
                //}
            }
        }

        advanceUntilIdle()

        testMonitor.readDeviceInformation()

        job.join()

        assertTrue(events.last() is BaseMonitorEvent.MonitoringFailed)
        val event = events.last() as BaseMonitorEvent.MonitoringFailed
        assertEquals(event.error.message, BTError.PERIPHERAL_NOT_CONNECTED)
    }
}
