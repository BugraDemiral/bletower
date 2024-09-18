package com.monomobile.bletower.peripheral.heartrate

import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import com.monomobile.bletower.BaseMonitorEvent
import com.monomobile.bletower.HeartRateMonitorEvent
import com.monomobile.bletower.peripheral.heartrate.android.AndroidHeartRateMonitorImpl
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import java.util.*

class AndroidHeartRateMonitorImplTest {

    private lateinit var androidHeartRateMonitorImpl: AndroidHeartRateMonitorImpl
    private val testScope = TestScope()

    @Before
    fun setUp() {
        androidHeartRateMonitorImpl = AndroidHeartRateMonitorImpl(mock(Context::class.java), testScope)
    }

    @Test
    fun `onCustomCharacteristicRead with heart rate measurement returns HeartRateRead event`() {
        val characteristic = mock(BluetoothGattCharacteristic::class.java)
        `when`(characteristic.uuid).thenReturn(UUID.fromString(HeartRateUUID.MEASUREMENT))
        val heartRateData = byteArrayOf(0x64)

        val event = androidHeartRateMonitorImpl.onCustomCharacteristicRead(characteristic, heartRateData)

        assertTrue(event is HeartRateMonitorEvent.HeartRateRead)
        val heartRateRead = event as HeartRateMonitorEvent.HeartRateRead
        assertEquals(100, heartRateRead.heartRate.getOrNull())
    }

    @Test
    fun `onCustomCharacteristicRead with battery level returns BatteryLevelRead event`() {
        val characteristic = mock(BluetoothGattCharacteristic::class.java)
        `when`(characteristic.uuid).thenReturn(UUID.fromString(HeartRateUUID.BATTERY_LEVEL))
        val batteryData = byteArrayOf(0x64) // Battery level: 100

        val event = androidHeartRateMonitorImpl.onCustomCharacteristicRead(characteristic, batteryData)

        assertTrue(event is BaseMonitorEvent.BatteryLevelRead)
        val batteryRead = event as BaseMonitorEvent.BatteryLevelRead
        assertEquals(100, batteryRead.batteryLevel.getOrNull())
    }

    @Test
    fun `onCustomCharacteristicRead with sensor location returns SensorLocationRead event`() {
        val characteristic = mock(BluetoothGattCharacteristic::class.java)
        `when`(characteristic.uuid).thenReturn(UUID.fromString(HeartRateUUID.BODY_SENSOR_LOCATION))
        val sensorLocationData = byteArrayOf(0x01) // Location: Chest

        val event = androidHeartRateMonitorImpl.onCustomCharacteristicRead(characteristic, sensorLocationData)

        assertTrue(event is HeartRateMonitorEvent.SensorLocationRead)
        val sensorLocationRead = event as HeartRateMonitorEvent.SensorLocationRead
        assertEquals("Chest", sensorLocationRead.sensorLocation.getOrNull())
    }

}
