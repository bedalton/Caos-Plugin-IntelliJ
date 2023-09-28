package com.badahori.creatures.plugins.intellij.agenteering.att.actions

import com.badahori.creatures.plugins.intellij.agenteering.att.editor.AttEditorImpl
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys

@Suppress("ComponentNotRegistered")
open class IncrementPartPoseAction (
    private val partName: String,
    private val partChar: Char,
): AnAction (
    "Increment $partName Part Pose"
), AttEditorAction  {

    override fun isDumbAware(): Boolean {
        return true
    }


    override fun update(e: AnActionEvent) {
        super.update(e)
        val isAttFile = isVisible(e)
        e.presentation.isVisible = isAttFile
        e.presentation.isEnabledAndVisible = isAttFile
        e.presentation.isEnabled = isAttFile
    }

    fun isVisible(e: AnActionEvent): Boolean {
        val editor = e.getData(PlatformDataKeys.FILE_EDITOR)
            ?: return false
        return editor.name == AttEditorImpl.NAME
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(PlatformDataKeys.FILE_EDITOR)
            ?: return
        if (editor.name != AttEditorImpl.NAME) {
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