package com.badahori.creatures.plugins.intellij.agenteering.sprites.indices

import bedalton.creatures.common.util.FileNameUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.VirtualFileUtil
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.openapi.vfs.VirtualFile

object SpriteLocator {



    fun findClosest(variant: CaosVariant?, fileName: String, directory: VirtualFile): VirtualFile? {
        val defaultExtensions = when(variant) {
            CaosVariant.C1 -> arrayOf("spr")
            CaosVariant.C2 -> arrayOf("s16")
            else -> arrayOf("s16", "c16")
        }
        var parent: VirtualFile? = directory
        val rawExtension = FileNameUtil.getExtension(fileName)?.nullIfEmpty()?.lowercase()
        val extensions = if (rawExtension == null || rawExtension !in listOf("spr", "s16", "c16", "blk"))
            defaultExtensions
        else
            arrayOf(rawExtension)
        val filenameWithoutExtension = FileNameUtil
            .getFileNameWithoutExtension(fileName)
            ?.lowercase()
            ?: return null
        val fileNames = extensions.map {ext ->
            "$filenameWithoutExtension.$ext"
        }
        var matches: List<VirtualFile>? = null
        while (parent != null) {
            matches = VirtualFileUtil
                .childrenWithExtensions(directory, true, *extensions)
                .filter {
                    it.name.lowercase() in fileNames
                }
                .nullIfEmpty()
            if (matches != null) {
                break
            }
            parent = parent.parent
        }
        if (matches == null) {
            return null
        }

        return VirtualFileUtil.nearest(directory, matches)
    }


}