package com.monomobile.bletower.example

import android.content.Context
import com.monomobile.bletower.monitor.BaseMonitor
import com.monomobile.bletower.peripheral.heartrate.HeartRateMonitor
import com.monomobile.bletower.peripheral.heartrate.android.AndroidHeartRateMonitorImpl
import com.monomobile.bletower.peripheral.heartrate.kable.KableHeartRateMonitorImpl
import com.monomobile.bletower.peripheral.heartrate.polar.PolarHeartRateMonitorImpl
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiDefaultImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideHeartRateMonitor(
        @ApplicationContext context: Context,
        scope: CoroutineScope,
        polarBleApi: PolarBleApi,
        scheduler: Scheduler
    ): HeartRateMonitor {
        return if(BuildConfig.Android) {
            AndroidHeartRateMonitorImpl(context, scope)
        } else if(BuildConfig.Kable) {
            KableHeartRateMonitorImpl(scope)
        } else {
            PolarHeartRateMonitorImpl(polarBleApi, scheduler)
        }
    }

    @Provides
    @Singleton
    fun provideCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }

    @Provides
    @Singleton
    fun provideScheduler(): Scheduler {
        return AndroidSchedulers.mainThread()
    }

    @Provides
    @Singleton
    fun providePolarBleApi(@ApplicationContext context: Context): PolarBleApi {
        return PolarBleApiDefaultImpl.defaultImplementation(
            context,
            setOf(
                PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE//,
                //PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                //PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_H10_EXERCISE_RECORDING,
                //PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_OFFLINE_RECORDING,
                //PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                //PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_DEVICE_TIME_SETUP,
                //PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO,
                //PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_LED_ANIMATION
            )
        )
    }
}