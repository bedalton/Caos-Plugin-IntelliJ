package com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.intellij.openapi.progress.ProgressIndicator
import java.util.*
import java.util.logging.Logger
import kotlin.math.floor

object PoseCalculator {

    @JvmStatic
    val nullableParts = "mnopq".toCharArray()
    private const val ERROR_HEAD_POSE_C1E = 8
    private const val ERROR_HEAD_POSE_C2E = 10

    @JvmStatic
    fun getEars(headPose: Int): Int {
        val index = headPose % 16
        if (headPose >= 160) {
            return index + 16
        }
        if (headPose >= 128) {
            return index + 48
        }
        if (headPose >= 96) {
            return index + 32
        }
        if (headPose >= 64) {
            return index + 16
        }
        return if (headPose >= 32) {
            index + 32
        } else index
    }

    /**
     * Gets the pose for a given combobox
     *
     * @param box             combo box to check
     * @param facingDirection facing direction of creature
     * @param offset          offset into sprite set
     * @param invert          whether or not to invert the pose from 1-4
     * @return pose index in sprite file
     */
    @JvmStatic
    fun getBodyPartPose(
        variant: CaosVariant,
        selectedIndex: Int,
        facingDirection: Int,
        offset: Int,
        invert: Boolean,
    ): Int {
        var pose = selectedIndex
        if (pose < 0) {
            return 0
        }
        if (variant.isOld) {
            if (invert && pose < 4) {
                pose = 3 - pose
            }
            if (facingDirection == 2 || pose == 4) {
                pose = 8
            } else if (facingDirection == 3 || pose == 5) {
                pose = 9
            } else {
                pose += offset
            }
            if (pose > 9 || pose < 0) {
                pose = when (facingDirection) {
                    0 -> 2
                    1 -> 6
                    else -> 8
                }
            }
        } else {
            if (invert) {
                pose = 3 - pose
            }
            pose += offset
        }
        return pose
    }

    @JvmStatic
    fun getHeadComboBoxOptions(variant: CaosVariant, direction: Int, oldDirection: Int): Pair<Array<String>, Int>? {
        return if (variant.isOld) {
            if (direction >= 2 && oldDirection != direction) {
                return Pair(
                    arrayOf(
                        "L. Far Up",
                        "L. Up",
                        "L. Straight",
                        "L. Down",
                        "R. Up",
                        "R. Far Up",
                        "R. Straight",
                        "R. Down",
                        if (direction == 2) "Forward" else "Backward"
                    ), 8
                )
            } else if (oldDirection >= 2 && direction < 2) {
                Pair(
                    arrayOf(
                        "Far Up",
                        "Up",
                        "Straight",
                        "Down",
                        "Forward",
                        "Backward"
                    ), 2
                )
            } else {
                null
            }
        } else {
            if (direction == 2 && oldDirection != 2) {
                Pair(
                    arrayOf(
                        "Left",
                        "Right",
                        "Forward"
                    ), 2
                )
            } else if (direction == 3 && oldDirection != 3) {
                Pair(
                    arrayOf(
                        "Left",
                        "Right",
                        "Backward"
                    ), 2
                )
            } else if (direction < 2 && oldDirection != direction) {
                Pair(
                    arrayOf(
                        if (direction == 0) "Left" else "Right",
                        "Forward",
                        "Backward"
                    ), 0
                )
            } else {
                null
            }
        }
    }

    @JvmStatic
    fun getTranslatedPose(variant: CaosVariant, facing: Int, part: Char, pose: Int, offset: Int): Int? {
        return if (variant.isOld) {
            when (facing) {
                2 -> 4
                3 -> 5
                else -> {
                    val offsetPose: Int = pose - offset
                    if (offsetPose > 3) {
                        LOGGER.severe("Part '$part' has invalid pose offset '$offsetPose' expected 0..3")
                        return null
                    }
                    3 - offsetPose
                }
            }
        } else {
            val offsetPose: Int = pose % 4
            if (offsetPose < 0) {
                LOGGER.severe("Part '$part' has invalid pose offset '$offsetPose' expected 0..3; Pose: $pose; Offset: $offset; Facing: $facing")
                return null
            }
            3 - offsetPose
        }
    }

    /**
     * Calculates the combo box pose, tilt and mood.
     */
    @JvmStatic
    fun getHeadPose(variant: CaosVariant, facing: Int, pose: Int): HeadPoseData? {
        return if (variant.isOld) {
            getHeadPoseOldVariant(variant, facing, pose)
        } else {
            getHeadPoseNewVariant(facing, pose)
        }
    }

