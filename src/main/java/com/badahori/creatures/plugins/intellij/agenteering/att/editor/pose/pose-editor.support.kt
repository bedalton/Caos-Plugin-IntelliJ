package com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose

import bedalton.creatures.util.pathSeparator
import bedalton.creatures.util.pathSeparatorChar
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseEditorSupport.getPartName
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.indices.BodyPartFiles
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Component
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

object PoseEditorSupport {

    @Suppress("SpellCheckingInspection")
    @JvmStatic
    val allParts = "abcdefghijklmnopq".toCharArray()

    @Suppress("unused")
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
    fun getMoodOptions(variant: CaosVariant): Array<String> {
        return when (variant) {
            CaosVariant.C1 -> moodsC1
            CaosVariant.CV -> moodsCV
            else -> moods
        }
    }

    @JvmStatic
    fun getPartName(part: Char): String? {
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
    val zoom: Int
    fun setFiles(files: List<BodyPartFiles>)
    fun setRendered(image: BufferedImage?)
    fun getPartPose(partChar: Char): Int?
    fun updatePoseAndGet(progressIndicator: ProgressIndicator?, vararg parts: Char): Pose
    fun getVisibilityMask(): Map<Char, PoseRenderer.PartVisibility>?
    fun getPartPose(part: Char, facingDirection: Int, offset: Int): Int?
    fun getHeadPoseActual(): Int
    fun getBreedFiles(part: PartGroups): List<BodyPartFiles>
    fun getBodyPoseActual(): Int
    val baseBreed: BreedPartKey
    val variant: CaosVariant
}

/**
 * Renders the virtual file for the breed in the dropdown list
 */
private class BreedFileCellRenderer : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>?,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): Component {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)


        if (value == null) {
            text = "..."
            return this
        }

        if (value is String) {
            text = value
            return this
        }

        val items = list?.items
            .nullIfEmpty()

        if (items == null) {
            text = "..."
            return this
        }

        val parentPaths = items.map map@{ tripleIn ->
            val triple = (tripleIn as? Triple<*,*,*>)
                ?: return@map null
            Pair((triple.second as BreedPartKey).copyWithPart(null), ((triple.third as? List<*>)?.firstOrNull() as? BodyPartFiles)?.spriteFile?.parent)
        }

        val triple = value as Triple<*, *, *>
        val fallbackIndex = items.indexOf(value)
        val self = parentPaths.getOrNull(maxOf(index, fallbackIndex))
        if (self == null) {
            text = "..."
            return this
        } else {
            isVisible = true
        }

        val breed = triple.second as BreedPartKey

        // Get relative path if any
        // Relative path is needed to disambiguate between conflicting parts with same breed key
        val relativePath = if (self.second != null) {
            parentPaths
                .filterNotNull()
                .filter { other ->
                    BreedPartKey.isGenericMatch(other.first,breed) && other.second != self.second
                }
                .nullIfEmpty()
                ?.mapNotNull { other ->
                    other.second
                }
                ?.let { otherPaths ->
                    " ..." + getRelativePath(otherPaths, self.second!!)
                }
        } else {
            null
        } ?: ""

        text = (breed.copyWithPart(null).code ?: "${breed.genus.orElse("?")}${breed.ageGroup.orElse("?")}${breed.breed.orElse("?")}") + relativePath
        return this
    }


    private fun getRelativePath(list:List<VirtualFile>, parent: VirtualFile): String {
        val myComponents = parent.path.split(pathSeparatorChar).reversed()
        var matches = list.map { it.path.split(pathSeparatorChar).reversed() }
        for (i in myComponents.indices) {
            val component = myComponents[i]
            matches = matches.filter { components: List<String> -> components.getOrNull(i) == component }
            if (matches.isEmpty()) {
                return myComponents.subList(0, i).reversed().joinToString(pathSeparator)
            }
        }
        return parent.path
    }
}

/**
 * Renders the virtual file for the breed in the drop-down list
 */
