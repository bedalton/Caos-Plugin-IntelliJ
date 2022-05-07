package com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose

import com.badahori.creatures.plugins.intellij.agenteering.indices.BodyPartFiles
import com.badahori.creatures.plugins.intellij.agenteering.indices.SpriteBodyPart
import com.badahori.creatures.plugins.intellij.agenteering.utils.lowercase
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import java.util.logging.Logger

object SpriteSetUtil {

    private val LOGGER = Logger.getLogger("#SpriteSetUtil")

    /**
     * Update the sprite set for changed chars and returns it
     *
     * @param parts parts that have changed
     * @return update sprite set
     */
    @JvmStatic
    fun getUpdatedSpriteSet(
        project: Project,
        progressIndicator: ProgressIndicator,
        spriteSet: PoseRenderer.CreatureSpriteSet?,
        head: List<BodyPartFiles>,
        body: List<BodyPartFiles>,
        legs: List<BodyPartFiles>,
        arms: List<BodyPartFiles>,
        tail: List<BodyPartFiles>,
        ears: List<BodyPartFiles>,
        hair: List<BodyPartFiles>,
        manualAtts: Map<Char, BodyPartFiles>,
        locked: Map<Char, BodyPartFiles>,
        vararg parts: Char,
    ): PoseRenderer.CreatureSpriteSet? {
        if (project.isDisposed) {
            return null
        }
        progressIndicator.checkCanceled()
        var realParts = parts
        val spriteTemp: PoseRenderer.CreatureSpriteSet = spriteSet?.copy() ?: defaultSpriteSet(
            project = project,
            headParts = head,
            bodyParts = body,
            legParts = legs,
            armParts = arms,
            tailParts = tail,
            earParts = ears,
            hairParts = hair,
            manualAtts = manualAtts,
            locked = locked
        )
        ?: return null
        if (parts.isEmpty()) {
            realParts = PoseEditorSupport.allParts
        }
        for (part in realParts) {
            progressIndicator.checkCanceled()
            val files = when (part.lowercase()) {
                'a' -> head
                'b' -> body
                'c', 'd', 'e', 'f', 'g', 'h' -> legs
                'i', 'j', 'k', 'l' -> arms
                'm', 'n' -> tail
                'o', 'p' -> ears
                'q' -> hair
                else -> continue
            }
            val bodyData = file(
                project,
                files,
                manualAtts,
                locked,
                part
            )
            if (bodyData == null && part.lowercase() in 'a' .. 'l') {
                return null
            }
            when (part) {
                'a' -> spriteTemp.head = bodyData!!
                'b' -> spriteTemp.body = bodyData!!
                'c' -> spriteTemp.leftThigh = bodyData!!
                'd' -> spriteTemp.leftShin = bodyData!!
                'e' -> spriteTemp.leftFoot = bodyData!!
                'f' -> spriteTemp.rightThigh = bodyData!!
                'g' -> spriteTemp.rightShin = bodyData!!
                'h' -> spriteTemp.rightFoot = bodyData!!
                'i' -> spriteTemp.leftUpperArm = bodyData!!
                'j' -> spriteTemp.leftForearm = bodyData!!
                'k' -> spriteTemp.rightUpperArm = bodyData!!
                'l' -> spriteTemp.rightForearm = bodyData!!
                'm' -> spriteTemp.tailBase = bodyData
                'n' -> spriteTemp.tailTip = bodyData
                'o' -> spriteTemp.leftEar = bodyData
                'p' -> spriteTemp.rightEar = bodyData
                'q' -> spriteTemp.hair = bodyData
                else -> {
                    LOGGER.severe("Invalid part '$part' called for update sprite set")
                }
            }
        }
        return spriteTemp
    }


    /**
     * Generate a set of default sprites for this pose editor
     *
     * @return sprite set with default breeds applied to the parts
     */
    private fun defaultSpriteSet(
        project: Project,
        headParts: List<BodyPartFiles>,
        bodyParts: List<BodyPartFiles>,
        legParts: List<BodyPartFiles>,
        armParts: List<BodyPartFiles>,
        tailParts: List<BodyPartFiles>,
        earParts: List<BodyPartFiles>,
        hairParts: List<BodyPartFiles>,
        manualAtts: Map<Char, BodyPartFiles>,
        locked: Map<Char, BodyPartFiles>
    ): PoseRenderer.CreatureSpriteSet? {
        val head = file(project, headParts, manualAtts, locked, 'a')
            ?: return null
        val body = file(project, bodyParts, manualAtts, locked, 'b')
            ?: return null
        val leftThigh = file(project, legParts, manualAtts, locked, 'c')
            ?: return null
        val leftShin = file(project, legParts, manualAtts, locked, 'd')
            ?: return null
        val leftFoot = file(project, legParts, manualAtts, locked, 'e')
            ?: return null
        val rightThigh = file(project, legParts, manualAtts, locked, 'f')
            ?: return null
        val rightShin = file(project, legParts, manualAtts, locked, 'g')
            ?: return null
        val rightFoot = file(project, legParts, manualAtts, locked, 'h')
            ?: return null
        val leftUpperArm = file(project, armParts, manualAtts, locked, 'i')
            ?: return null
        val leftForearm = file(project, armParts, manualAtts, locked, 'j')
            ?: return null
        val rightUpperArm = file(project, armParts, manualAtts, locked, 'k')
            ?: return null
        val rightForearm = file(project, armParts, manualAtts, locked, 'l')
            ?: return null
        val tailBase = file(project, tailParts, manualAtts, locked, 'm')
        val tailTip = file(project, tailParts, manualAtts, locked, 'n')
        val leftEar = file(project, earParts, manualAtts, locked, 'o')
        val rightEar = file(project, earParts, manualAtts, locked, 'p')
        val hair = file(project, hairParts, manualAtts, locked, 'q')
        return PoseRenderer.CreatureSpriteSet(
            head = head,
            body = body,
            leftThigh = leftThigh,
            leftShin = leftShin,
            leftFoot = leftFoot,
            rightThigh = rightThigh,
            rightShin = rightShin,
            rightFoot = rightFoot,
            leftUpperArm = leftUpperArm,
            leftForearm = leftForearm,
            rightUpperArm = rightUpperArm,
            rightForearm = rightForearm,
            tailBase = tailBase,
            tailTip = tailTip,
            leftEar = leftEar,
            rightEar = rightEar,
            hair = hair
        )
    }

    /**
     * Gets the file given for selected breed and part
     *
     * @param part     the part to get body/sprite data for
     * @return the selected Sprite/Body data object
     */
    private fun file(
        project: Project,
        files: List<BodyPartFiles>,
        manualAtts: Map<Char, BodyPartFiles>,
        locked: Map<Char, BodyPartFiles>,
        part: Char,
    ): SpriteBodyPart? {
        if (project.isDisposed) {
            return null
        }
        manualAtts[part]?.let {
            return it.data(project)
        }

        locked[part]?.let {
            return it.data(project)
        }
        val breedFile = files
            .firstOrNull {
                it.spriteFile.name[0].lowercase() == part
            }
            ?: return null
        // Check if manual att exists
        // Return Sprite body part using manual att
        return breedFile.data(project)
    }
}