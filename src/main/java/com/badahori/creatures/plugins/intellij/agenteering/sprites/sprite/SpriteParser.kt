package com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite

import com.badahori.creatures.plugins.intellij.agenteering.sprites.c16.C16SpriteFile
import com.badahori.creatures.plugins.intellij.agenteering.sprites.s16.S16SpriteFile
import com.badahori.creatures.plugins.intellij.agenteering.sprites.spr.SprSpriteFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.littleEndian
import com.badahori.creatures.plugins.intellij.agenteering.utils.skip
import com.badahori.creatures.plugins.intellij.agenteering.utils.uInt16
import com.badahori.creatures.plugins.intellij.agenteering.utils.uInt32
import com.intellij.openapi.vfs.VirtualFile
import java.nio.ByteBuffer

object SpriteParser {

    @JvmStatic
    fun parse(virtualFile: VirtualFile) : SpriteFile<*> {
        return when (virtualFile.extension?.toLowerCase()) {
            "spr" -> SprSpriteFile(virtualFile)
            "c16" -> C16SpriteFile(virtualFile)
            "s16" -> S16SpriteFile(virtualFile)
            else -> throw SpriteParserException("Invalid image file extension found")
        }
    }

    @JvmStatic
    fun numImages(virtualFile: VirtualFile) : Int? {
        return try {
            val bytesBuffer = ByteBuffer.wrap(virtualFile.contentsToByteArray()).littleEndian()
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