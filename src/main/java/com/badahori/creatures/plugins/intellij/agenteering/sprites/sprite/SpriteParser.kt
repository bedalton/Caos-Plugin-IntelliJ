package com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite

import bedalton.creatures.bytes.ByteStreamReader
import bedalton.creatures.bytes.skip
import bedalton.creatures.bytes.uInt16
import com.badahori.creatures.plugins.intellij.agenteering.sprites.blk.BlkSpriteFile
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
            "back","blk" -> BlkSpriteFile(virtualFile)
            else -> throw SpriteParserException("Invalid image file extension found")
        }
    }

    @JvmStatic
    fun numImages(virtualFile: VirtualFile) : Int? {
        return try {
            val bytesBuffer = ByteStreamReader(virtualFile.contentsToByteArray())
            if (virtualFile.extension?.toLowerCase() != "spr") {
                bytesBuffer.skip(4)
            }
            bytesBuffer.uInt16
        } catch (e:Exception) {
            null
        }
    }

    val VALID_SPRITE_EXTENSIONS = listOf("spr", "c16", "s16")

}

class SpriteParserException(message:String, throwable: Throwable? = null) : Exception(message, throwable)