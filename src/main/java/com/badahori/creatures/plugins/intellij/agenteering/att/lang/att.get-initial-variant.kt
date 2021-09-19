package com.badahori.creatures.plugins.intellij.agenteering.att.lang

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.cachedVariantExplicitOrImplicit
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.setCachedVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile


/**
 * Tries to determine the initial variant of an att file or a sprite file using itself and its corresponding other
 */
internal fun getInitialVariant(project: Project?, file: VirtualFile): CaosVariant {

    // Get cached variant if any
    file.cachedVariantExplicitOrImplicit.nullIfUnknown()?.let {
        //return it
    }

    // May not actually be a breed file
    if (file.nameWithoutExtension.length != 4) {
        // If project is non-null, try getting module variant
        if (project != null)
            file.getModule(project)?.variant?.let {
                return it
            }
        // If nothing else try to guess based on file extension
        val variant = when (file.extension?.toLowerCase()) {
            "spr" -> CaosVariant.C1
            "c16" -> CaosVariant.C3
            "s16" -> CaosVariant.UNKNOWN
            "att" -> getVariantByAttLengths(file, null)
            else -> CaosVariant.UNKNOWN
        }
        // Cache whatever is returned
        file.setCachedVariant(variant.nullIfUnknown(), false)

        // Return result
        return variant
    }
    // Get breed char
    val breed = file.name.toLowerCase()[3]

    // Get part char
    val part = file.name.toLowerCase()[0]

    // Check if part is CV only
    if (part in listOf('o', 'p', 'q'))
        return CaosVariant.CV

    // Check if breed is C1 only
    if (breed in '0'..'9') {
        return CaosVariant.C1
    }

    // If project is non-null, try and get module variant
    if (project != null) {
        // Get module variant first
        file.getModule(project)?.variant?.nullIfUnknown()?.let {
            file.setCachedVariant(it, false)
            return it
        }
    }

    // Try to figure out type if file is sprite
    val variant = when (file.extension?.toLowerCase()) {
        "spr" -> CaosVariant.C1
        "c16" -> {
            val numImages = SpriteParser.numImages(file)
            when (part) {
                'a' -> when (numImages) {
                    576 -> CaosVariant.CV
                    else -> CaosVariant.C3
                }
                // 'o', 'p', 'q' -> CaosVariant.CV already checked for
                else -> CaosVariant.C3
            }
        }
        "s16" -> {
            val numImages = SpriteParser.numImages(file)
            when (part) {
                'a' -> when (numImages) {
                    120 -> CaosVariant.C2
                    576 -> CaosVariant.CV
                    else -> CaosVariant.C3
                }
                // 'o', 'p', 'q' -> CaosVariant.CV already checked for
                else -> if (numImages == 10)
                    CaosVariant.C2
                else
                    CaosVariant.C3
            }
        }
        else -> getVariantByAttLengths(file, part)
    }
    file.setCachedVariant(variant, false)
    return variant
}

// Tries to determine the Variant based on att file length alone
private fun getVariantByAttLengths(file: VirtualFile, part: Char?, searchSprite:Boolean = true): CaosVariant {
    val contents = file.contents.trim()
    val lines = contents.split("[\r\n]+".toRegex()).filter { it.isNotBlank() }
    if (lines.size == 10) {
        if (searchSprite) {
            val sprite = file.findChildInSelfOrParent(file.nameWithoutExtension, listOf(".spr", "s16"), true)
                ?: return CaosVariant.UNKNOWN
            return if (sprite.extension == "spr")
                CaosVariant.C1
            else
                CaosVariant.C2
        }
        return CaosVariant.UNKNOWN
    }
    if (part == 'a') {
        val longestLine = lines.maxBy { it.length }
            ?: return CaosVariant.C3
        val points = longestLine.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (points.lastOrNull()?.toIntSafe().orElse(0) > 0)
            return CaosVariant.CV
        return CaosVariant.C3
    }
    val partLowerCase = part?.toLowerCase()
    if (partLowerCase == 'o' || partLowerCase == 'p' || partLowerCase == 'q')
        return CaosVariant.CV
    return CaosVariant.C3
}
