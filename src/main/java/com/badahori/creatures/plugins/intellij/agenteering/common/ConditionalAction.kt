package com.badahori.creatures.plugins.intellij.agenteering.common

import com.intellij.openapi.actionSystem.AnActionEvent

internal interface ConditionalAction {
    fun isEnabled(e: AnActionEvent): Boolean

}