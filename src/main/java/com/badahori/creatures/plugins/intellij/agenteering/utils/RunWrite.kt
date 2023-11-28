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

@Deprecated(
    "Use IntelliJ official invokeLater",
    replaceWith = ReplaceWith(
        "com.intellij.openapi.application.invokeLater(runnable)",
        imports = ["com.intellij.openapi.application.invokeLater"]
    )
)
inline fun invokeLater(crossinline runnable: () -> Unit) {
    com.intellij.openapi.application.invokeLater(null, runnable)
}

@Deprecated(
    "Use IntelliJ official invokeLater",
    replaceWith = ReplaceWith(
        "com.intellij.openapi.application.invokeLater(modality, runnable)",
        imports = ["com.intellij.openapi.application.invokeLater"]
    )
)
fun invokeLater(modality: ModalityState, runnable: () -> Unit) {
    com.intellij.openapi.application.invokeLater(modality, runnable)
}


inline fun <T> executeOnPooledThread(crossinline callable: () -> T): Future<T> {
    val application = ApplicationManager.getApplication();
    return application.executeOnPooledThread(Callable { callable() })
}

fun executeOnPooledThread(runnable: Runnable): Future<*> {
    val application = ApplicationManager.getApplication();
    return application.executeOnPooledThread(runnable)
}

@Deprecated(
    "Use IntelliJ official invokeLater",
    replaceWith = ReplaceWith(
        "com.intellij.openapi.application.runWriteAction(runnable)",
        imports = ["com.intellij.openapi.application.runWriteAction"]
    )
)
inline fun <T> runWriteAction(crossinline runnable: () -> T): T {
    return com.intellij.openapi.application.runWriteAction(runnable)
}


@Deprecated(
    "Use IntelliJ official runUndoTransparentAction(runnable)",
    replaceWith = ReplaceWith(
        "com.intellij.openapi.application.runUndoTransparentWriteAction(runnable)",
        imports = ["com.intellij.openapi.application.runUndoTransparentWriteAction"]
    )
)
inline fun <T> runUndoTransparentWriteAction(crossinline runnable: () -> T): T {
    return com.intellij.openapi.application.runUndoTransparentWriteAction(runnable)
}