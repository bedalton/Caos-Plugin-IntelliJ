package com.badahori.creatures.plugins.intellij.agenteering.att.actions

import com.badahori.creatures.plugins.intellij.agenteering.att.editor.AttEditorImpl
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseRenderer
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseRenderer.PartVisibility.GHOST
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseRenderer.PartVisibility.HIDDEN
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys

open class HidePartAction (
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
        (editor as? AttEditorImpl)?.togglePartVisibility(partChar, HIDDEN)
    }
}


class HideHeadPart: HidePartAction(
    "Head", 'a'
)

class HideBodyPart: HidePartAction(
    "Body", 'b'
)

class HideLeftThighPart: HidePartAction(
    "Left Thigh", 'c'
)

class HideLeftShinPart: HidePartAction(
    "Left Shin", 'd'
)

class HideLeftFootPart: HidePartAction(
    "Left Foot", 'e'
)

class HideRightThighPart: HidePartAction(
    "Right Thigh", 'f'
)

class HideRightShinPart: HidePartAction(
    "Right Shin", 'g'
)

class HideRightFootPart: HidePartAction(
    "Right Foot", 'h'
)

class HideLeftHumerusPart: HidePartAction(
    "Left Humerus", 'i'
)

class HideLeftRadiusPart: HidePartAction(
    "Left Radius", 'j'
)

class HideRightHumerusPart: HidePartAction(
    "Right Humerus", 'k'
)

class HideRightRadiusPart: HidePartAction(
    "Right Radius", 'l'
)

class HideTailBasePart: HidePartAction(
    "Tail Base", 'm'
)

class HideTailTipPart: HidePartAction(
    "Tail Tip", 'n'
)