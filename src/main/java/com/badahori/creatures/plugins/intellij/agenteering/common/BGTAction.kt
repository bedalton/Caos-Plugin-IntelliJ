package com.badahori.creatures.plugins.intellij.agenteering.common

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.UpdateSession


/**
 * Updates presentation on the Event Dispatch Thread
 * AnAction's that run on the background threads cannot access Swing menu items
 *
 * @param e
 * @param init
 * @receiver
 */
fun AnAction.updatePresentation(e: AnActionEvent, init: Presentation.() -> Unit) {
    UpdateSession.EMPTY.compute(
        this,
        "Update Presentation",
        ActionUpdateThread.EDT
    ) {
        e.presentation.init()
    }
}