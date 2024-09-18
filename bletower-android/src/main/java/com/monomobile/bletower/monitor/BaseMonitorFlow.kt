package com.monomobile.bletower.monitor

import com.monomobile.bletower.BaseMonitorEvent
import kotlinx.coroutines.flow.Flow

abstract class BaseMonitorFlow {
    protected abstract val monitoringEventsFlow: Flow<BaseMonitorEvent>
    abstract val monitoringEvents: Flow<BaseMonitorEvent>
}