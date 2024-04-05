package com.badahori.creatures.plugins.intellij.agenteering.att.actions

import com.badahori.creatures.plugins.intellij.agenteering.att.parser.AttFileLine
import com.badahori.creatures.plugins.intellij.agenteering.att.parser.AttFileParser
import com.badahori.creatures.plugins.intellij.agenteering.att.lang.getInitialVariant
import com.badahori.creatures.plugins.intellij.agenteering.att.parser.toFileText
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser
import com.bedalton.io.bytes.CREATURES_CHARACTER_ENCODING
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage

internal object AttCopyMirroredUtil {

    @Throws
    internal fun copyMirrored(project: Project, copyFrom: VirtualFile, copyTo: VirtualFile) {

        val mirrorResult = mirrorAtt(project, copyFrom, copyTo)

        if (mirrorResult !is AttCopyResult.OK) {
            throw Exception("Failed to mirror att. " + (mirrorResult as AttCopyResult.Error).error)
        }

        // Figure out the variant of this file
        val variant: CaosVariant = getInitialVariant(project, copyTo)

        // Format ATT lines for write
        val newText = mirrorResult.lines.toFileText(variant)

        //Write changes on the writeAction thread
        runUndoTransparentWriteAction {
            copyTo.setBinaryContent(newText.toByteArray(CREATURES_CHARACTER_ENCODING))
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    internal fun mirrorAtt(project: Project, copyFrom: VirtualFile, copyTo: VirtualFile): AttCopyResult {
        validateFileType(copyFrom, copyTo)?.let { error ->
            return error
        }
        // Get sprites for the copyFrom file
        val copyFromSprite = getSprite(project, copyFrom)
            ?: return AttCopyResult.Error("Failed to find copy to sprite")

        // Get copy to sprites
        val copyToSprite = getSprite(project, copyTo)
            ?: return AttCopyResult.Error("Failed to find copy to sprite")

        // Ensure both files have the same number of sprites
        if (copyToSprite.size != copyFromSprite.size) {
            return AttCopyResult.Error("Sprite file count mismatch")
        }

        // TODO validate is corresponding sprite body

        // Get lines in copyFrom file
        val attLines = AttFileParser.parse(project, copyFrom).lines

        // Ensure there are enough ATT lines for the sprites in the file
        if (attLines.size < copyFromSprite.size) {
            return AttCopyResult.Error("Not enough ATT lines to cover all images")
        }
        val numSprites = copyToSprite.size
        // Offset all ATT lines for present images
        val lines = try {
            copyFromSprite.mapIndexed { i, image ->
                val otherSprite = if (i < numSprites - 2) {
                    copyToSprite[numSprites - i - 1]
                } else {
                    copyToSprite[i]
                }
                AttFileLine(
                    calculateOffset(attLines[i], image, otherSprite)
                )
            }
        } catch (e: Exception) {
            if (e is ProcessCanceledException) {
                throw e
            }
            // Most likely thrown from images being null
            return AttCopyResult.Error("Invalid or empty sprite encountered")
        }
        return AttCopyResult.OK(lines)
    }

    /**
     * Calculates the offset of X,Y from the base sprite to the copyTo sprite
     * Offsets are calculated from first visiblePixel and last visible pixel
     */
    private fun calculateOffset(
        attLine: AttFileLine,
        copyFrom: BufferedImage,
        copyTo: BufferedImage,
    ): List<Pair<Int, Int>> {
        val fromOffsets = getOffsets(copyFrom)
        val toOffsets = getOffsets(copyTo)
        val fromStartX = fromOffsets.startX
        val toEndX = toOffsets.endX
        return attLine.points.map {
            val fromOffsetX = it.first - fromStartX
            val toOffsetX = toEndX - fromOffsetX
            val yMod = toOffsets.startY - fromOffsets.startY
            val toOffsetY = it.second + yMod
            Pair(if (toOffsetX < 0) 0 else toOffsetX, if (toOffsetY < 0) 0 else toOffsetY)
        }
    }

    private fun getOffsets(image: BufferedImage): Offsets {
        val width = image.width
        val height = image.height
        var startX: Int = width
        var startY: Int = height
        var endX = 0
        var endY = 0
        repeat(height) { y ->
            repeat(width) { x ->
                val pixel = image.getRGB(x, y)
                val solid = pixel shr 24 != 0x00
                if (solid) {
                    if (startX > x)
                        startX = x
                    if (endX < x)
                        endX = x
                    if (startY > y)
                        startY = y
                    if (endY < y)
                        endY = y
                }
            }
        }
        return Offsets(
            startX,
            startY,
            endX,
            endY
        )
    }

    private fun validateFileType(copyFrom: VirtualFile, copyTo: VirtualFile): AttCopyResult.Error? {
        var error: String? = null
        if (copyTo.extension?.lowercase() != "att")
            error = "Copy to file"
        if (copyFrom.extension?.lowercase() != "att")
            error =
                if (error != null) "$error and copy from file are not att files" else "Copy from file is not an att file"
        if (error != null) {
            return if (!error.endsWith("files") && !error.endsWith("file"))
                AttCopyResult.Error("$error is not an att file")
            else
                AttCopyResult.Error(error)
        }
        return null
    }

    private fun getSprite(project: Project, attFile: VirtualFile): List<BufferedImage>? {
        val spriteFile = getAnyPossibleSprite(project, attFile)
            ?: return null
        return runBlocking { SpriteParser
            .parse(spriteFile)
            .imagesAsync()
        }
    }

    sealed class AttCopyResult {
        data class OK(val lines: List<AttFileLine>) : AttCopyResult()
        data class Error(val error: String) : AttCopyResult()
    }

    data class Offsets(
        val startX: Int,
        val endX: Int,
        val startY: Int,
        val endY: Int,
    )

}