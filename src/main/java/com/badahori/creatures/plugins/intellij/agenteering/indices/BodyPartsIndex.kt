package com.badahori.creatures.plugins.intellij.agenteering.indices

import com.badahori.creatures.plugins.intellij.agenteering.att.indices.AttFileByVariantIndex
import com.badahori.creatures.plugins.intellij.agenteering.att.indices.AttFilesIndex
import com.badahori.creatures.plugins.intellij.agenteering.att.lang.getInitialVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.sprites.indices.BreedSpriteIndex
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import java.io.File


object BodyPartsIndex {

    @JvmStatic
    fun variantParts(project: Project, gameVariant: CaosVariant): List<BodyPartFiles> {
        val fudgedVariant = if (gameVariant == CaosVariant.DS)
            CaosVariant.C3
        else
            gameVariant
        val attFiles = AttFileByVariantIndex.findMatching(project, fudgedVariant)
        return matchAttsToSprite(project, attFiles) { attFile ->
            BreedPartKey.fromFileName(attFile.nameWithoutExtension, variant = fudgedVariant)
        }
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
                val pPath = parent.path.let { if (it.endsWith(File.separator)) it else it + File.separator }
                //    ?: break
                matchingSprite = matchingSprites.firstOrNull check@{ spriteFile ->
                    spriteFile.path.startsWith(pPath).orFalse()
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

    private fun matchAttsToSprite(project:Project, attFiles:Collection<VirtualFile>, makeKey:(VirtualFile) -> BreedPartKey?) : List<BodyPartFiles> {
        // If there are no att files, there is nothing to do
        if (attFiles.isEmpty()) {
            return emptyList()
        }

        val out = mutableListOf<BodyPartFiles>()

        // Match atts to sprites
        for (attFile in attFiles) {
            // Get the sprite search key
            val key = makeKey(attFile)
                ?: continue

            val scope = attFile.getModule(project)?.moduleContentScope
            // Find matching sprites
            val matchingSprites = BreedSpriteIndex.findMatching(project, key, scope).nullIfEmpty()
                ?: continue
            var matchingSprite: VirtualFile? = null

            // Find sprites under parent
            // TODO, find nearest path to parent
            var parent: VirtualFile? = attFile.parent
            var offset = Int.MAX_VALUE
            while (parent != null) {
                // IN IntelliJ paths use '/' on both windows and linux in VFS
                val pPath = (parent.canonicalPath?:parent.path).toLowerCase().let { if (it.endsWith('/')) it else "$it/" }
                    //?: continue

                // Finds nearest sprite to parent directory
                for (spriteFile in matchingSprites) {
                    val spritePath = (spriteFile.canonicalPath ?: spriteFile.path).toLowerCase()
                        //?: continue
                    if (!spritePath.startsWith(pPath).orFalse())
                        continue
                    if (spritePath.length < offset) {
                        offset = spritePath.length
                        matchingSprite = spriteFile
                    }
                }

                if (matchingSprite != null) {
                    break
                }
                parent = parent.parent
                    ?: break
            }
            if (matchingSprite != null) {
                out.add(BodyPartFiles(spriteFile = matchingSprite, bodyDataFile = attFile))
            }
        }
        return out;
    }
}