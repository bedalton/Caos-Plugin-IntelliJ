package com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite

//import bedalton.creatures.bytes.MemoryByteStreamReader
import bedalton.creatures.bytes.ByteStreamReader
import bedalton.creatures.bytes.readAt
import bedalton.creatures.bytes.skip
import bedalton.creatures.bytes.uInt16
import bedalton.creatures.sprite.parsers.*
import bedalton.creatures.sprite.util.SpriteType
import bedalton.creatures.sprite.util.SpriteType.*
import bedalton.creatures.util.Log
import bedalton.creatures.util.iIf
import bedalton.creatures.util.ifLog
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.SpriteSetUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.flipHorizontal
import com.badahori.creatures.plugins.intellij.agenteering.utils.lowercase
import com.badahori.creatures.plugins.intellij.agenteering.utils.toListOf
import com.badahori.creatures.plugins.intellij.agenteering.vfs.VirtualFileStreamReader
import com.intellij.openapi.vfs.VirtualFile
import com.soywiz.korim.awt.toAwt
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.RGBA
import java.awt.image.BufferedImage

object SpriteParser {

    @JvmStatic
    fun parse(virtualFile: VirtualFile): SpriteFileHolder {
        return parse(virtualFile, null, null)
    }

    @JvmStatic
    fun parse(virtualFile: VirtualFile, bodyPart: Boolean? = null): SpriteFileHolder {
        return parse(virtualFile, bodyPart, null)
    }

    @JvmStatic
    fun parse(
        virtualFile: VirtualFile,
        callback: ((i: Int, total: Int) -> Boolean?)?,
    ): SpriteFileHolder {
        return parse(virtualFile, null, callback)
    }

    @JvmStatic
    fun parse(
        virtualFile: VirtualFile,
        bodyPart: Boolean? = null,
        callback: ((i: Int, total: Int) -> Boolean?)?,
    ): SpriteFileHolder {
        val extension = virtualFile.extension?.uppercase()
            ?: throw SpriteParserException("Cannot get sprite parser without file extension")
        val type = SpriteType.fromString(extension)
        val stream = VirtualFileStreamReader(virtualFile)
        return parse(type, stream, 1, fileName = virtualFile.name, bodyPart, callback)
    }

    @JvmStatic
    fun parse(
        extension: String,
        stream: ByteStreamReader,
        fileName: String? = null,
        bodyPart: Boolean? = null,
        callback: ((i: Int, total: Int) -> Boolean?)? = null,
    ): SpriteFileHolder {
        val type = SpriteType.fromString(extension.uppercase())
        return parse(type, stream, 1, fileName, bodyPart, callback)
    }

    @JvmStatic
    fun parse(
        spriteType: SpriteType,
        stream: ByteStreamReader,
        progressEvery: Int = 1,
        fileName: String? = null,
        bodyPart: Boolean? = null,
        callback: ((i: Int, total: Int) -> Boolean?)? = null,
    ): SpriteFileHolder {

        Log.setMode(SPR_SHORT_DEBUG_LOGGING, true)
        return when (spriteType) {
            SPR ->
                try {
                    SprSpriteFile(stream, false, progressEvery, callback)
                        .toListOf().apply {
                            Log.iIf(SPR_SHORT_DEBUG_LOGGING) {
                                "Is default sprite format"
                            }
                        }
                } catch (e: Exception) {
                    Log.iIf(SPR_SHORT_DEBUG_LOGGING) { "Parsing SPR short: $fileName" }
                    try {
                        stream.readAt(0) {
                            SprSetDecompiler.decompileSets(
                                stream,
                                keepBlack = false,
                                null,
                                null,
                            )!!
                        }
                    } catch (_: Exception) {
                        throw e
                    }
                }
            S16 -> S16SpriteFile(stream, false, progressEvery, callback)
                .toListOf()
            C16 -> C16SpriteFile(stream, false, progressEvery, callback)
                .toListOf()
            BLK -> BlkSpriteFile(stream, progressEvery, callback)
                .toListOf()
            else -> throw SpriteParserException("Invalid image file extension found")
        }.let { spriteSets ->
            SpriteFileHolder(spriteSets, fileName, bodyPart ?: false)
        }
    }

