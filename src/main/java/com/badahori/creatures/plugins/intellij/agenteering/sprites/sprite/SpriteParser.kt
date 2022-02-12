package com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite

//import bedalton.creatures.bytes.MemoryByteStreamReader
import bedalton.creatures.bytes.ByteStreamReader
import bedalton.creatures.bytes.skip
import bedalton.creatures.bytes.uInt16
import bedalton.creatures.sprite.parsers.*
import bedalton.creatures.sprite.util.SpriteType
import bedalton.creatures.sprite.util.SpriteType.*
import com.badahori.creatures.plugins.intellij.agenteering.vfs.VirtualFileStreamReader
import com.intellij.openapi.vfs.VirtualFile
import com.soywiz.korim.awt.toAwt
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.RGBA
import java.awt.image.BufferedImage

object SpriteParser {

    @JvmStatic
    fun parse(virtualFile: VirtualFile) : SpriteFileHolder {
        return parse(virtualFile, null)
    }

    @JvmStatic
    fun parse(virtualFile: VirtualFile, callback: ((i:Int, total: Int) -> Boolean?)?) : SpriteFileHolder {
        val extension = virtualFile.extension?.uppercase()
            ?: throw SpriteParserException("Cannot get sprite parser without file extension")
        val type = SpriteType.fromString(extension)
        val stream =  VirtualFileStreamReader(virtualFile)
        return parse(type, stream, 1, callback)
    }

    @JvmStatic
    fun parse(
        extension: String,
        stream: ByteStreamReader,
        callback: ((i:Int, total: Int) -> Boolean?)? = null
    ) : SpriteFileHolder {
        val type = SpriteType.fromString(extension.uppercase())
        return parse(type, stream, 1, callback)
    }

    @JvmStatic
    fun parse(
        spriteType: SpriteType,
        stream: ByteStreamReader,
        progressEvery: Int = 1,
        callback: ((i:Int, total: Int) -> Boolean?)? = null
    ) : SpriteFileHolder {
        return when (spriteType) {
            SPR -> SprSpriteFile(stream, false, progressEvery, callback)
            S16 -> S16SpriteFile(stream, false, progressEvery, callback)
            C16 -> C16SpriteFile(stream, false, progressEvery, callback)
            BLK -> BlkSpriteFile(stream,progressEvery, callback)
            else -> throw SpriteParserException("Invalid image file extension found")
        }.let {
            SpriteFileHolder(it)
        }
    }

    @JvmStatic
    fun numImages(virtualFile: VirtualFile) : Int? {
        return try {
            val bytesBuffer = VirtualFileStreamReader(virtualFile)
            if (virtualFile.extension?.lowercase() != "spr") {
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


class SpriteFileHolder(sprite: SpriteFile) {

    val type = sprite.type

    private var mSpriteFile: SpriteFile = sprite

    val bitmaps:List<Bitmap32> get() = mSpriteFile.frames

    val images: List<BufferedImage> get() {
        return mSpriteFile.frames.map {
            it.toAwt()
        }
    }

    val size: Int = sprite.size

    operator fun get(frame: Int): BufferedImage? = images.getOrNull(frame)

}

internal val transparentBlack = RGBA(0,0,0,0)
//internal val solidBlack = RGBA(0,0,0)