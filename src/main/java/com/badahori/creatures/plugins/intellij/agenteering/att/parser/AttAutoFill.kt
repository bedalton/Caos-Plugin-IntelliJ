package com.badahori.creatures.plugins.intellij.agenteering.att.parser

import bedalton.creatures.util.FileNameUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser
import com.badahori.creatures.plugins.intellij.agenteering.utils.WHITESPACE
import com.badahori.creatures.plugins.intellij.agenteering.utils.lowercase
import com.badahori.creatures.plugins.intellij.agenteering.utils.toIntSafe
import com.intellij.openapi.vfs.VirtualFile

object AttAutoFill {

    @JvmStatic
    fun getExpected(fileName: String, variant: CaosVariant): Pair<Int, Int>? {
        if (variant.nullIfUnknown() == null) {
            return null
        }
        if (!BreedPartKey.isPartName(fileName)) {
            return null
        }
        val part = fileName[0].lowercase()

        if (variant.isOld) {
            return if (part == 'b') {
                Pair(10, 6)
            } else {
                Pair(10, 2)
            }
        }
        return when (part) {
            'a' -> Pair(16, 5)
            'b' -> Pair(16, 6)
            else -> Pair(16, 2)
        }
    }

    @JvmStatic
    fun paddedData(attFile: String? = null, spriteFile: VirtualFile, defaultVariant: CaosVariant): Pair<CaosVariant, AttFileData>? {
        if (!BreedPartKey.isPartName(spriteFile.nameWithoutExtension)) {
            return null
        }
        val expected = getExpected(spriteFile.name, defaultVariant)
            ?: return null

        val variant = SpriteParser.getBodySpriteVariant(spriteFile, defaultVariant)
        // Get ATT padded
        val attData = AttFileParser
            .parse(
                attFile ?: "",
                expectedLines = expected.first,
                expectedPoints = expected.second,
                fileName = spriteFile.nameWithoutExtension + ".att"
            )

        return Pair(variant, attData)
    }

    @JvmStatic
    fun blankAttData(fileName: String, variant: CaosVariant): AttFileData? {
        val nameWithoutExtension = FileNameUtil.getFileNameWithoutExtension(fileName)
            ?: return null
        if (!BreedPartKey.isPartName(nameWithoutExtension)) {
            return null
        }
        val (expectedLines, expectedPoints) = getExpected(nameWithoutExtension, variant)
            ?: return null
        val lines = (0 until expectedLines).map { _ ->
            val points = (0 until expectedPoints).map {
                Pair(0, 0)
            }
            AttFileLine(points = points)
        }
        return AttFileData(
            lines,
            fileName
        )
    }
    @JvmStatic
    fun blankAttText(fileName: String, variant: CaosVariant): String? {
        val nameWithoutExtension = FileNameUtil.getFileNameWithoutExtension(fileName)
            ?: return null
        if (!BreedPartKey.isPartName(nameWithoutExtension)) {
            return null
        }
        val (expectedLines, expectedPoints) = getExpected(nameWithoutExtension, variant)
            ?: return null
        val line = "0 ".repeat(expectedPoints * 2).trim() + "\n"
        return line.repeat(expectedLines)
    }


}