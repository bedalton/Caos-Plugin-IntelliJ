package com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose

import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseRenderer.PartVisibility.*
import com.badahori.creatures.plugins.intellij.agenteering.att.parser.AttFileData
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.indices.SpriteBodyPart
import com.badahori.creatures.plugins.intellij.agenteering.utils.lowercase
import com.bedalton.common.util.toListOf
import java.awt.AlphaComposite
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.abs


object PoseRenderer {

    private const val ghostAlpha: Float = 0.4F

    @JvmStatic
    fun render(
        variant: CaosVariant,
        sprites: CreatureSpriteSet,
        pose: Pose,
        visibilityMask: Map<Char, PartVisibility>,
        zoom: Int = 1,
    ): BufferedImage {
        val (parts, size) = buildParts(variant, sprites, pose, visibilityMask)
            ?: throw Exception("Failed to build body parts for pose render")
        val (width, height) = size
        val zoomWidth = width * zoom
        val zoomHeight = height * zoom

        @Suppress("UndesirableClassUsage")
        val image = BufferedImage(zoomWidth, zoomHeight, BufferedImage.TYPE_INT_ARGB)

        val graphics2d = image.graphics as Graphics2D
        graphics2d.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        )

        for (part in parts) {
            // Handle visibility masking
            if (part.visibility == HIDDEN) {
                continue
            } else if (part.visibility == GHOST) {
                val ac: AlphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ghostAlpha)
                graphics2d.composite = ac
            } else {
                val ac: AlphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER)
                graphics2d.composite = ac
                graphics2d.composite
            }

            graphics2d.drawImage(
                part.bufferedImage,
                part.position.first * zoom,
                part.position.second * zoom,
                part.bufferedImage.width * zoom,
                part.bufferedImage.height * zoom,
                null
            )
        }

        val raster = image.raster
        var rgba = IntArray(4)
        val rasterAlpha = (ghostAlpha * 255).toInt()
        repeat(zoomHeight) { y ->
            repeat(zoomWidth) x@{ x ->
                rgba = raster.getPixel(x, y, rgba)
                val sum = rgba[3]
                if (sum == 0 || sum == 255)
                    return@x
                rgba[3] = rasterAlpha
                raster.setPixel(x, y, rgba)
            }
        }
        return image
    }


    private fun buildParts(
        variant: CaosVariant,
        sprites: CreatureSpriteSet,
        pose: Pose,
        visibilityMask: Map<Char, PartVisibility>,
    ): Pair<List<RenderPart>, Pair<Int, Int>>? {

        // Body
        if (pose.body !in 0..sprites.body.bodyData.lines.lastIndex) {
            throw Exception("Body pose '${pose.body}' out of bounds. Variant: ${variant.code};")
        }
        val bodyAtt = sprites.body.bodyData[pose.body]
        val bodySprite = sprites.body.sprite[pose.body]!!
        val bodyPart = RenderPart(bodySprite, Pair(0, 0), visibilityMask['b'] ?: VISIBLE)

        // Head
        val head = sprites.head

        val headPose = pose.head.let {
            when {
                variant == CaosVariant.C2 -> it % 10
                variant.isNotOld -> it % 16
                it > 9 -> 8
                else -> it
            }
        }
        if (headPose !in 0..head.bodyData.lines.lastIndex) {
            throw Exception("Head pose '${pose.head}->($headPose)' out of bounds.")
        }
        val headPart = RenderPart(
            head.sprite[pose.head]!!,
            bodyAtt[0].let { (bodyHeadX, bodyHeadY) ->
                val (headX, headY) = head.bodyData[headPose][0]
                Pair(bodyHeadX - headX, bodyHeadY - headY)
            },
            visibilityMask['a'] ?: VISIBLE
        )

        // Left Ear
        val leftEarPart = sprites.leftEar?.let { part ->
            val headAtt = head.bodyData[headPose].getOrNull(2)?.let { headPart.position + it }
                ?: return@let null
            getPart('o', part, headPose, pose.ears, headAtt, visibilityMask['o'] ?: VISIBLE)
        }

        // Right Ear
        val rightEarPart = sprites.rightEar?.let { part ->
            val headAtt = head.bodyData[headPose].getOrNull(3)?.let { headPart.position + it }
                ?: return@let null
            getPart('p', part, headPose, pose.ears, headAtt, visibilityMask['p'] ?: VISIBLE)
        }

        // Hair
        val hairPart = sprites.hair?.let { part ->
            val headAtt = head.bodyData[headPose].getOrNull(4)?.let { headPart.position + it }
                ?: return@let null
            getPart('q', part, headPose, headAtt, visibilityMask['q'] ?: VISIBLE)
        }

        // Left Thigh
        val leftThigh = sprites.leftThigh.let { part ->
            val attachAt = bodyAtt.getOrNull(1)
                ?: return@let null
            getPart('c', part, pose.leftThigh, attachAt, visibilityMask['c'] ?: VISIBLE)
        }

        // Left Shin
        val leftShin = sprites.leftShin.let { part ->
            val attachAt = leftThigh!!.position + sprites.leftThigh.bodyData[pose.leftThigh][1]
            getPart('d', part, pose.leftShin, attachAt, visibilityMask['d'] ?: VISIBLE)
        }

        // Left Foot
        val leftFoot = sprites.leftFoot.let { part ->
            val attachAt = leftShin!!.position + sprites.leftShin.bodyData[pose.leftShin][1]
            getPart('e', part, pose.leftFoot, attachAt, visibilityMask['e'] ?: VISIBLE)
        }

        // Right Thigh
        val rightThigh = sprites.rightThigh.let { part ->
            val attachAt = bodyAtt.getOrNull(2)
                ?: return@let null
            getPart('f', part, pose.rightThigh, attachAt, visibilityMask['f'] ?: VISIBLE)
        }

        // Right Shin
        val rightShin = sprites.rightShin.let { part ->
            val attachAt = rightThigh!!.position + sprites.rightThigh.bodyData[pose.rightThigh][1]
            getPart('g', part, pose.rightShin, attachAt, visibilityMask['g'] ?: VISIBLE)
        }

        // Right Foot
        val rightFoot = sprites.rightFoot.let { part ->
            val attachAt = rightShin!!.position + sprites.rightShin.bodyData[pose.rightShin][1]
            getPart('h', part, pose.rightFoot, attachAt, visibilityMask['h'] ?: VISIBLE)
        }

        // Left Upper Arm
        val leftUpperArm = sprites.leftUpperArm.let { part ->
            val attachAt = bodyAtt.getOrNull(3)
                ?: return@let null
            getPart('i', part, pose.leftUpperArm, attachAt, visibilityMask['i'] ?: VISIBLE)
        }

        // Left Forearm
        val leftForearm = sprites.leftForearm.let { part ->
            val attachAt = leftUpperArm!!.position + sprites.leftUpperArm.bodyData[pose.leftUpperArm][1]
            getPart('j', part, pose.leftForearm, attachAt, visibilityMask['j'] ?: VISIBLE)
        }

        // Right Upper Arm
        val rightUpperArm = sprites.rightUpperArm.let { part ->
            val attachAt = bodyAtt.getOrNull(4)
                ?: return@let null
            getPart('k', part, pose.rightUpperArm, attachAt, visibilityMask['k'] ?: VISIBLE)
        }

        // Right Forearm
        val rightForearm = sprites.rightForearm.let { part ->
            val attachAt = rightUpperArm!!.position + sprites.rightUpperArm.bodyData[pose.rightUpperArm][1]
            getPart('l', part, pose.rightForearm, attachAt, visibilityMask['l'] ?: VISIBLE)
        }

        val tail = if (variant == CaosVariant.C1) {
            val tailBase = sprites.tailTip?.let { part ->
                val attachAt = bodyAtt.getOrNull(5)
                    ?: return@let null
                getPart('n', part, pose.tailTip, attachAt, visibilityMask['m'] ?: VISIBLE)
            }

            val tailTip = sprites.tailBase?.let { part ->
                val attachAt = sprites
                    .tailTip
                    ?.bodyData
                    ?.getOrNull(pose.tailTip)
                    ?.getOrNull(1)
                    ?.let { tailBase!!.position + it }
                    ?: return@let null
                getPart('m', part, pose.tailBase, attachAt, visibilityMask['n'] ?: VISIBLE)
            }
            // Tail layer
            listOfNotNull(
                tailBase,
                tailTip
            )
        } else {
            val tailBase = sprites.tailBase?.let { part ->
                val attachAt = bodyAtt.getOrNull(5)
                    ?: return@let null
                getPart('m', part, pose.tailBase, attachAt, visibilityMask['m'] ?: VISIBLE)
            }

            val tailTip = sprites.tailTip?.let { part ->
                val attachAt = sprites.tailBase
                    ?.bodyData
                    ?.getOrNull(pose.tailBase)
                    ?.getOrNull(1)
                    ?.let { tailBase!!.position + it }
                    ?: return@let null
                getPart('n', part, pose.tailTip, attachAt, visibilityMask['n'] ?: VISIBLE)
            }
            // Tail layer
            listOfNotNull(
                tailBase,
                tailTip
            )
        }

        // Head layer
        val headParts: List<RenderPart> = when (pose.head) {
            in 0..3 -> listOfNotNull(
                leftEarPart,
                headPart,
                hairPart,
                rightEarPart
            )
            in 4..7 -> listOfNotNull(
                rightEarPart,
                headPart,
                hairPart,
                rightEarPart
            )
            else -> listOfNotNull(
                headPart,
                hairPart,
                leftEarPart,
                rightEarPart
            )
        }

        // Left Leg Layer
        val leftLeg = listOf(
            leftThigh,
            leftShin,
            leftFoot
        )

        // Right leg layer
        val rightLeg = listOf(
            rightThigh,
            rightShin,
            rightFoot
        )

        // Left Arm layer
        val leftArm = listOf(
            leftUpperArm,
            leftForearm
        )

        // Right arm layer
        val rightArm = listOf(
            rightUpperArm,
            rightForearm
        )

        // Parts in order
        val parts = if (sprites.body.sprite.size == 10) {
            // OLD VARIANTS
            when (pose.body) {
                in 0..3 -> leftArm + leftLeg + tail.getOrNull(0) + bodyPart + tail.getOrNull(1) + headParts + rightLeg + rightArm
                in 4..7 -> rightArm + rightLeg + tail.getOrNull(0) + bodyPart + tail.getOrNull(1)+ headParts + leftLeg + leftArm
                8 -> if (variant == CaosVariant.C1) {
                    tail + bodyPart + leftLeg + rightLeg + leftArm + rightArm + headParts
                } else {
                    tail + leftLeg + rightLeg + bodyPart + leftArm + rightArm + headParts
                }
                9 -> if (variant == CaosVariant.C1) {
                    bodyPart.toListOf() + leftLeg.reversed() + rightLeg.reversed() + leftArm.reversed() + rightArm.reversed() + tail + headParts
                } else {
                    leftLeg.reversed() + rightLeg.reversed() + leftArm.reversed() + rightArm.reversed() + bodyPart + tail + headParts
                }
                else -> null
            }
        } else {
            // NEW VARIANTS
            when (pose.body) {
                in 0..3 -> leftArm + leftLeg + tail.getOrNull(0) + bodyPart + tail.getOrNull(1) + headParts + rightLeg + rightArm
                in 4..7 -> rightArm + rightLeg + tail.getOrNull(0) + bodyPart + tail.getOrNull(1) + headParts + leftLeg + leftArm
                in 8..11 -> tail + leftLeg.reversed() + rightLeg.reversed() + leftUpperArm + rightUpperArm + bodyPart + leftForearm + rightForearm + headParts
                in 12..15 -> leftArm.reversed() + rightArm.reversed() + leftLeg.reversed() + rightLeg.reversed() + bodyPart + headParts + tail
                else -> null
            }
        }?.filterNotNull()
            ?: return null

        val minX = parts.minOfOrNull { it.position.first } ?: 0
        val maxX = parts.maxOfOrNull { it.position.first + it.bufferedImage.width } ?: 0
        val minY = parts.minOfOrNull { it.position.second } ?: 0
        val maxY = parts.maxOfOrNull { it.position.second + it.bufferedImage.height } ?: 0

        val size = Pair(abs(minX) + maxX, abs(minY) + maxY)
        val offset = Pair(-minX, -minY)
        val partsAtOrigin = parts.map { part ->
            part.copy(
                position = part.position + offset,
            )
        }
        return Pair(partsAtOrigin, size)
    }

    private fun getPart(
        partChar: Char,
        part: SpriteBodyPart,
        pose: Int,
        attachAt: Pair<Int, Int>,
        visibility: PartVisibility,
    ): RenderPart? {
        return getPart(partChar, part, pose, pose, attachAt, visibility)
    }

    private fun getPart(
        partChar: Char,
        part: SpriteBodyPart,
        pose: Int,
        spritePose: Int,
        attachAt: Pair<Int, Int>,
        visibility: PartVisibility,
    ): RenderPart? {
        if (pose !in 0..part.bodyData.lines.lastIndex) {
            throw Exception("Pose: '$pose' is out of range for part: '$partChar'")
        }
        val thisAtt = part.bodyData[pose][0]
        val sprite = part.sprite[spritePose]
            ?: return null
        return RenderPart(sprite, attachAt - thisAtt, visibility)
    }

    private data class RenderPart(
        val bufferedImage: BufferedImage,
        val position: Pair<Int, Int>,
        val visibility: PartVisibility,
    )


    data class CreatureSpriteSet(
        var head: SpriteBodyPart,
        var body: SpriteBodyPart,
        var leftThigh: SpriteBodyPart,
        var leftShin: SpriteBodyPart,
        var leftFoot: SpriteBodyPart,
        var rightThigh: SpriteBodyPart,
        var rightShin: SpriteBodyPart,
        var rightFoot: SpriteBodyPart,
        var leftUpperArm: SpriteBodyPart,
        var leftForearm: SpriteBodyPart,
        var rightUpperArm: SpriteBodyPart,
        var rightForearm: SpriteBodyPart,
        var tailBase: SpriteBodyPart?,
        var tailTip: SpriteBodyPart?,
        var leftEar: SpriteBodyPart?,
        var rightEar: SpriteBodyPart?,
        var hair: SpriteBodyPart?,
    ) {
        fun replacing(part: Char, att: AttFileData): CreatureSpriteSet {
            return when (part) {
                'a' -> copy(
                    head = head.copy(
                        sprite = head.sprite, bodyData = att
                    )
                )
                'b' -> copy(
                    body = body.copy(
                        sprite = body.sprite, bodyData = att
                    )
                )
                'c' -> copy(
                    leftThigh = leftThigh.copy(
                        bodyData = att
                    )
                )
                'd' -> copy(
                    leftShin = leftShin.copy(
                        bodyData = att
                    )
                )
                'e' -> copy(
                    leftFoot = leftFoot.copy(
                        bodyData = att
                    )
                )
                'f' -> copy(
                    rightThigh = rightThigh.copy(
                        bodyData = att
                    )
                )
                'g' -> copy(
                    rightShin = rightShin.copy(
                        bodyData = att
                    )
                )
                'h' -> copy(
                    rightFoot = rightFoot.copy(
                        bodyData = att
                    )
                )
                'i' -> copy(
                    leftUpperArm = leftUpperArm.copy(
                        bodyData = att
                    )
                )
                'j' -> copy(
                    leftForearm = leftForearm.copy(
                        bodyData = att
                    )
                )
                'k' -> copy(
                    rightUpperArm = rightUpperArm.copy(
                        bodyData = att
                    )
                )
                'l' -> copy(
                    rightForearm = rightForearm.copy(
                        bodyData = att
                    )
                )

                'm' -> copy(
                    tailBase = tailBase?.copy(
                        bodyData = att
                    )
                )
                'n' -> copy(
                    tailTip = tailTip?.copy(
                        bodyData = att
                    )
                )

                'o' -> copy(
                    leftEar = leftEar?.copy(
                        bodyData = att
                    )

                )

                'p' -> copy(
                    rightEar = rightEar?.copy(
                        bodyData = att
                    )
                )

                'q' -> copy(
                    hair = hair?.copy(
                        bodyData = att
                    )
                )
                else -> throw Exception("Invalid part '$part' for att replacing")
            }
        }

        fun asMap(): Map<Char, SpriteBodyPart?> {
            return ('a'..'q').associateWith { part ->
                get(part)
            }
        }

        operator fun get(part: Char): SpriteBodyPart? {
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
                'o' -> leftEar
                'p' -> rightEar
                'q' -> hair
                else -> throw Exception("Invalid part '$part' for att replacing")
            }
        }
    }

    enum class PartVisibility {
        VISIBLE,
        GHOST,
        HIDDEN;

        companion object {
            @Suppress("SpellCheckingInspection")
            private val ALL_PARTS = "abcdefghijklmnopq".toCharArray()

            @JvmStatic
            val allVisible: MutableMap<Char, PartVisibility>
                get() = HashMap(ALL_PARTS.associateWith { VISIBLE })

            @JvmStatic
            val allGhost: MutableMap<Char, PartVisibility>
                get() = HashMap(ALL_PARTS.associateWith { GHOST })

            @JvmStatic
            val allHidden: MutableMap<Char, PartVisibility>
                get() = HashMap(ALL_PARTS.associateWith { HIDDEN })
        }
    }
}

private operator fun Pair<Int, Int>.minus(other: Pair<Int, Int>): Pair<Int, Int> {
    return Pair(first - other.first, second - other.second)
}


private operator fun Pair<Int, Int>.plus(other: Pair<Int, Int>): Pair<Int, Int> {
    return Pair(first + other.first, second + other.second)
}
