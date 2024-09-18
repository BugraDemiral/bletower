package com.monomobile.bletower.peripheral.heartrate

import com.juul.kable.Characteristic
import com.juul.kable.Peripheral
import com.juul.kable.WriteType.WithoutResponse
import com.juul.kable.characteristicOf
import com.monomobile.bletower.BaseMonitorEvent
import com.monomobile.bletower.HeartRateMonitorEvent
import com.monomobile.bletower.peripheral.heartrate.kable.KableHeartRateMonitorImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class KableHeartRateMonitorImplTest {

    private lateinit var monitor: KableHeartRateMonitorImpl
    private lateinit var mockPeripheral: Peripheral
    private val testScope = CoroutineScope(Dispatchers.Unconfined)
    private val characteristic = mock<Characteristic>()

    @Before
    fun setup() {
        mockPeripheral = mock()
        monitor = spy(KableHeartRateMonitorImpl(testScope))
        doReturn(mockPeripheral).whenever(monitor).peripheral
    }

    @Test
    fun `readHeartRate returns successful event`() = runTest {
        val heartRateData = byteArrayOf(0x00, 0x64) // Heart rate = 100

        whenever(mockPeripheral.read(characteristic)).thenReturn(heartRateData)

        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is HeartRateMonitorEvent.HeartRateRead)
                assertEquals(100, (event as HeartRateMonitorEvent.HeartRateRead).heartRate.getOrNull())
            }
        }

        monitor.readMeasurement()

        result.cancel()
    }

    @Test
    fun `readHeartRate returns failure event`() = runTest {
        val exception = RuntimeException("Read failed")
        whenever(mockPeripheral.read(characteristic)).thenThrow(exception)

        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is HeartRateMonitorEvent.HeartRateRead)
                assertEquals(exception, (event as HeartRateMonitorEvent.HeartRateRead).heartRate.exceptionOrNull())
            }
        }

        monitor.readMeasurement()

        result.cancel()
    }

    @Test
    fun `readHeartRate returns error when peripheral is not connected`() = runTest {
        doReturn(null).whenever(monitor).peripheral

        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is HeartRateMonitorEvent.HeartRateRead)
                assertTrue((event as HeartRateMonitorEvent.HeartRateRead).heartRate.isFailure)
                assertTrue(event.heartRate.exceptionOrNull() is IllegalStateException)
            }
        }

        monitor.readMeasurement()

        result.cancel()
    }

    @Test
    fun `readBatteryLevel returns success`() = runTest {
        val batteryData = byteArrayOf(0x64) // Battery = 100%
        whenever(mockPeripheral.read(characteristic)).thenReturn(batteryData)

        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is BaseMonitorEvent.BatteryLevelRead)
                assertEquals(100, (event as BaseMonitorEvent.BatteryLevelRead).batteryLevel.getOrNull())
            }
        }

        monitor.readBatteryLevel()

        result.cancel()
    }

    @Test
    fun `readBatteryLevel returns failure event`() = runTest {
        val exception = RuntimeException("Read failed")
        whenever(mockPeripheral.read(characteristic)).thenThrow(exception)

        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is BaseMonitorEvent.BatteryLevelRead)
                assertEquals(exception, (event as BaseMonitorEvent.BatteryLevelRead).batteryLevel.exceptionOrNull())
            }
        }

        monitor.readBatteryLevel()

        result.cancel()
    }

    @Test
    fun `resetEnergyExpended returns success`() = runTest {
        val controlPointCharacteristic = characteristicOf(
            service = HeartRateUUID.SERVICE,
            characteristic = HeartRateUUID.CONTROL_POINT
        )
        val resetCommand = byteArrayOf(0x01)

        whenever(mockPeripheral.write(eq(controlPointCharacteristic), eq(resetCommand), eq(WithoutResponse))).thenReturn(Unit)

        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is HeartRateMonitorEvent.EnergyExpendedReset)
                assertTrue((event as HeartRateMonitorEvent.EnergyExpendedReset).result.isSuccess)
            }
        }

        monitor.resetEnergyExpended()

        result.cancel()
    }

    @Test
    fun `resetEnergyExpended returns failure event`() = runTest {
        val exception = RuntimeException("Reset failed")
        whenever(mockPeripheral.write(any(), any(), any())).thenThrow(exception)

        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is HeartRateMonitorEvent.EnergyExpendedReset)
                assertEquals(exception, (event as HeartRateMonitorEvent.EnergyExpendedReset).result.exceptionOrNull())
            }
        }

        monitor.resetEnergyExpended()

        result.cancel()
    }

    @Test
    fun `startObservingHeartRate emits heart rate successfully`() = runTest {
        val heartRateData = byteArrayOf(0x00, 75) // heart rate: 75 BPM

        whenever(mockPeripheral.observe(characteristic)).thenReturn(flowOf(heartRateData))

        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is HeartRateMonitorEvent.HeartRateRead)
                assertEquals(75, (event as HeartRateMonitorEvent.HeartRateRead).heartRate.getOrNull())
            }
        }

        monitor.startObservingMeasurement()
        result.cancel()
    }

    @Test
    fun `startObservingHeartRate emits failure on exception`() = runTest {
        val exception = RuntimeException("Heart rate observation failed")
        whenever(mockPeripheral.observe(characteristic)).thenThrow(exception)

        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is HeartRateMonitorEvent.HeartRateRead)
                assertEquals(exception, (event as HeartRateMonitorEvent.HeartRateRead).heartRate.exceptionOrNull())
            }
        }

        monitor.startObservingMeasurement()
        result.cancel()
    }

    @Test
    fun `startObservingBattery emits battery level successfully`() = runTest {
        val batteryLevelData = byteArrayOf(50) // battery level: 50%

        whenever(mockPeripheral.observe(characteristic)).thenReturn(flowOf(batteryLevelData))

        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is BaseMonitorEvent.BatteryLevelRead)
                assertEquals(50, (event as BaseMonitorEvent.BatteryLevelRead).batteryLevel.getOrNull())
            }
        }

        monitor.startObservingBattery()
        result.cancel()
    }

    @Test
    fun `startObservingBattery emits failure on exception`() = runTest {
        val exception = RuntimeException("Battery observation failed")
        whenever(mockPeripheral.observe(characteristic)).thenThrow(exception)

        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is BaseMonitorEvent.BatteryLevelRead)
                assertEquals(exception, (event as BaseMonitorEvent.BatteryLevelRead).batteryLevel.exceptionOrNull())
            }
        }

        monitor.startObservingBattery()
        result.cancel()
    }

    @Test
    fun `readHeartRate emits successful heart rate reading`() = runTest {
        val heartRateData = byteArrayOf(0x00, 80) // heart rate: 80 BPM

        whenever(mockPeripheral.read(characteristic)).thenReturn(heartRateData)

        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is HeartRateMonitorEvent.HeartRateRead)
                assertEquals(80, (event as HeartRateMonitorEvent.HeartRateRead).heartRate.getOrNull())
            }
        }

        monitor.readMeasurement()
        result.cancel()
    }

    @Test
    fun `readHeartRate emits failure on exception`() = runTest {
        val exception = RuntimeException("Heart rate read failed")
        whenever(mockPeripheral.read(characteristic)).thenThrow(exception)

        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is HeartRateMonitorEvent.HeartRateRead)
                assertEquals(exception, (event as HeartRateMonitorEvent.HeartRateRead).heartRate.exceptionOrNull())
            }
        }

        monitor.readMeasurement()
        result.cancel()
    }

    @Test
    fun `readSensorLocation emits successful sensor location reading`() = runTest {
        val sensorLocationData = byteArrayOf(1) // sensor location: Chest

        whenever(mockPeripheral.read(characteristic)).thenReturn(sensorLocationData)

        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is HeartRateMonitorEvent.SensorLocationRead)
                assertEquals("Chest", (event as HeartRateMonitorEvent.SensorLocationRead).sensorLocation.getOrNull())
            }
        }

        monitor.readSensorLocation()
        result.cancel()
    }

    @Test
    fun `readSensorLocation emits failure on exception`() = runTest {
        val exception = RuntimeException("Sensor location read failed")
        whenever(mockPeripheral.read(characteristic)).thenThrow(exception)

        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is HeartRateMonitorEvent.SensorLocationRead)
                assertEquals(exception, (event as HeartRateMonitorEvent.SensorLocationRead).sensorLocation.exceptionOrNull())
            }
        }

        monitor.readSensorLocation()
        result.cancel()
    }

    @Test
    fun `resetEnergyExpended emits successful energy expended reset`() = runTest {
        val resetCommand = byteArrayOf(0x01)
        whenever(mockPeripheral.write(characteristic, resetCommand)).thenReturn(Unit)

        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is HeartRateMonitorEvent.EnergyExpendedReset)
                assertTrue((event as HeartRateMonitorEvent.EnergyExpendedReset).result.getOrNull() ?: false)
            }
        }

        monitor.resetEnergyExpended()
        result.cancel()
    }

    @Test
    fun `resetEnergyExpended emits failure on exception`() = runTest {
        val exception = RuntimeException("Reset failed")
        val resetCommand = byteArrayOf(0x01)
        whenever(mockPeripheral.write(characteristic, resetCommand)).thenThrow(exception)

        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is HeartRateMonitorEvent.EnergyExpendedReset)
                assertEquals(exception, (event as HeartRateMonitorEvent.EnergyExpendedReset).result.exceptionOrNull())
            }
        }

        monitor.resetEnergyExpended()
        result.cancel()
    }

    @Test
    fun `readBatteryLevel emits failure when data is empty`() = runTest {
        whenever(mockPeripheral.read(characteristic)).thenReturn(byteArrayOf())

        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is BaseMonitorEvent.BatteryLevelRead)
                val failure = (event as BaseMonitorEvent.BatteryLevelRead).batteryLevel.exceptionOrNull()
                assertTrue(failure is IllegalStateException)
                assertEquals("Battery data is invalid", failure?.message)
            }
        }

        monitor.readBatteryLevel()
        result.cancel()
    }

    @Test
    fun `readSensorLocation emits failure for invalid sensor location data`() = runTest {
        val invalidSensorLocationData = byteArrayOf(10) // Invalid location (not mapped)

        whenever(mockPeripheral.read(characteristic)).thenReturn(invalidSensorLocationData)

        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is HeartRateMonitorEvent.SensorLocationRead)
                val sensorLocation = (event as HeartRateMonitorEvent.SensorLocationRead).sensorLocation.getOrNull()
                assertEquals("Unknown", sensorLocation)
            }
        }

        monitor.readSensorLocation()
        result.cancel()
    }

    @Test
    fun `startObservingHeartRate emits failure on invalid heart rate data`() = runTest {
        val invalidHeartRateData = byteArrayOf(0x00) // Only one byte, which is incomplete

        whenever(mockPeripheral.observe(characteristic)).thenReturn(flowOf(invalidHeartRateData))

        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is HeartRateMonitorEvent.HeartRateRead)
                assertTrue((event as HeartRateMonitorEvent.HeartRateRead).heartRate.isFailure)
            }
        }

        monitor.startObservingMeasurement()
        result.cancel()
    }

    @Test
    fun `startObservingHeartRate emits failure if peripheral is null`() = runTest {
        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is HeartRateMonitorEvent.HeartRateRead)
                assertTrue((event as HeartRateMonitorEvent.HeartRateRead).heartRate.isFailure)
                val failure = event.heartRate.exceptionOrNull()
                assertTrue(failure is IllegalStateException)
                assertEquals("Peripheral is not connected", failure?.message)
            }
        }

        monitor.startObservingMeasurement()
        result.cancel()
    }

    @Test
    fun `readHeartRate emits failure on timeout`() = runTest {
        runBlocking {
            delay(1000L) // Simulate timeout
            byteArrayOf(0x00, 60) // Simulated heart rate data
        }

        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is HeartRateMonitorEvent.HeartRateRead)
                val failure = (event as HeartRateMonitorEvent.HeartRateRead).heartRate.exceptionOrNull()
                assertTrue(failure is TimeoutCancellationException)
            }
        }

        monitor.readMeasurement()
        result.cancel()
    }

    @Test
    fun `readHeartRate emits failure on null data`() = runTest {
        whenever(mockPeripheral.read(characteristic)).thenReturn(null)

        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is HeartRateMonitorEvent.HeartRateRead)
                val failure = (event as HeartRateMonitorEvent.HeartRateRead).heartRate.exceptionOrNull()
                assertTrue(failure is IllegalStateException)
                assertEquals("Heart rate data is invalid", failure?.message)
            }
        }

        monitor.readMeasurement()
        result.cancel()
    }

    @Test
    fun `readHeartRate emits failure for empty heart rate data`() = runTest {
        whenever(mockPeripheral.read(characteristic)).thenReturn(byteArrayOf())

        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is HeartRateMonitorEvent.HeartRateRead)
                val failure = (event as HeartRateMonitorEvent.HeartRateRead).heartRate.exceptionOrNull()
                assertTrue(failure is IllegalStateException)
                assertEquals("Heart rate data is invalid", failure?.message)
            }
        }

        monitor.readMeasurement()
        result.cancel()
    }

    @Test
    fun `readBatteryLevel emits failure when peripheral is null`() = runTest {
        val result = launch {
            monitor.monitoringEvents.collect { event ->
                assertTrue(event is BaseMonitorEvent.BatteryLevelRead)
                assertTrue((event as BaseMonitorEvent.BatteryLevelRead).batteryLevel.isFailure)
                val failure = event.batteryLevel.exceptionOrNull()
                assertTrue(failure is IllegalStateException)
                assertEquals("Peripheral is not connected", failure?.message)
            }
        }

        monitor.readBatteryLevel()
        result.cancel()
    }
}
