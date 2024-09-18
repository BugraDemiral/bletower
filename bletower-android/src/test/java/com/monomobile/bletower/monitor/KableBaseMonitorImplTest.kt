package com.monomobile.bletower.monitor

import android.bluetooth.le.ScanSettings
import com.juul.kable.Peripheral
import com.juul.kable.PlatformAdvertisement
import com.juul.kable.PlatformScanner
import com.juul.kable.State
import com.monomobile.bletower.BaseMonitorEvent
import com.monomobile.bletower.monitor.kable.KableBaseMonitorImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class KableBaseMonitorTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val mockScanner = mock<PlatformScanner>()
    private val mockPeripheral = mock<Peripheral>()
    private val monitor = spy(KableBaseMonitorImpl(scope))

    private val mockScanSettings = mock(ScanSettings::class.java)
    private val advertisementFlow = MutableSharedFlow<PlatformAdvertisement>()

    private val scanSettingsBuilder = mock(ScanSettings.Builder::class.java)

    @Before
    fun setup() {
        whenever(mockScanner.advertisements).thenReturn(advertisementFlow.asSharedFlow())

        whenever(scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY))
            .thenReturn(scanSettingsBuilder)
        whenever(scanSettingsBuilder.build()).thenReturn(mockScanSettings)

        // Mocking the behavior of ScanSettings.Builder
        whenever(scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY))
            .thenReturn(scanSettingsBuilder)
        whenever(scanSettingsBuilder.build()).thenReturn(mockScanSettings)

    }

    @Test
    fun `should emit MonitoringFailed event on failed connection`() = runTest {
        val advertisement = mock<PlatformAdvertisement>()
        val stateFlow = MutableStateFlow<State>(State.Connected)
        whenever(mockPeripheral.state).thenReturn(stateFlow)
        whenever(monitor.peripheral).thenReturn(mockPeripheral)

        val events = mutableListOf<BaseMonitorEvent>()
        val job = monitor.monitoringEvents
            .take(1)
            .onEach() {
                events.add(it)
            }.launchIn(this)

        monitor.startMonitoring(mockScanner) {}
        advanceUntilIdle()

        advertisementFlow.emit(advertisement)

        job.join()

        assert(events.isNotEmpty())
        assert(events.first() is BaseMonitorEvent.MonitoringFailed)
    }

    @Test
    fun `should emit MonitoringFailed event on scan failure`() = runTest {
        val advertisement = mock<PlatformAdvertisement>()
        val events = mutableListOf<BaseMonitorEvent>()
        val job = monitor.monitoringEvents
            .take(1)
            .onEach { events.add(it) }.launchIn(this)

        //whenever(mockScanner.advertisements).thenThrow(RuntimeException("Scan failed"))

        monitor.startMonitoring(mockScanner) {}
        advanceUntilIdle()

        advertisementFlow.emit(advertisement)

        job.join()

        assert(events.isNotEmpty())
        assert(events.first() is BaseMonitorEvent.MonitoringFailed)
    }
}

