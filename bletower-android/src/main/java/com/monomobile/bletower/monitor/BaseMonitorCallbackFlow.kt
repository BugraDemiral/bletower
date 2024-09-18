package com.monomobile.bletower.monitor

import com.monomobile.bletower.BaseMonitorEvent
import com.monomobile.bletower.monitor.android.AndroidBaseMonitorImpl.Companion.TAG
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.onFailure
import timber.log.Timber

abstract class BaseMonitorCallbackFlow: BaseMonitorFlow() {
    protected var sendChannel: SendChannel<BaseMonitorEvent>? = null

    protected fun fireTrySend(event: BaseMonitorEvent) {
        sendChannel?.let {
            it.trySend(event)
                .onFailure { result ->
                    Timber.tag(TAG).i(
                        "Failed to send ${event.javaClass.name} event: ${result?.message}"
                    )
                }
        }
    }
}