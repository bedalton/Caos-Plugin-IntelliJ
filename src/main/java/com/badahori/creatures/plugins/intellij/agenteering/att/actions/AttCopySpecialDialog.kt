package com.badahori.creatures.plugins.intellij.agenteering.att.actions

import com.badahori.creatures.plugins.intellij.agenteering.att.lang.AttMessages
import com.badahori.creatures.plugins.intellij.agenteering.utils.lowercase
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScopes
import javax.swing.*

class AttCopySpecialDialog(val project: Project, private val thisAtt: VirtualFile) : DialogBuilder(project) {

    private val thisPart: Char by lazy {
        thisAtt.name[0].lowercase()
    }

    private val mirroredPart: Char? by lazy {
        opposite(thisPart)?.lowercase()
    }

    val left: Char? by lazy {
        when {
            thisPart in leftSided -> thisPart
            mirroredPart != null -> opposite(mirroredPart!!)
            else -> null
        }?.lowercase()
    }

    val right: Char? by lazy {
        left?.let {
            opposite(it)
        }?.lowercase()
    }

    var other:VirtualFile? = null

    val index by lazy {
        thisAtt.nameWithoutExtension.substring(1).lowercase()
    }
    private val copyLeftValuesOverRight = JRadioButton(AttMessages.message("mirror-left-values-over-right-in-file"))
    private val copyRightValuesOverLeft = JRadioButton(AttMessages.message("mirror-right-values-over-left-in-file"))

    private val copyThisOverOther = JRadioButton(AttMessages.message(
        "mirror-this-att-over-att",
        thisAtt.nameWithoutExtension,
        "$mirroredPart${thisAtt.nameWithoutExtension}"
    ))

    private val copyOtherOverThis = JRadioButton(AttMessages.message(
        "mirror-corresponding-att-over-this-att",
        "$mirroredPart${thisAtt.nameWithoutExtension}",
        thisAtt.nameWithoutExtension
    ))

    init {

        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(JLabel(AttMessages.message("choose-att-copy-options")))


        val radioGroup = ButtonGroup()
        radioGroup.add(copyLeftValuesOverRight)
        radioGroup.add(copyRightValuesOverLeft)

        val otherFile = if (mirroredPart != null) {
            val scope = GlobalSearchScopes.directoriesScope(project, true, thisAtt.parent)
            val targetFile = "$mirroredPart$index"
            FilenameIndex.getAllFilesByExt(project, "att", scope).firstOrNull {
                it.nameWithoutExtension.lowercase() == targetFile
            }
        } else {
            null
        }

        if (otherFile != null) {
            radioGroup.add(copyThisOverOther)
            radioGroup.add(copyOtherOverThis)
        }
        showAndGet()
    }

    override fun showAndGet(): Boolean {
        if (!super.showAndGet())
            return false
        when {
            copyLeftValuesOverRight.isSelected -> doCopyLeftValuesOverRight()
            copyRightValuesOverLeft.isSelected -> doCopyRightValuesOverLeft()
            copyThisOverOther.isSelected -> doCopyThisOverOther()
            copyOtherOverThis.isSelected -> doCopyOtherOverThis()
        }
        return true
    }

    private fun doCopyLeftValuesOverRight() {
        TODO()
    }

    private fun doCopyRightValuesOverLeft() {
        TODO()
    }

//    private fun doCopyOver(copyFrom: VirtualFile, copyTo:VirtualFile) {
//        TODO()
//    }

    private fun doCopyThisOverOther() {
        other?.let { otherAtt ->
            AttCopyMirroredUtil.copyMirrored(project, thisAtt, otherAtt)
        }
    }

    private fun doCopyOtherOverThis() {
        other?.let { otherAtt ->
            AttCopyMirroredUtil.copyMirrored(project, otherAtt, thisAtt)
        }
    }

    companion object {
        private val leftSided = listOf(
            'c', 'd', 'e', 'i', 'j', 'o'
        )
        private val rightSided = listOf(
            'f', 'g', 'h', 'i', 'k', 'l', 'p'
        )

        private val centerSided = listOf(
            'a',
            'b',
            'm',
            'n',
            'q'
        )

        fun opposite(part: Char): Char? {
            return when (part) {
                'a' -> null // Head
                'b' -> null // Body
                'c' -> 'f' // Left thigh
                'd' -> 'g' // left shin
                'e' -> 'h' // Left foot
                'f' -> 'c' // Right thigh
                'g' -> 'd' // Right shin
                'h' -> 'e' // Right foot
                'i' -> 'k' // Left upper-arm
                'j' -> 'l' // left lower-arm/hand
                'k' -> 'i' // right upper-arm
                'l' -> 'j' // left lower-arm/hand
                'm' -> null // Tail base
                'n' -> null // tail tip
                'o' -> 'p' // Left ear
                'p' -> 'o' // Right ear
                'q' -> null // Hair
                else -> null // ????
            }
        }
    }
}