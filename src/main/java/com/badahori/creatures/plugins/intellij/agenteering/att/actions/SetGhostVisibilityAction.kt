package com.badahori.creatures.plugins.intellij.agenteering.att.actions

import com.badahori.creatures.plugins.intellij.agenteering.att.editor.AttEditorImpl
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseRenderer
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseRenderer.PartVisibility.GHOST
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys

open class SetGhostVisibilityAction (
    private val partName: String,
    private val partChar: Char,
): AnAction(
    "Set $partName to Ghost Visibility"
), AttEditorAction {

    override fun isDumbAware(): Boolean {
        return true
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val isAttFile = isVisible(e)
        e.presentation.isEnabledAndVisible = isAttFile
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
        (editor as? AttEditorImpl)?.togglePartVisibility(partChar, GHOST)
    }
}


class SetHeadToGhostVisibility: SetGhostVisibilityAction(
    "Head", 'a'
)

class SetBodyToGhostVisibility: SetGhostVisibilityAction(
    "Body", 'b'
)

class SetLeftThighToGhostVisibility: SetGhostVisibilityAction(
    "Left Thigh", 'c'
)

class SetLeftShinToGhostVisibility: SetGhostVisibilityAction(
    "Left Shin", 'd'
)

class SetLeftFootToGhostVisibility: SetGhostVisibilityAction(
    "Left Foot", 'e'
)

class SetRightThighToGhostVisibility: SetGhostVisibilityAction(
    "Right Thigh", 'f'
)

class SetRightShinToGhostVisibility: SetGhostVisibilityAction(
    "Right Shin", 'g'
)

class SetRightFootToGhostVisibility: SetGhostVisibilityAction(
    "Right Foot", 'h'
)

class SetLeftHumerusToGhostVisibility: SetGhostVisibilityAction(
    "Left Humerus", 'i'
)

class SetLeftRadiusToGhostVisibility: SetGhostVisibilityAction(
    "Left Radius", 'j'
)

class SetRightHumerusToGhostVisibility: SetGhostVisibilityAction(
    "Right Humerus", 'k'
)

class SetRightRadiusToGhostVisibility: SetGhostVisibilityAction(
    "Right Radius", 'l'
)

class SetTailBaseToGhostVisibility: SetGhostVisibilityAction(
    "Tail Base", 'm'
)

class SetTailTipToGhostVisibility: SetGhostVisibilityAction(
    "Tail Tip", 'n'
)