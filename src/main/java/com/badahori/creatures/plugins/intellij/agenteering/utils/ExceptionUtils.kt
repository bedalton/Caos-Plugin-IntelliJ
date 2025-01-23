package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.intellij.openapi.progress.ProcessCanceledException

internal fun Throwable.rethrowAnyCancellationException() {

    if (this is kotlin.coroutines.cancellation.CancellationException) {
        throw this
    }

    if (this is ProcessCanceledException) {
        throw this
    }
}