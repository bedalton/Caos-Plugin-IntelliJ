@file:Suppress("ActionPresentationInstantiatedInCtor")

package com.badahori.creatures.plugins.intellij.agenteering.att.actions

import com.badahori.creatures.plugins.intellij.agenteering.att.editor.AttEditorImpl
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseRenderer.PartVisibility.HIDDEN
import com.badahori.creatures.plugins.intellij.agenteering.att.lang.AttMessages
import com.badahori.creatures.plugins.intellij.agenteering.att.lang.PartNames
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import java.util.function.Supplier

open class HidePartAction (
    private val partChar: Char
): AnAction(
    Supplier {
        val partName = PartNames.getPartName(partChar)
        AttMessages.message("hide-part", partName)
    },
    Supplier {
        val partName = PartNames.getPartName(partChar)
        AttMessages.message("hide-part-description", partName.lowercase())
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
            return;
        }
        (editor as? AttEditorImpl)?.togglePartVisibility(partChar, HIDDEN)
    }
}


class HideHeadPart: HidePartAction('a')

class HideBodyPart: HidePartAction('b')

class HideLeftThighPart: HidePartAction('c')

class HideLeftShinPart: HidePartAction('d')

class HideLeftFootPart: HidePartAction('e')

class HideRightThighPart: HidePartAction('f')

class HideRightShinPart: HidePartAction('g')

class HideRightFootPart: HidePartAction('h')

class HideLeftHumerusPart: HidePartAction('i')

class HideLeftRadiusPart: HidePartAction('j')

class HideRightHumerusPart: HidePartAction('k')

class HideRightRadiusPart: HidePartAction('l')

class HideTailBasePart: HidePartAction('m')

class HideTailTipPart: HidePartAction('n')