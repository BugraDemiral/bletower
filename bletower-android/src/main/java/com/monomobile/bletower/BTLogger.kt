package com.monomobile.bletower

import timber.log.Timber

object BTLogger {
    fun initLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}