package com.monomobile.bletower.peripheral.heartrate

import com.monomobile.bletower.BaseMonitorEvent
import com.monomobile.bletower.HeartRateMonitorEvent
import com.monomobile.bletower.peripheral.heartrate.polar.PolarHeartRateMonitorImpl
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.model.PolarHrData
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.TestScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import java.util.concurrent.TimeUnit

class PolarHeartRateMonitorImplTest {
    private lateinit var polarBleApi: PolarBleApi
    private lateinit var testScheduler: TestScheduler
    private lateinit var hrMonitor: PolarHeartRateMonitorImpl
    private lateinit var hrDisposable: Disposable

    @Before
    fun setup() {
        polarBleApi = mock(PolarBleApi::class.java)
        testScheduler = TestScheduler()
        hrMonitor = PolarHeartRateMonitorImpl(polarBleApi, testScheduler)
        hrDisposable = mock(Disposable::class.java)
    }

    /*
    @Test
    fun `startObservingHeartRate successfully emits heart rate events`() = runBlocking {
        val deviceId = "testDeviceId"
        hrMonitor.setDevice(deviceId)

        val hrData = mock(PolarHrData::class.java)
        val sample = PolarHrData.PolarHrSample(
            70,
            listOf(500, 600),
            false,
            false,
            false
        )
        `when`(hrData.samples).thenReturn(listOf(sample))

        val hrFlowable = Flowable.just(hrData).delay(
            100,
            TimeUnit.MILLISECONDS,
            testScheduler
        )
        `when`(polarBleApi.startHrStreaming(deviceId)).thenReturn(hrFlowable)

        hrMonitor.startObservingHeartRate()
        testScheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS)

        val event = hrMonitor.monitoringEvents.first() as HeartRateMonitorEvent.HeartRateRead
        assertEquals(70, event.heartRate.getOrNull())
    }

    @Test
    fun `startObservingHeartRate fails when device is not connected`() = runBlocking {
        hrMonitor.setDevice("")  // No device connected

        hrMonitor.startObservingHeartRate()

        val event = hrMonitor.monitoringEvents.first() as BaseMonitorEvent.MonitoringFailed
        assertTrue(event.error is IllegalStateException)
        assertEquals("Device not connected", event.error.message)
    }

    @Test
    fun `startObservingHeartRate fails when HR object is not disposed`() = runBlocking {
        hrMonitor.setDevice("testDeviceId")
        `when`(hrDisposable.isDisposed).thenReturn(false)
        hrMonitor.setHrDisposable(hrDisposable)

        hrMonitor.startObservingHeartRate()

        val event = hrMonitor.monitoringEvents.first() as BaseMonitorEvent.MonitoringFailed
        assertTrue(event.error is RuntimeException)
        assertEquals("HR object is not disposed", event.error.message)
    }

    @Test
    fun `stopMonitoring properly cleans up resources`() {
        hrMonitor.setHrDisposable(hrDisposable)
        `when`(hrDisposable.isDisposed).thenReturn(false)

        hrMonitor.stopMonitoring()

        verify(hrDisposable).dispose()
        assertNull(hrMonitor.getHrDisposable())
    }

    @Test
    fun `startObservingHeartRate handles exception during HR streaming`() = runBlocking {
        val deviceId = "testDeviceId"
        hrMonitor.setDevice(deviceId)

        val throwable = Throwable("Streaming failed")
        val hrFlowable = Flowable.error<PolarHrData>(throwable)
        `when`(polarBleApi.startHrStreaming(deviceId)).thenReturn(hrFlowable)

        hrMonitor.startObservingHeartRate()
        testScheduler.triggerActions()

        val event = hrMonitor.monitoringEvents.first() as BaseMonitorEvent.MonitoringFailed
        assertEquals(throwable, event.error)
    }

     */
}
