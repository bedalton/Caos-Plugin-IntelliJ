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


class IncrementHeadPartPose: IncrementPartPoseAction(
    "Head", 'a'
)

class IncrementBodyPartPose: IncrementPartPoseAction(
    "Body", 'b'
)

class IncrementLeftThighPartPose: IncrementPartPoseAction(
    "Left Thigh", 'c'
)

class IncrementLeftShinPartPose: IncrementPartPoseAction(
    "Left Shin", 'd'
)

class IncrementLeftFootPartPose: IncrementPartPoseAction(
    "Left Foot", 'e'
)

class IncrementRightThighPartPose: IncrementPartPoseAction(
    "Right Thigh", 'f'
)

class IncrementRightShinPartPose: IncrementPartPoseAction(
    "Right Shin", 'g'
)

class IncrementRightFootPartPose: IncrementPartPoseAction(
    "Right Foot", 'h'
)

class IncrementLeftHumerusPartPose: IncrementPartPoseAction(
    "Left Humerus", 'i'
)

class IncrementLeftRadiusPartPose: IncrementPartPoseAction(
    "Left Radius", 'j'
)

class IncrementRightHumerusPartPose: IncrementPartPoseAction(
    "Right Humerus", 'k'
)

class IncrementRightRadiusPartPose: IncrementPartPoseAction(
    "Right Radius", 'l'
)

class IncrementTailBasePartPose: IncrementPartPoseAction(
    "Tail Base", 'm'
)

class IncrementTailTipPartPose: IncrementPartPoseAction(
    "Tail Tip", 'n'
)