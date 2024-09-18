
# BLETowerExample for Android

## Overview

The **BLETowerSDK Example** provides Heart Rate Monitor implementation for Native, Kable and Polar SDK BLE engines.

### Key Features:
- **Monitor**: Heart rate peripheral.
- **Connect**: Connect to the device by BuildConfig selected BLE engine which could be changed in app gradle.
- **Read**: Read heart rate from the peripheral upon connect.
- **Show data**: Display heart rate value.

---

## Example Usage

Please refer to HomeViewModel.kt for detailed demonstration.

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
