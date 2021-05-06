package com.badahori.creatures.plugins.intellij.agenteering.indices

import com.badahori.creatures.plugins.intellij.agenteering.att.indices.AttFileByVariantIndex
import com.badahori.creatures.plugins.intellij.agenteering.att.indices.AttFilesIndex
import com.badahori.creatures.plugins.intellij.agenteering.att.lang.getInitialVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.sprites.indices.BreedSpriteIndex
import com.badahori.creatures.plugins.intellij.agenteering.utils.findChildInSelfOrParent
import com.badahori.creatures.plugins.intellij.agenteering.utils.isNotNullOrEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import java.io.File


object BodyPartsIndex {

    @JvmStatic
    fun variantParts(project: Project, gameVariant: CaosVariant): List<BodyPartFiles> {
        val fudgedVariant = if (gameVariant == CaosVariant.DS)
            CaosVariant.C3
        else
            gameVariant
        val out = mutableListOf<BodyPartFiles>()
        val attFiles = AttFileByVariantIndex.findMatching(project, fudgedVariant)
        if (attFiles.isEmpty()) {
            return emptyList()
        }

        for (attFile in attFiles) {
            val key = BreedPartKey.fromFileName(attFile.nameWithoutExtension, variant = fudgedVariant)
                ?: continue
            val matchingSprites = BreedSpriteIndex.findMatching(project, key).nullIfEmpty()
            var parent: VirtualFile? = attFile.parent
            var matchingSprite: VirtualFile? = null
            while (matchingSprites.isNotNullOrEmpty() && parent != null) {
                val pPath = parent.canonicalPath?.let { if (it.endsWith(File.separator)) it else it + File.separator }
                    ?: break
                matchingSprite = matchingSprites.firstOrNull check@{ spriteFile ->
                    spriteFile.canonicalPath?.startsWith(pPath).orFalse()
                }
                if (matchingSprite != null)
                    break
                parent = parent.parent
                    ?: break
            }
            if (matchingSprite != null) {
                out.add(BodyPartFiles(spriteFile = matchingSprite, bodyDataFile = attFile))
            } else {
//                parent?.findChildInSelfOrParent(attFile.nameWithoutExtension, variantExtensions, true)?.let { sprite ->
//                    out.add(BodyPartFiles(spriteFile = sprite, bodyDataFile = attFile))
//                }
            }
        }
        return out
    }

    @JvmStatic
    fun findWithKey(project: Project, searchKey: BreedPartKey, scope:GlobalSearchScope? = null): List<BodyPartFiles> {
        val out = mutableListOf<BodyPartFiles>()
        val attFiles = AttFilesIndex.findMatching(project, searchKey, scope)
        for (attFile in attFiles) {
            val variant = getInitialVariant(project, attFile)
            val attKey = BreedPartKey.fromFileName(attFile.nameWithoutExtension, variant = variant)
                ?: continue
            val matchingSprites = BreedSpriteIndex.findMatching(project, attKey)
            var parent: VirtualFile? = attFile.parent
            var matchingSprite: VirtualFile? = null
            while (parent != null) {
                val pPath = parent.canonicalPath?.let { if (it.endsWith(File.separator)) it else it + File.separator }
                    ?: break
                matchingSprite = matchingSprites.firstOrNull check@{ spriteFile ->
                    spriteFile.canonicalPath?.startsWith(pPath).orFalse()
                }
                if (matchingSprite != null)
                    break
                parent = parent.parent
                    ?: break
            }
            if (matchingSprite != null) {
                out.add(BodyPartFiles(spriteFile = matchingSprite, bodyDataFile = attFile))
            }
        }
        return out
    }
}