    @JvmStatic
    fun calculateHeadPose(
        variant: CaosVariant,
        facingDirection: Int,
        headPose: Int,
        tilt: Int,
        mood: Int,
        eyesClosed: Boolean,
    ): Int {
        // Head is funny, and needs special handling

        return if (variant.isOld)
            calculateHeadPoseOldVariant(
                variant,
                facingDirection,
                headPose,
                tilt,
                mood,
                eyesClosed
            )
        else {
            calculateHeadPoseNewVariant(
                facingDirection,
                headPose,
                tilt,
                mood,
                eyesClosed
            )
        }.apply {
            if (this < 0) {
                LOGGER.severe("Failed to calculate head pose: Variant:$variant; FacingDirection:$facingDirection; HeadPose: $headPose; Tilt: $tilt; Mood: $mood; EyesClosed: $eyesClosed")

            }
        }
    }

    private fun calculateHeadPoseOldVariant(
        variant: CaosVariant,
        facingDirection: Int,
        headPose: Int,
        tilt: Int,
        mood: Int,
        eyesClosed: Boolean,
    ): Int {
        if (headPose !in 0..9) {
            LOGGER.severe("Invalid head pose $headPose found. SelectedIndex: $headPose; Facing: $facingDirection; tilt: $tilt; mood: $mood; eyesClosed: $eyesClosed")
            Exception().printStackTrace()
            return ERROR_HEAD_POSE_C1E
        }
        val calculated = when {
            facingDirection >= 2 -> {
                if (headPose < 0) {
                    LOGGER.severe("No head pose selected")
                    return ERROR_HEAD_POSE_C1E
                } else if (headPose > 9) {
                    LOGGER.severe("Invalid head pose encountered for pose '$headPose' expected 0..10")
                    return ERROR_HEAD_POSE_C1E
                }
                when {
                    headPose < 4 -> 3 - headPose
                    headPose < 8 -> 4 + (3 - (headPose - 4))
                    facingDirection == 3 -> 9
                    else -> headPose
                }
            }
            headPose == 4 -> 8
            headPose == 5 -> 9
            headPose > 5 -> headPose
            facingDirection == 0 -> 3 - headPose
            facingDirection == 1 -> 4 + (3 - headPose)
            else -> {
                LOGGER.severe("Invalid head pose $headPose found. SelectedIndex: $headPose; Facing: $facingDirection; tilt: $tilt; mood: $mood; eyesClosed: $eyesClosed")
                Exception().printStackTrace()
                return ERROR_HEAD_POSE_C1E
            }
        }
        return if (variant == CaosVariant.C1) {
            val eyesMod = if (eyesClosed) {
                8
            } else 0
            val moodMod = if (calculated == 8 && mood > 0) mood + 1 else 0
            calculated + eyesMod + moodMod
        } else {
            calculated + (mood * 20) + (if (eyesClosed) 10 else 0)
        }
    }

    private fun calculateHeadPoseNewVariant(
        facingDirection: Int,
        headPose: Int,
        tilt: Int,
        mood: Int,
        eyesClosed: Boolean,
    ): Int {
        var calculated = when {
            headPose >= 0 -> {
                // If first option (Can be left or right)
                val headDirection = if (headPose == 0) {
                    // If facing is right, value is right (1)
                    if (facingDirection == 1)
                        1
                    else // Else: Value is truly left (0)
                        headPose
                } else if (headPose == 1) {
                    // Second slot can be right or forward
                    // If facing left or right
                    if (facingDirection < 2)
                    //  slot is Forward
                        2
                    else // else slot is right
                        headPose
                } else if (headPose == 2) {
                    // Head pose 2 can only be forward if facing backward
                    facingDirection
                } else {
                    LOGGER.severe("Cannot find head pose: SelectedIndex: $headPose; Facing: $facingDirection; tilt: $tilt; mood: $mood; eyesClosed: $eyesClosed")
                    facingDirection
                }
                val offsetBase = headDirection * 4
                val tiltOffset = 3 - tilt
                tiltOffset + offsetBase
            }
            else -> {
                LOGGER.severe("Head pose is set to a number less than 0. Perhaps no head pose index is selected")
                return ERROR_HEAD_POSE_C2E
            }
        }
        calculated += mood * 32 + if (eyesClosed) 16 else 0
        return calculated
    }

    /**
     * Data object to hold head pose combo box information
     */
    data class HeadPoseData(
        val direction: Int,
        val tilt: Int? = null,
        val mood: Int? = null,
    )


