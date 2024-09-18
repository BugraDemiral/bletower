package com.monomobile.bletower.example

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.monomobile.bletower.example.MainActivity.Companion.TAG
import timber.log.Timber

@Suppress("DEPRECATION", "DEPRECATION")
class BluetoothBondingReceiver(private val deviceBonded: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice::class.java
                    )
                } else {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                val pairingVariant: Int = intent.getIntExtra(
                    BluetoothDevice.EXTRA_PAIRING_VARIANT,
                    BluetoothDevice.ERROR
                )
                Timber.tag(TAG).d("Pairing request from device: " +
                        "${device?.address}, variant: $pairingVariant")

            }
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice::class.java
                    )
                } else {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                val bondState: Int = intent.getIntExtra(
                    BluetoothDevice.EXTRA_BOND_STATE,
                    BluetoothDevice.ERROR
                )
                when (bondState) {
                    BluetoothDevice.BOND_BONDED -> {
                        Timber.tag(TAG).d("Device bonded: ${device?.address}")
                        deviceBonded()
                    }
                    BluetoothDevice.BOND_BONDING -> {
                        Timber.tag(TAG).d("Bonding with device: ${device?.address}")
                    }
                    BluetoothDevice.BOND_NONE -> {
                        Timber.tag(TAG).d( "No bond with device: ${device?.address}")
                    }
                }
            }
        }
    }
}