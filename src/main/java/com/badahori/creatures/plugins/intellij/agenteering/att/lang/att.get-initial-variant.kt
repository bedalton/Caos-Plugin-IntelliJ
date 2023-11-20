package com.badahori.creatures.plugins.intellij.agenteering.att.lang

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.cachedVariantExplicitOrImplicit
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.setCachedVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.inferVariantHard
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.settings
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import kotlinx.coroutines.runBlocking


internal fun getInitialVariant(project: Project?, file: VirtualFile): CaosVariant {
    // Get cached variant if any
    file.cachedVariantExplicitOrImplicit.nullIfUnknown()?.let {
        return it
    }
    val variant = getInitialVariantImpl(project, file)
    if (variant == null) {
        file.setCachedVariant(null, false)
        return CaosVariant.UNKNOWN
    }
    file.setCachedVariant(variant, false)
    for (sibling in file.parent?.children.orEmpty()) {
        if (!sibling.isDirectory) {
            sibling.setCachedVariant(variant, false)
        }
    }
    return variant
}
/**
 * Tries to determine the initial variant of an att file or a sprite file using itself and its corresponding other
 */
private fun getInitialVariantImpl(project: Project?, file: VirtualFile): CaosVariant? {

    // May not actually be a breed file
    if (file.nameWithoutExtension.length != 4) {
        // If project is non-null, try getting module variant
        if (project != null)
            file.getModule(project)?.variant?.let {
                return it
            }
        // If nothing else try to guess based on file extension
        val variant = when (file.extension?.lowercase()) {
            "spr" -> CaosVariant.C1
            "c16" -> CaosVariant.C3
            "s16" -> null
            "att" -> getVariantByAttLengths(project, file, null)
            else -> null
        }
        // Cache whatever is returned
        file.setCachedVariant(variant, false)

        // Return result
        return variant
    }
    // Get breed char
    val breed = file.name.lowercase()[3]

    // Get part char
    val part = file.name.lowercase()[0]

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
        (project.settings.defaultVariant)?.let {
            return it
        }
    }


    // Try to figure out type if file is sprite
    val variant = when (file.extension?.lowercase()) {
        "spr" -> CaosVariant.C1
        "c16" -> {
            val imageCount = runBlocking { SpriteParser.imageCount(file) }
            when (part) {
                'a' -> when (imageCount) {
                    576 -> CaosVariant.CV
                    else -> CaosVariant.C3
                }
                // 'o', 'p', 'q' -> CaosVariant.CV already checked for
                else -> CaosVariant.C3
            }
        }
        "s16" -> {
            val imageCount = runBlocking { SpriteParser.imageCount(file) }
            when (part) {
                'a' -> when (imageCount) {
                    120 -> CaosVariant.C2
                    576 -> CaosVariant.CV
                    else -> CaosVariant.C3
                }
                // 'o', 'p', 'q' -> CaosVariant.CV already checked for
                else -> if (imageCount == 10) {
                    CaosVariant.C2
                } else {
                    CaosVariant.C3
                }
            }
        }
        else -> getVariantByAttLengths(project, file, part)
    }
    return variant
}

// Tries to determine the Variant based on att file length alone
private fun getVariantByAttLengths(
    project: Project?,
    file: VirtualFile,
    part: Char?,
    searchSprite: Boolean = true
): CaosVariant {
    val scope = if (project != null) {
        file.getModule(project)?.moduleContentScope
            ?: GlobalSearchScope.projectScope(project)
    } else {
        null
    }

    val contents = file.contents.trimEnd(' ', '\n', '\r', '\t')
    val lines = contents.split("[\r\n]+".toRegex()).filter { it.isNotBlank() }
    if (lines.size == 10) {
        if (searchSprite) {
            val sprite =
                file.parent.findChildInSelfOrParent(file.nameWithoutExtension, listOf(".spr", "s16"), true, scope)
                    ?: file.parent.parent?.findChildInSelfOrParent(file.nameWithoutExtension, listOf(".spr", "s16"), true, scope)
                    ?: return (project?.inferVariantHard() ?: CaosVariant.UNKNOWN)
            return if (sprite.extension == "spr") {
                CaosVariant.C1
            } else {
                CaosVariant.C2
            }
        }
        return CaosVariant.UNKNOWN
    }
    if (part == 'a') {
//        val longestLine = lines.maxByOrNull { it.length }
//            ?: return CaosVariant.C3
        // USING last point led to false CV positives. Decided to just make it CV if asked
//        val points = longestLine.split("\\s+".toRegex()).filter { it.isNotBlank() }
//        if (points.lastOrNull()?.toIntSafe().orElse(0) > 0) {
//            return CaosVariant.CV
//        }
        return CaosVariant.C3
    }
    val partLowerCase = part?.lowercase()
    if (partLowerCase == 'o' || partLowerCase == 'p' || partLowerCase == 'q')
        return CaosVariant.CV
    return CaosVariant.C3
}
