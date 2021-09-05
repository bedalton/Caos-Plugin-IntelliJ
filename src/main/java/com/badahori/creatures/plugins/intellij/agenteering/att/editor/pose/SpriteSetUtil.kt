package com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose

import com.badahori.creatures.plugins.intellij.agenteering.indices.BodyPartFiles
import com.badahori.creatures.plugins.intellij.agenteering.indices.SpriteBodyPart
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.logging.Logger

object SpriteSetUtil {

    private val LOGGER = Logger.getLogger("#SpriteSetUtil")

    /**
     * Update the sprite set for changed chars and returns it
     *
     * @param holder panel to get updated sprite set for
     * @param parts parts that have changed
     * @return update sprite set
     */
    @JvmStatic
    fun getUpdatedSpriteSet(
        project: Project,
        holder: BreedPoseHolder,
        spriteSet: PoseRenderer.CreatureSpriteSet?,
        files: List<BodyPartFiles>,
        manualAtts: Map<Char, VirtualFile>,
        vararg parts: Char
    ): PoseRenderer.CreatureSpriteSet? {
        var realParts = parts
        val spriteTemp: PoseRenderer.CreatureSpriteSet = spriteSet?.copy() ?: defaultSpriteSet(
            project,
            holder,
            files,
            manualAtts
        )
        ?: return null
        if (parts.isEmpty()) {
            realParts = PoseEditorSupport.allParts
        }
        for (part in realParts) {
            val breed = holder.getPartBreed(part)
            val bodyData = breed?.let { breedFile ->
                BreedDataUtil.getSpriteBodyPart(
                    project,
                    holder.variant,
                    files,
                    manualAtts,
                    holder.baseBreed,
                    part,
                    breedFile
                )
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
        holder: BreedPoseHolder,
        files: List<BodyPartFiles>,
        manualAtts: Map<Char, VirtualFile>
    ): PoseRenderer.CreatureSpriteSet? {
        val head = file(project, holder, files, manualAtts, 'a')
            ?: return null
        val body = file(project, holder, files, manualAtts, 'b')
            ?: return null
        val leftThigh = file(project, holder, files, manualAtts, 'c')
            ?: return null
        val leftShin = file(project, holder, files, manualAtts, 'd')
            ?: return null
        val leftFoot = file(project, holder, files, manualAtts, 'e')
            ?: return null
        val rightThigh = file(project, holder, files, manualAtts, 'f')
            ?: return null
        val rightShin = file(project, holder, files, manualAtts, 'g')
            ?: return null
        val rightFoot = file(project, holder, files, manualAtts, 'h')
            ?: return null
        val leftUpperArm = file(project, holder, files, manualAtts, 'i')
            ?: return null
        val leftForearm = file(project, holder, files, manualAtts, 'j')
            ?: return null
        val rightUpperArm = file(project, holder, files, manualAtts, 'k')
            ?: return null
        val rightForearm = file(project, holder, files, manualAtts, 'l')
            ?: return null
        val tailBase = file(project, holder, files, manualAtts, 'm')
        val tailTip = file(project, holder, files, manualAtts, 'n')
        val leftEar = file(project, holder, files, manualAtts, 'o')
        val rightEar = file(project, holder, files, manualAtts, 'p')
        val hair = file(project, holder, files, manualAtts, 'q')
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
     * @param holder pose panel to fetch information for
     * @param part     the part to get body/sprite data for
     * @return the selected Sprite/Body data object
     */
    private fun file(
        project: Project,
        holder: BreedPoseHolder,
        files: List<BodyPartFiles>,
        manualAtts: Map<Char, VirtualFile>,
        part: Char
    ): SpriteBodyPart? {
        val baseFile = holder.getPartBreed(part)
        return baseFile?.let { breedFile ->
            BreedDataUtil.getSpriteBodyPart(
                project,
                holder.variant,
                files,
                manualAtts,
                holder.baseBreed,
                part,
                breedFile
            )
        }
    }
}