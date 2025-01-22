package com.badahori.creatures.plugins.intellij.agenteering.att.actions

import com.badahori.creatures.plugins.intellij.agenteering.att.lang.AttMessages
import com.intellij.openapi.actionSystem.ActionPromoter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys

class AttEditorActionPromoter: ActionPromoter {
    override fun promote(actions: MutableList<out AnAction>, context: DataContext): List<AnAction> {
        val editor = context.getData(PlatformDataKeys.FILE_EDITOR)
            ?: return emptyList()
        if (editor.name != AttMessages.message("att-editor.name")) {
            return emptyList()
        }
        return actions.filter { it is AttEditorAction }
    }
}