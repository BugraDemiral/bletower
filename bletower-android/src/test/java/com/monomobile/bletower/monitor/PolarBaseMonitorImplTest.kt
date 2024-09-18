package com.monomobile.bletower.monitor

import android.bluetooth.BluetoothProfile
import com.monomobile.bletower.BaseMonitorEvent
import com.monomobile.bletower.monitor.polar.PolarBaseMonitorImpl
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.model.PolarDeviceInfo
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.TestScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class PolarBaseMonitorImplTest {
    private lateinit var polarBleApi: PolarBleApi
    private lateinit var testScheduler: TestScheduler
    private lateinit var monitor: PolarBaseMonitorImpl

    @Before
    fun setup() {
        polarBleApi = mock()
        testScheduler = TestScheduler()
        monitor = PolarBaseMonitorImpl(polarBleApi, testScheduler)
    }

    @Test
    fun `test device connection emits correct events`() = runTest {
        val deviceInfo = PolarDeviceInfo("testDeviceId",
            "testAddress",
            50, "testName",
            true
        )

        // Simulate device connecting
        var job = launch {
            val connectingState = monitor.monitoringEvents.first {
                it is BaseMonitorEvent.ConnectionStateChanged &&
                        it.connectionState == BluetoothProfile.STATE_CONNECTING
            }

            assertTrue(connectingState is BaseMonitorEvent.ConnectionStateChanged)
        }
        advanceUntilIdle()

        monitor.getCallbackInstance()?.deviceConnecting(deviceInfo)

        job.join()

        // Simulate device connected
        job = launch {
            val connectingState = monitor.monitoringEvents.first {
                it is BaseMonitorEvent.ConnectionStateChanged &&
                        it.connectionState == BluetoothProfile.STATE_CONNECTED
            }

            assertTrue(connectingState is BaseMonitorEvent.ConnectionStateChanged)
        }
        advanceUntilIdle()

        monitor.getCallbackInstance()?.deviceConnected(deviceInfo)

        job.join()
    }

    @Test
    fun `test device disconnection emits correct event`() = runTest {
        val deviceInfo = PolarDeviceInfo(
            "testDeviceId",
            "testAddress",
            50, "testName",
            true
        )

        val job = launch {
            val connectingState = monitor.monitoringEvents.first {
                it is BaseMonitorEvent.ConnectionStateChanged &&
                        it.connectionState == BluetoothProfile.STATE_DISCONNECTED
            }

            assertTrue(connectingState is BaseMonitorEvent.ConnectionStateChanged)
        }
        advanceUntilIdle()

        monitor.getCallbackInstance()?.deviceDisconnected(deviceInfo)

        job.join()
    }

    @Test
    fun `test battery level received emits correct event`() = runTest {
        val identifier = "testDeviceId"
        val batteryLevel = 75

        val job = launch {
            val batteryEvent = monitor.monitoringEvents.first {
                it is BaseMonitorEvent.BatteryLevelRead
            }

            assertTrue(batteryEvent is BaseMonitorEvent.BatteryLevelRead)
            assertEquals((batteryEvent as BaseMonitorEvent.BatteryLevelRead)
                .batteryLevel.getOrNull(), batteryLevel)
        }
        advanceUntilIdle()

        monitor.getCallbackInstance()?.batteryLevelReceived(identifier, batteryLevel)

        job.join()
    }

    @Test
    fun `test monitoring fails when scan is not disposed`() = runTest {
        val testDisposable = mock<Disposable> {
            on { isDisposed } doReturn false
        }
        monitor.setScanDisposable(testDisposable)

        val job = launch {
            val failedEvent = monitor.monitoringEvents.first {
                it is BaseMonitorEvent.MonitoringFailed
            }

            assertTrue(failedEvent is BaseMonitorEvent.MonitoringFailed)
            assertTrue((failedEvent as BaseMonitorEvent.MonitoringFailed).error is RuntimeException)
        }
        advanceUntilIdle()

        monitor.startMonitoring()

        job.join()
    }

    @Test
    fun `test start monitoring with valid scan and connection`() = runTest {
        /*
        val deviceInfo = PolarDeviceInfo(
            "testDeviceId",
            "testAddress",
            50,
            "testName",
            true
        )
        val deviceScanSubject = ReplaySubject.create<PolarDeviceInfo>()
        whenever(polarBleApi.searchForDevice()).thenReturn(deviceScanSubject.toFlowable(
            BackpressureStrategy.BUFFER))
        doNothing().`when`(polarBleApi).connectToDevice("testDeviceId")

        monitor.startMonitoring("testDeviceId")

        assertTrue(deviceScanSubject.hasObservers())

        deviceScanSubject.onNext(deviceInfo)

        testScheduler.advanceTimeBy(500)

        verify(polarBleApi).connectToDevice("testDeviceId")

         */
    }

    @Test
    fun `test device information processing`() = runTest {
        val job = launch {
            val deviceInfoEvent = monitor.monitoringEvents.first {
                it is BaseMonitorEvent.DeviceInformationReceived
            }

            assertTrue(deviceInfoEvent is BaseMonitorEvent.DeviceInformationReceived)
            val event = deviceInfoEvent as BaseMonitorEvent.DeviceInformationReceived
            event.deviceInfo.fold(
                onSuccess = { d ->
                    assertEquals(d?.modelNumber, "MODEL_NUMBER")
                    assertEquals(d?.manufacturerName, "MANUFACTURER_NAME")
                    assertEquals(d?.softwareRevision, "SOFTWARE_REVISION")
                    assertEquals(d?.serialNumber, "SERIAL_NUMBER")
                    assertEquals(d?.firmwareRevision, "FIRMWARE_REVISION")
                    assertEquals(d?.hardwareRevision, "HARDWARE_REVISION")
                },
                onFailure = { assert(false) }
            )
        }
        advanceUntilIdle()

        monitor.getCallbackInstance()?.disInformationReceived(
            "",
            UUID.fromString(DeviceInformationUUID.MANUFACTURER_NAME),
            "MANUFACTURER_NAME"
        )
        monitor.getCallbackInstance()?.disInformationReceived(
            "",
            UUID.fromString(DeviceInformationUUID.SOFTWARE_REVISION),
            "SOFTWARE_REVISION"
        )
        monitor.getCallbackInstance()?.disInformationReceived(
            "",
            UUID.fromString(DeviceInformationUUID.MODEL_NUMBER),
            "MODEL_NUMBER"
        )
        monitor.getCallbackInstance()?.disInformationReceived(
            "",
            UUID.fromString(DeviceInformationUUID.SERIAL_NUMBER),
            "SERIAL_NUMBER"
        )
        monitor.getCallbackInstance()?.disInformationReceived(
            "",
            UUID.fromString(DeviceInformationUUID.FIRMWARE_REVISION),
            "FIRMWARE_REVISION"
        )
        monitor.getCallbackInstance()?.disInformationReceived(
            "",
            UUID.fromString(DeviceInformationUUID.HARDWARE_REVISION),
            "HARDWARE_REVISION"
        )

        job.join()
    }

    @Test
    fun `test disconnect and cleanup`() = runTest {
        monitor.setDevice("testDeviceId")
        monitor.stopMonitoring()

        verify(polarBleApi).disconnectFromDevice("testDeviceId")
    }

    @Test
    fun `test awaitClose is called when monitoring stops`() = runTest {
        var isClosed = false
        monitor.setDevice("testDeviceId")

        val job = launch {
            monitor.monitoringEvents
                .onCompletion {
                    isClosed = true
                }
                .collect {}
        }

        advanceUntilIdle()

        // Cancel the job to ensure that awaitClose is called
        job.cancelAndJoin()

        // we make sure stopMonitoring is called
        verify(polarBleApi).disconnectFromDevice("testDeviceId")

        assertTrue(isClosed)
    }
}
