package com.monomobile.bletower.example.ui

import android.bluetooth.BluetoothProfile
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.monomobile.bletower.BaseMonitorEvent
import com.monomobile.bletower.HeartRateMonitorEvent
import com.monomobile.bletower.example.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val homeViewModel: HomeViewModel by activityViewModels()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.btStartMonitoring.isEnabled = false
        binding.btConnect.isEnabled = false
        binding.btRead.isEnabled = false

        "".also { binding.tvHeartRate.text = it }
        "".also { binding.tvBatteryLevel.text = it }
        "".also { binding.tvSensorLocation.text = it }

        binding.btStartMonitoring.setOnClickListener {
            binding.btStartMonitoring.isEnabled = false
            binding.btConnect.isEnabled = false
            binding.btRead.isEnabled = false

            homeViewModel.startHeartRateMonitor()
        }

        binding.btConnect.setOnClickListener {
            homeViewModel.connectToPeripheral()
        }

        binding.btRead.setOnClickListener {
            homeViewModel.readData()
        }

        homeViewModel.permissionsGranted.observe(viewLifecycleOwner) {
            binding.btStartMonitoring.isEnabled = it
        }

        homeViewModel.handleEvent.observe(viewLifecycleOwner) { event ->
            when(event) {
                is BaseMonitorEvent.DeviceFound -> {
                    binding.btConnect.isEnabled = true
                    binding.btStartMonitoring.isEnabled = true
                }

                is BaseMonitorEvent.ServiceDiscovered -> {
                    binding.btRead.isEnabled = true
                }

                is HeartRateMonitorEvent.HeartRateRead -> {
                    event.heartRate.fold(
                        onSuccess = { value ->
                            binding.tvHeartRate.text = value.toString()
                                    },
                        onFailure = { e ->
                            binding.tvHeartRate.text = e.localizedMessage
                        }
                    )
                }

                is BaseMonitorEvent.BatteryLevelRead -> {
                    event.batteryLevel.fold(
                        onSuccess = { value ->
                            binding.tvBatteryLevel.text = value.toString()
                        },
                        onFailure = { e ->
                            binding.tvBatteryLevel.text = e.localizedMessage
                        }
                    )
                }

                is HeartRateMonitorEvent.SensorLocationRead -> {
                    event.sensorLocation.fold(
                        onSuccess = { value ->
                            binding.tvSensorLocation.text = value.toString()
                        },
                        onFailure = { e ->
                            binding.tvSensorLocation.text = e.localizedMessage
                        }
                    )
                }

                is BaseMonitorEvent.ConnectionStateChanged -> {
                    when(event.connectionState) {
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            binding.btConnect.isEnabled = false
                            binding.btRead.isEnabled = false
                        }
                    }
                }

                is BaseMonitorEvent.MonitoringFailed -> {
                    binding.btStartMonitoring.isEnabled = true
                }
                else -> {}
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}