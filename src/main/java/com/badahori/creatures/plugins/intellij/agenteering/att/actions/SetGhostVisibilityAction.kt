@file:Suppress("ActionPresentationInstantiatedInCtor")

package com.badahori.creatures.plugins.intellij.agenteering.att.actions

import com.badahori.creatures.plugins.intellij.agenteering.att.editor.AttEditorImpl
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseRenderer.PartVisibility.GHOST
import com.badahori.creatures.plugins.intellij.agenteering.att.lang.AttMessages
import com.badahori.creatures.plugins.intellij.agenteering.att.lang.PartNames
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import java.util.function.Supplier

open class SetGhostVisibilityAction (
    private val partChar: Char,
): AnAction(
    Supplier {
        val partName = PartNames.getPartName(partChar)
        AttMessages.message("set-part-ghost-visibility", partName)
    },
    Supplier {
        val partName = PartNames.getPartName(partChar)
        AttMessages.message("set-part-ghost-visibility-description", partName.lowercase())
    },
    null,
), AttEditorAction {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun isDumbAware(): Boolean {
        return true
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val isAttFile = isVisible(e)
        e.presentation.isEnabled = isAttFile
    }

    fun isVisible(e: AnActionEvent): Boolean {
        val editor = e.getData(PlatformDataKeys.FILE_EDITOR)
            ?: return false
        return editor.name == AttEditorImpl.getEditorName()
    }


    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(PlatformDataKeys.FILE_EDITOR)
            ?: return
        if (editor.name != AttEditorImpl.getEditorName()) {
            return
        }
        (editor as? AttEditorImpl)?.togglePartVisibility(partChar, GHOST)
    }
}

class SetHeadToGhostVisibility: SetGhostVisibilityAction('a')

class SetBodyToGhostVisibility: SetGhostVisibilityAction('b')

class SetLeftThighToGhostVisibility: SetGhostVisibilityAction('c')

class SetLeftShinToGhostVisibility: SetGhostVisibilityAction('d')

class SetLeftFootToGhostVisibility: SetGhostVisibilityAction('e')

class SetRightThighToGhostVisibility: SetGhostVisibilityAction('f')

class SetRightShinToGhostVisibility: SetGhostVisibilityAction('g')

class SetRightFootToGhostVisibility: SetGhostVisibilityAction('h')

class SetLeftHumerusToGhostVisibility: SetGhostVisibilityAction('i')

class SetLeftRadiusToGhostVisibility: SetGhostVisibilityAction('j')

class SetRightHumerusToGhostVisibility: SetGhostVisibilityAction('k')

class SetRightRadiusToGhostVisibility: SetGhostVisibilityAction('l')

class SetTailBaseToGhostVisibility: SetGhostVisibilityAction('m')

class SetTailTipToGhostVisibility: SetGhostVisibilityAction('n')