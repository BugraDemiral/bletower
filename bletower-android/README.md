
# BLETowerSDK for Android / iOS

## Overview

The **BLETowerSDK** for mobile provides an easy-to-use Coroutines-powered API for interacting with Bluetooth Low Energy (BLE) devices. The library is designed to support common BLE devices such as fitness trackers, health wearables, and smart home devices, allowing for smooth integration into your application.

### Key Features:
- **Coroutines Support**: Enables asynchronous BLE operations.
- **Multi-engine Support**: Integrates with Kable, Polar, and native Android BLE engines.
- **Heart Rate Monitoring**: Includes a dedicated monitor for heart rate devices across engines.
- **Easy Scanning**: Built-in support for scanning nearby BLE devices with configurable filters.
- **Custom Engine Support**: Allows developers to write their own BLE engine for monitoring.
- **BLE peripherals**: Implement new peripherals by using simple common interfaces with ease
---

## Architecture

The SDK is structured into several core packages, each focusing on a specific BLE monitoring function:

### 1. **Monitor**: Core monitoring functionality.
   - **Android**: Implements BLE monitoring using native Android APIs.
   - **Kable**: Integrates the Kable library for BLE monitoring.
   - **Polar**: Provides Polar device-specific monitoring.
   - **Custom**: Add your favorite BLE engine

### 2. **Peripheral (Heart Rate Monitoring)**: Dedicated monitoring for heart rate devices across all supported engines.
   - **AndroidHeartRateMonitorImpl**
   - **KableHeartRateMonitorImpl**
   - **PolarHeartRateMonitorImpl**
   - **New Devices** will be added in the future!

### 3. **Utilities**: Utility classes for error handling, logging, device information retrieval, and UUID management.

---

## Example Usage

Please refer to [examples/example.android] for Heart Rate Monitor usage with three different engines.

To implement a new engine called `MyCustomBaseMonitor` you must use following interface.

```Android
interface MyCustomBaseMonitor: BaseMonitor {
    fun startMonitoring()
    fun stopMonitoring()
    fun connectToPeripheral(autoConnect: Boolean = false)
    fun disconnectFromPeripheral()
    fun readDeviceInformation()
}
```

### Monitoring Peripheral Data

To implement a peripheral called `GlucoseMonitor` you must use following interface.

```Android
interface PeripheralMonitor: BaseMonitor {
    fun readSensorLocation()
    fun readMeasurement()
    fun readBatteryLevel()
    fun resetEnergyExpended()
    fun startObservingMeasurement()
    fun startObservingBattery()
}

// using above as a base;

interface GlucoseMonitor: PeripheralMonitor {
    // add custom methods
}
```

---

## Setup

### Installation

To use the BLETower SDK in your Android project, include the following dependency in your `build.gradle`:

```gradle
repositories {
    mavenCentral()
}

dependencies {
    implementation 'com.monomobile.bletower:1.0.0'
}
```

### Permissions

Make sure to add the required permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
```

For Android 12 (API level 31) and above, also add:

```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"/>
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>
```

---

## License

This SDK is licensed under the MIT License. See `LICENSE` for more information.
