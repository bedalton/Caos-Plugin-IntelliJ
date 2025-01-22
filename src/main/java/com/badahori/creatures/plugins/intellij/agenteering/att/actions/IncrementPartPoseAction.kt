@file:Suppress("ActionPresentationInstantiatedInCtor")

package com.badahori.creatures.plugins.intellij.agenteering.att.actions

import com.badahori.creatures.plugins.intellij.agenteering.att.editor.AttEditorImpl
import com.badahori.creatures.plugins.intellij.agenteering.att.lang.AttMessages
import com.badahori.creatures.plugins.intellij.agenteering.att.lang.PartNames
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import java.util.function.Supplier

open class IncrementPartPoseAction (
    private val partChar: Char,
): AnAction (
    Supplier {
        val partName = PartNames.getPartName(partChar)
        AttMessages.message("increment-part-pose", partName)
    },
    Supplier {
        val partName = PartNames.getPartName(partChar)
        AttMessages.message("increment-part-pose-description", partName.lowercase())
    },
    null,
), AttEditorAction  {

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
        (editor as? AttEditorImpl)?.incrementPartPose(partChar)
    }
}


class IncrementHeadPartPose: IncrementPartPoseAction('a')

class IncrementBodyPartPose: IncrementPartPoseAction('b')

class IncrementLeftThighPartPose: IncrementPartPoseAction('c')

class IncrementLeftShinPartPose: IncrementPartPoseAction('d')

class IncrementLeftFootPartPose: IncrementPartPoseAction('e')

class IncrementRightThighPartPose: IncrementPartPoseAction('f')

class IncrementRightShinPartPose: IncrementPartPoseAction('g')

class IncrementRightFootPartPose: IncrementPartPoseAction('h')

class IncrementLeftHumerusPartPose: IncrementPartPoseAction('i')

class IncrementLeftRadiusPartPose: IncrementPartPoseAction('j')

class IncrementRightHumerusPartPose: IncrementPartPoseAction('k')

class IncrementRightRadiusPartPose: IncrementPartPoseAction('l')

class IncrementTailBasePartPose: IncrementPartPoseAction('m')

class IncrementTailTipPartPose: IncrementPartPoseAction('n')