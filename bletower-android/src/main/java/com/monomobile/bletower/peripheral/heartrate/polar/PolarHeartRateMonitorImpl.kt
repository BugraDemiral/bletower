package com.monomobile.bletower.peripheral.heartrate.polar

import com.monomobile.bletower.BaseMonitorEvent
import com.monomobile.bletower.HeartRateMonitorEvent
import com.monomobile.bletower.monitor.polar.PolarBaseMonitorImpl
import com.monomobile.bletower.peripheral.heartrate.HeartRateMonitor
import com.polar.sdk.api.PolarBleApi
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import org.jetbrains.annotations.VisibleForTesting
import timber.log.Timber

class PolarHeartRateMonitorImpl(
    private val polarBleApi: PolarBleApi,
    private val scheduler: Scheduler = AndroidSchedulers.mainThread()
) : PolarBaseMonitorImpl(polarBleApi, scheduler), HeartRateMonitor {
    companion object {
        const val TAG = "PolarHeartRateMonitorImpl"
    }

    private var hrDisposable: Disposable? = null

    @VisibleForTesting
    fun setHrDisposable(disposable: Disposable) { hrDisposable = disposable }
    @VisibleForTesting
    fun getHrDisposable() { hrDisposable }

    override fun readSensorLocation() {
        TODO("Not yet implemented")
    }

    override fun readMeasurement() {
        TODO("Not yet implemented")
    }

    override fun readBatteryLevel() {
        TODO("Not yet implemented")
    }

    override fun resetEnergyExpended() {
        TODO("Not yet implemented")
    }

    override fun startObservingMeasurement() {
        try {
            val isDisposed = hrDisposable?.isDisposed ?: true
            if (isDisposed) {
                deviceId?.let {
                    hrDisposable = polarBleApi.startHrStreaming(it)
                        .observeOn(scheduler)
                        .subscribe(
                            { hrData ->
                                for (sample in hrData.samples) {
                                    fireTrySend(
                                        HeartRateMonitorEvent.HeartRateRead(
                                            Result.success(sample.hr)
                                        )
                                    )
                                    Timber.tag(TAG).i(
                                        "HR ${sample.hr} RR ${sample.rrsMs}"
                                    )
                                }
                            },
                            { throwable ->
                                Timber.tag(TAG).e(throwable)
                                fireTrySend(
                                    BaseMonitorEvent.MonitoringFailed(throwable)
                                )
                            }
                        )
                } ?: run {
                    val exception = IllegalStateException("Device not connected")
                    Timber.tag(TAG).e(exception)
                    fireTrySend(BaseMonitorEvent.MonitoringFailed(exception))
                }
            } else {
                fireTrySend(
                    BaseMonitorEvent.MonitoringFailed(
                    RuntimeException("HR object is not disposed"))
                )
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e)
            fireTrySend(BaseMonitorEvent.MonitoringFailed(e))
        }
    }

    override fun startObservingBattery() {
        TODO("Not yet implemented")
    }

    override fun stopMonitoring() {
        super.stopMonitoring()

        hrDisposable?.dispose()
        hrDisposable = null
    }
}
