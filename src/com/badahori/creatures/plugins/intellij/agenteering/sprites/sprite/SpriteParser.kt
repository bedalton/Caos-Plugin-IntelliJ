package com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite

import com.badahori.creatures.plugins.intellij.agenteering.sprites.c16.C16SpriteFile
import com.badahori.creatures.plugins.intellij.agenteering.sprites.s16.S16SpriteFile
import com.badahori.creatures.plugins.intellij.agenteering.sprites.spr.SprSpriteFile
import com.intellij.openapi.vfs.VirtualFile

object SpriteParser {

    @JvmStatic
    fun parse(virtualFile: VirtualFile) : SpriteFile<*> {
        return when (virtualFile.extension?.toLowerCase()) {
            "spr" -> SprSpriteFile(virtualFile)
            "c16" -> C16SpriteFile(virtualFile)
            "s16" -> S16SpriteFile(virtualFile)
            else -> throw Exception("Invalid image file extension found")
        }
    }

    val VALID_SPRITE_EXTENSIONS = listOf("spr", "c16", "s16")

}