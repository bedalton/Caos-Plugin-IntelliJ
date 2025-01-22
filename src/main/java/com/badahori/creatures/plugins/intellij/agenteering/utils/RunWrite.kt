package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import java.util.concurrent.Callable
import java.util.concurrent.Future
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal inline fun <T> invokeAndWait(modalityState: ModalityState? = ModalityState.defaultModalityState(), crossinline runnable: () -> T): T {
    contract {
        callsInPlace(runnable, InvocationKind.EXACTLY_ONCE)
    }
    val app = ApplicationManager.getApplication()
    return if (app.isDispatchThread) {
        runnable()
    } else {
        computeDelegated {
            app.invokeAndWait({ it(runnable()) }, modalityState ?: ModalityState.defaultModalityState())
        }
    }
}

internal inline fun <T> computeDelegated(executor: (setter: (T) -> Unit) -> Unit): T {
    contract {
        callsInPlace(executor, InvocationKind.EXACTLY_ONCE)
    }
    var resultRef: T? = null
    executor { resultRef = it }
    @Suppress("UNCHECKED_CAST")
    return resultRef as T
}

inline fun <T> executeOnPooledThread(crossinline callable: () -> T): Future<T> {
    val application = ApplicationManager.getApplication()
    return application.executeOnPooledThread(Callable { callable() })
}