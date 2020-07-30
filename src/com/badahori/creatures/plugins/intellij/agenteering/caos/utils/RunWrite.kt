package com.badahori.creatures.plugins.intellij.agenteering.caos.utils

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import org.jetbrains.annotations.NotNull

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