package com.badahori.creatures.plugins.intellij.agenteering.caos.utils

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import org.jetbrains.annotations.NotNull
import java.util.concurrent.Future

fun invokeAndWait (modality:ModalityState, runnable:()->Unit) {
    val application = ApplicationManager.getApplication();
    application.invokeAndWait(runnable, modality)
}

fun invokeLater(runnable: ()->Unit) {
    val application = ApplicationManager.getApplication();
    application.invokeLater(runnable)
}

fun invokeLater (modality:ModalityState, runnable:()->Unit) {
    val application = ApplicationManager.getApplication();
    application.invokeLater(runnable, modality)
}

fun <T> executeOnPooledThread(callable: () -> T) : Future<T> {
    val application = ApplicationManager.getApplication();
    return application.executeOnPooledThread(callable)
}

fun executeOnPooledThread(runnable: Runnable) : Future<*> {
    val application = ApplicationManager.getApplication();
    return application.executeOnPooledThread(runnable)
}