private class PartFileCellRenderer(val strict: Boolean = false) : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>?,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): Component {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        var disable = false
        val text = when (value) {
            is String -> value
            is VirtualFile -> value.nameWithoutExtension
            is BodyPartFiles -> {
                disable = strict && value.spriteFile.nameWithoutExtension != value.bodyDataFile.nameWithoutExtension
                value.spriteFile.nameWithoutExtension
            }
            null -> {
                text = "..related part"
                foreground = TRANSLUCENT
                return this
            }
            else -> {
                foreground = TRANSLUCENT
                isVisible = false
                return this
            }
        }
        foreground = BLACK
        this.text = text
        this.isEnabled = !disable
        val part = getPartName(text[0].lowercaseChar())
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


internal data class PoseTransferable(
    internal val pose: Pose,
    private val facing: Int,
    private val variant: CaosVariant,
) : Transferable {

    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return flavors
    }

    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean {
        return when (flavor) {
            DataFlavor.stringFlavor -> true
            PoseDataFlavor -> true
            else -> false
        }
    }

    override fun getTransferData(flavor: DataFlavor?): Any {
        return when (flavor) {
            DataFlavor.stringFlavor -> pose.poseString(variant, facing)!!
            PoseDataFlavor -> pose
            else -> throw UnsupportedFlavorException(flavor)
        }
    }

    companion object {
        private val flavors = arrayOf(
            DataFlavor.stringFlavor,
            PoseDataFlavor
        )
    }

    object PoseDataFlavor : DataFlavor("x-application/creatures.pose", "Creatures Pose String")

}


