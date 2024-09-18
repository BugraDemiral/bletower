package com.monomobile.bletower.utils

import timber.log.Timber

class NoOpTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // No-op: Do nothing during tests
    }
}