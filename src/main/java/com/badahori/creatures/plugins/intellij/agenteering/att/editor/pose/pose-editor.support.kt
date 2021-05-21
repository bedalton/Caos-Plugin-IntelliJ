package com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose

import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseEditorSupport.getPartName
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

object PoseEditorSupport {

    @JvmStatic
    val allParts = "abcdefghijklmnopq".toCharArray()

    @JvmStatic
    val directions = arrayOf(
        "Up",
        "Straight",
        "Down",
        "Far Down"
    )

    private val moodsC1 = arrayOf(
        "Neutral",
        "Happy",
        "Sad",
        "Angry"
    )

    private val moodsCV = arrayOf(
        "Neutral",
        "Happy",
        "Sad",
        "Angry",
        "Surprised",
        "Sick",
        "Sick (Mouth Open)",
        "Elated",
        "Angry 2",
        "Concerned",
        "Sick",
        "Tongue Out",
        "Neutral 2",
        "Happy 3",
        "Cry 1",
        "Cry 2",
        "Neutral 3",
        "Neutral 4"
    )

    private val moods = arrayOf(
        "Neutral",
        "Happy",
        "Sad",
        "Angry",
        "Surprised",
        "Sick"
    )

    @JvmStatic
    fun getMoodOptions(variant:CaosVariant) : Array<String> {
        return when (variant) {
            CaosVariant.C1 -> moodsC1
            CaosVariant.CV -> moodsCV
            else -> moods
        }
    }

    @JvmStatic
    fun getPartName(part: Char) : String? {
        return when (part) {
            'a' -> "Head"
            'b' -> "Body"
            'c' -> "L. Thigh"
            'd' -> "L. Shin"
            'e' -> "L. Foot"
            'f' -> "R. Thigh"
            'g' -> "R. Shin"
            'h' -> "R. Foot"
            'i' -> "L. Upper Arm"
            'j' -> "L. Lower Arm"
            'k' -> "R. Upper Arm"
            'l' -> "R. Lower Arm"
            'm' -> "Tail Base"
            'n' -> "Tail Tip"
            'o' -> "Left Ear"
            'p' -> "Right Ear"
            'q' -> "Hair"
            else -> null
        }
    }
}

interface BreedPoseHolder {
    fun getPartPose(partChar:Char) : Int?
    fun getPartPose(part: Char, facingDirection: Int, offset:Int) : Int?
    fun getHeadPoseActual() : Int
    fun getPartBreed(partChar:Char) : VirtualFile?
    fun getBodyPoseActual(): Int
    val baseBreed:BreedPartKey
    val variant:CaosVariant
}

/**
 * Renders the virtual file for the breed in the drop down list
 */
private class BreedFileCellRenderer : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>?,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        val file = value as? VirtualFile
        if (file == null) {
            text = "..."
            return this
        }
        text = file.nameWithoutExtension.substring(1)
        return this
    }
}

/**
 * Renders the virtual file for the breed in the drop down list
 */
private class PartFileCellRenderer : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>?,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        val file = value as? VirtualFile
        if (file == null) {
            text = "..related part"
            foreground = TRANSLUCENT
            return this
        }
        foreground = BLACK
        val text = file.nameWithoutExtension
        val part = getPartName(text[0])
        if (part != null) {
            setText("$text - $part")
        } else {
            setText(text)
        }
        return this
    }

    companion object {
        private val TRANSLUCENT: Color = JBColor(Color(0, 0, 0, 140), Color(0, 0, 0, 140))
        private val BLACK: Color = JBColor.BLACK
    }
}