    /**
     * Finds associated parts given a char for use in focus modes
     *
     * @param part part to find related for
     * @return related parts
     */
    @JvmStatic
    fun getAssociatedParts(part: Char): List<Char> {
        return when (part) {
            'a' -> listOf('o', 'p', 'q')
            'b' -> listOf('a', 'c', 'f', 'i', 'k', 'm')
            'c' -> listOf('d', 'e')
            'd' -> listOf('c', 'e')
            'e' -> listOf('c', 'd')
            'f' -> listOf('g', 'h')
            'g' -> listOf('f', 'h')
            'h' -> listOf('f', 'g')
            'i' -> return listOf('j')
            'j' -> return listOf('i')
            'k' -> return listOf('l')
            'l' -> return listOf('k')
            'm' -> return listOf('n')
            'n' -> return listOf('m')
            'o', 'p', 'q' -> return listOf('a')
            else -> emptyList()
        }
    }


    /**
     * Updates the pose object with whatever parts may have changed according to the char parts given
     *
     * @param parts parts that need recalculating
     * @return the updated pose, though the instance pose object is also updated
     */
    @JvmStatic
    fun getUpdatedPose(
        progressIndicator: ProgressIndicator?,
        variant: CaosVariant,
        pose: Pose?,
        facingDirection: Int,
        poseHolder: BreedPoseHolder,
        vararg parts: Char,
    ): Pose {

        // Get facing direction to calculate the sprite offset in the sprite file
        val offset: Int = when (facingDirection) {
            0 -> 0
            1 -> 4
            2 -> 8
            3 -> if (variant.isOld) 9 else 12
            else -> {
                LOGGER.severe("Invalid direction offset")
                0
            }
        }
        // Gets the pose object for editing
        val poseTemp: Pose = pose?.copy() ?: (if (offset < 8) (offset + 3) else offset).let { def ->
            // If the pose object is not yet initialized, initialize it
            Pose(offset + 2, def, def, def, def, def, def, def, def, def, def, def, def, def, def)
        }

        // Go through each part passed in for updating, and update it.
        for (part in parts) {
            progressIndicator?.checkCanceled()
            if (part !in PoseEditorSupport.allParts)
                continue
            when (part) {
                'a' -> poseTemp.head = poseHolder.getHeadPoseActual().apply {
                    if (this < 0)
                        throw Exception("Invalid head pose calculated. FacingDirection:$facingDirection; Output: $this")
                }
                'b' -> {
                    poseTemp.body = poseHolder.getBodyPoseActual()
                }
                'o', 'p' -> {
                    poseTemp.ears = getEars(poseHolder.getHeadPoseActual()).apply {
                        if (this < 0)
                            throw Exception("Invalid ears pose. FacingDirection:$facingDirection; Output: $this")
                    }
                }
                else -> {
                    poseTemp[part] = poseHolder.getPartPose(part, facingDirection, offset)?.apply {
                        if (this < 0)
                            throw Exception("Invalid pose calculated for part '$part'. FacingDirection:$facingDirection; Offset: $offset; Output: $this")
                    }
                }
            }
        }
        return poseTemp
    }


}


/**
 * Calculates the head pose combo box information for Old variants
 */
private fun getHeadPoseOldVariant(variant: CaosVariant, facing: Int, pose: Int): PoseCalculator.HeadPoseData {

    // If head is facing forward or backwards,
    // Leave pose the way it is
    val headPose: Int = when {
        facing < 2 -> {
            when {
                pose < 8 -> 3 - (pose % 4)
                pose == 8 -> 4
                pose == 9 -> 5
                else -> {
                    LOGGER.severe("Invalid head pose '$pose' with facing: $facing")
                    (Exception()).printStackTrace()
                    0
                }
            }
        }
        facing == 3 -> 8
        else -> pose
    }

    // Calculate the mood of the head when facing forwards
    val moodIndex: Int =
        if (variant === CaosVariant.C1) {
            if (pose < 10) {
                0
            } else {
                pose - 9
            }
        } else {
            floor(pose / 20.0).toInt()
        }
    return PoseCalculator.HeadPoseData(headPose, tilt = null, mood = moodIndex)
}

/**
 * Gets the head pose combo box information for C2e variants
 */
private fun getHeadPoseNewVariant(facing: Int, pose: Int): PoseCalculator.HeadPoseData {
    val offset = pose % 16
    val headPose: Int = when {
        offset < 4 -> 0
        offset < 8 -> if (facing < 2) 0 else 1
        offset < 12 -> if (facing < 2) 1 else 2 // If face is facing forward
        else -> 2
    }
    val tilt: Int = 3 - (pose % 4)
    val mood: Int? = if (pose > 32) floor(pose / 32.0).toInt() else null
    return PoseCalculator.HeadPoseData(headPose, tilt = tilt, mood = mood)
}

private val LOGGER = Logger.getLogger("#PoseCalculator")