data class Pose(
    var head: Int,
    var body: Int,
    var leftThigh: Int,
    var leftShin: Int,
    var leftFoot: Int,
    var rightThigh: Int,
    var rightShin: Int,
    var rightFoot: Int,
    var leftUpperArm: Int,
    var leftForearm: Int,
    var rightUpperArm: Int,
    var rightForearm: Int,
    var tailBase: Int,
    var tailTip: Int,
    var ears: Int,
) {

    operator fun get(part: Char): Int? {
        return when (part.lowercase()) {
            'a' -> head
            'b' -> body
            'c' -> leftThigh
            'd' -> leftShin
            'e' -> leftFoot
            'f' -> rightThigh
            'g' -> rightShin
            'h' -> rightFoot
            'i' -> leftUpperArm
            'j' -> leftForearm
            'k' -> rightUpperArm
            'l' -> rightForearm
            'm' -> tailBase
            'n' -> tailTip
            'o' -> ears
            'p' -> ears
            'q' -> head
            else -> return null
        }
    }

    operator fun set(part: Char, value: Int?) {
        when (part) {
            'a' -> head = value!!
            'b' -> body = value!!
            'c' -> leftThigh = value!!
            'd' -> leftShin = value!!
            'e' -> leftFoot = value!!
            'f' -> rightThigh = value!!
            'g' -> rightShin = value!!
            'h' -> rightFoot = value!!
            'i' -> leftUpperArm = value!!
            'j' -> leftForearm = value!!
            'k' -> rightUpperArm = value!!
            'l' -> rightForearm = value!!
            'm' -> tailBase = value ?: 0
            'n' -> tailTip = value ?: 0
            'o' -> ears = value ?: 0
            'p' -> ears = value ?: 0
        }
    }


    // Example pose: 241000330022333
    fun poseString(variant: CaosVariant, facing: Int): String? {
        if (facing !in 0..3) {
            return null
        }
        val out = StringBuilder()
        val direction = 3 - facing
        out.append(direction)
        if (variant.isOld) {
            getPoseStringC1e(this, out)
        } else
            getPoseStringC2e(this, out)
        return out.toString()
    }


    companion object {
        private const val DO_NOT_SET_POSE = -1
        private const val INVALID_POSE = -2

        @JvmStatic
        fun fromString(
            variant: CaosVariant,
            currentDirection: Int,
            currentPose: Pose?,
            poseString: String,
        ): Pair<Int, Pose> {
            val directionInt = (poseString[0] - '0').let {
                if (it !in 0..3) currentDirection else it
            }
            val directionOffset = when (directionInt) {
                3 -> 0 // Right
                2 -> 4 // Left
                1 -> if (variant.isOld) 0 else 8 // FRONT
                0 -> if (variant.isOld) 0 else 12 // BACK
                else -> currentDirection
            }
            val pose = Pose(
                head = get(variant, poseString, directionOffset, 'a', currentPose?.head) ?: INVALID_POSE,
                body = get(variant, poseString, directionOffset, 'b', currentPose?.body) ?: INVALID_POSE,
                leftThigh = get(variant, poseString, directionOffset, 'c', currentPose?.leftThigh) ?: INVALID_POSE,
                leftShin = get(variant, poseString, directionOffset, 'd', currentPose?.leftShin) ?: INVALID_POSE,
                leftFoot = get(variant, poseString, directionOffset, 'e', currentPose?.leftFoot) ?: INVALID_POSE,
                rightThigh = get(variant, poseString, directionOffset, 'f', currentPose?.rightThigh) ?: INVALID_POSE,
                rightShin = get(variant, poseString, directionOffset, 'g', currentPose?.rightShin) ?: INVALID_POSE,
                rightFoot = get(variant, poseString, directionOffset, 'h', currentPose?.rightFoot) ?: INVALID_POSE,
                leftUpperArm = get(variant, poseString, directionOffset, 'i', currentPose?.leftUpperArm)
                    ?: INVALID_POSE,
                leftForearm = get(variant, poseString, directionOffset, 'j', currentPose?.leftForearm) ?: INVALID_POSE,
                rightUpperArm = get(variant, poseString, directionOffset, 'k', currentPose?.rightUpperArm)
                    ?: INVALID_POSE,
                rightForearm = get(variant, poseString, directionOffset, 'l', currentPose?.rightForearm)
                    ?: INVALID_POSE,
                tailBase = get(variant, poseString, directionOffset, 'm', currentPose?.tailBase) ?: INVALID_POSE,
                tailTip = get(variant, poseString, directionOffset, 'n', currentPose?.tailTip) ?: INVALID_POSE,
                ears = currentPose?.ears ?: DO_NOT_SET_POSE
            )
            val poseEditorDirection = when (directionInt) {
                3 -> 0 // Right
                2 -> 1 // Left
                1 -> 2 // Front
                0 -> 3 // back
                else -> currentDirection
            }
            return Pair(poseEditorDirection, pose)
        }

        private fun get(
            variant: CaosVariant,
            poseString: String,
            directionOffset: Int,
            part: Char,
            currentPose: Int?,
        ): Int? {
            if (variant.isOld) {
                if (directionOffset == 0) {
                    if (poseString[0] == '0') { // Back == 0
                        return 9
                    } else if (poseString[0] == '1') { // FRONT = 1
                        return 8
                    }
                }
            }
            val i = (part.lowercase() - 'a')
            if (i < 0)
                return -1
            return when (val pose = poseString[i + 1]) {
                in '0'..'3' -> directionOffset + ("$pose".toInt())
                'x', '?', '!' -> currentPose ?: -1
                '4' -> if (part == 'a') {
                    8
                } else {
                    null
                }
                '5' -> if (part == 'a') {
                    if (variant.isOld)
                        9
                    else if (variant.isNotOld)
                        13
                    else
                        9
                } else {
                    null
                }
                else -> {
                    null
                }
            }
        }
    }
}


private fun getPoseStringC1e(pose: Pose, out: StringBuilder): StringBuilder {
    val head = when (pose.head) {
        8 -> 4
        9 -> 5
        else -> pose.head % 4
    }
    out.append(head)
    for (part in 'b'..'n') {
        out.append(pose[part].orElse(0) % 4)
    }
    return out
}

private fun getPoseStringC2e(pose: Pose, out: StringBuilder): StringBuilder {
    val head = when (pose.head) {
        8 -> 4
        9 -> 5
        else -> pose.head % 4
    }
    out.append(head)
    for (part in 'b'..'q') {
        out.append(pose[part].orElse(0) % 4)
    }
    return out
}