    @JvmStatic
    fun imageCount(virtualFile: VirtualFile): Int? {
        return try {
            val bytesBuffer = VirtualFileStreamReader(virtualFile)
            if (virtualFile.extension?.lowercase() != "spr") {
                bytesBuffer.skip(4)
            }
            val size = bytesBuffer.uInt16
            bytesBuffer.close()
            size
        } catch (e: Exception) {
            null
        }
    }

    @JvmStatic
    fun getBodySpriteVariant(spriteFile: VirtualFile, defaultVariant: CaosVariant): CaosVariant {
        if (!BreedPartKey.isPartName(spriteFile.nameWithoutExtension)) {
            return defaultVariant
        }
        val extension = spriteFile.extension?.lowercase()
            ?: defaultVariant
        if (extension !in SpriteParser.VALID_SPRITE_EXTENSIONS) {
            return defaultVariant
        }
        if (extension == "spr") {
            return CaosVariant.C1
        }

        val part = spriteFile.name.getOrNull(0)?.lowercase()
            ?: defaultVariant

        val imageCount = try {
            imageCount(spriteFile)
                ?: return defaultVariant
        } catch (e: Exception) {
            return defaultVariant
        }
        if (extension == "s16") {
            if (imageCount == 10 || imageCount == 120) {
                return CaosVariant.C2
            }
        }
        if (defaultVariant == CaosVariant.CV) {
            return CaosVariant.CV
        }
        if (part == 'a') {
            if (imageCount > 192) {
                return CaosVariant.CV
            }
        }
        return CaosVariant.C3
    }

    val VALID_SPRITE_EXTENSIONS = listOf("spr", "c16", "s16")

}

class SpriteParserException(message: String, throwable: Throwable? = null) : Exception(message, throwable)


class SpriteFileHolder(sprites: List<SpriteFile>, val fileName: String? = null, private val bodyPart: Boolean = false) {


    constructor(sprite: SpriteFile, fileName: String? = null) : this(
        sprite.toListOf(), fileName
    )

    constructor(sprite: SpriteFile, fileName: String? = null, bodyPart: Boolean) : this(
        sprite.toListOf(), fileName, bodyPart
    )

    val type = sprites.first().type

    private var mSpriteFile: List<SpriteFile> = sprites

    val bitmaps: List<List<Bitmap32>>
        get() = mSpriteFile.map {
            it.frames
        }

    val images: List<BufferedImage>
        get() {
            return mSpriteFile.first().frames.map(Bitmap32::toAwt)
        }

    val imageSets: List<List<BufferedImage>>
        get() {
            return mSpriteFile.map { file ->
                file.frames.map {
                    it.toAwt()
                }
            }
        }

    val size: Int = mSpriteFile.firstOrNull()?.size ?: 0

    fun getSize(spriteFileIndex: Int): Int? {
        return mSpriteFile.getOrNull(spriteFileIndex)?.size
    }

    operator fun get(frame: Int): BufferedImage? {
        return if (bodyPart) {
            images.getBodyImageAt(frame)
        } else {
            images.getOrNull(frame)
        }
    }

    operator fun get(spriteFileIndex: Int, frame: Int): BufferedImage? {
        return imageSets.getOrNull(spriteFileIndex)?.getOrNull(frame)
    }

}

internal val transparentBlack = RGBA(0, 0, 0, 0)
//internal val solidBlack = RGBA(0,0,0)


private fun List<BufferedImage>.getBodyImageAt(i: Int): BufferedImage {
    var image = get(i)
    val width = image.width
    val height = image.height
    if (width != 32 || height != 32) {
        return image
    }
    for (y in 0 until height) {
        for (x in 0 until width) {
            if ((image.getRGB(x, y) shr 24) != 0x00) {
                return image
            }
        }
    }
    image = when {
        i in 0..3 -> get(i + 4)
        i in 4..7 -> get(i - 4)
        else -> image
    }
    return image.flipHorizontal()
}