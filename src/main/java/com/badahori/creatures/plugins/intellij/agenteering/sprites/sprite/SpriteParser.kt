@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite

import com.bedalton.common.util.PathUtil
import com.bedalton.common.util.toListOf
import com.bedalton.creatures.sprite.parsers.*
import com.bedalton.creatures.sprite.util.SpriteType
import com.bedalton.creatures.sprite.util.SpriteType.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.badahori.creatures.plugins.intellij.agenteering.utils.flipHorizontal
import com.badahori.creatures.plugins.intellij.agenteering.utils.lowercase
import com.badahori.creatures.plugins.intellij.agenteering.vfs.VirtualFileStreamReader
import com.bedalton.io.bytes.*
import com.bedalton.log.*
import com.intellij.openapi.vfs.VirtualFile
import korlibs.image.awt.toAwt
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage

object SpriteParser {

    private const val BLK_ASYNC_DEFAULT = true


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
        return parse(type, stream, fileName = virtualFile.name, 1, bodyPart, callback)
    }

    @JvmStatic
    fun parse(
        extension: String,
        stream: ByteStreamReader,
        fileName: String,
        bodyPart: Boolean? = null,
        callback: ((i: Int, total: Int) -> Boolean?)? = null,
    ): SpriteFileHolder {
        val type = SpriteType.fromString(extension.uppercase())
        return parse(type, stream, fileName, 1, bodyPart, callback)
    }

    @JvmStatic
    fun parse(
        spriteType: SpriteType,
        stream: ByteStreamReader,
        fileName: String,
        progressEvery: Int = 1,
        bodyPart: Boolean? = null,
        callback: ((i: Int, total: Int) -> Boolean?)? = null,
    ): SpriteFileHolder {

        Log.setMode(SPR_SHORT_DEBUG_LOGGING, true)
        return when (spriteType) {
            SPR ->
                try {
                    SprSpriteFile(stream, false, progressEvery, callback)
                        .toListOf()
                } catch (e: Exception) {
                    Log.iIf(SPR_SHORT_DEBUG_LOGGING) { "Parsing SPR short: $fileName" }
                    try {
                        SprSetDecompiler.decompileSets(
                            stream,
                            keepBlack = false,
                            null,
                            null,
                        )!!
                    } catch (_: Exception) {
                        throw e
                    }
                }

            S16 -> S16SpriteFile(stream, false, null, progressEvery, callback)
                .toListOf()

            C16 -> C16SpriteFile(stream, false, null, progressEvery, callback)
                .toListOf()

            BLK -> BlkSpriteFile(stream, BLK_ASYNC_DEFAULT, progressEvery, callback)
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
            val size = bytesBuffer.uInt16()
            bytesBuffer.close()
            size
        } catch (e: Exception) {
            null
        }
    }

    @JvmStatic
    fun getBodySpriteVariant(spriteFile: VirtualFile, defaultVariant: CaosVariant): CaosVariant {
        return runBlocking {
            getBodySpriteVariantSuspending(spriteFile, defaultVariant)
        }
    }

    private fun getBodySpriteVariantSuspending(
        spriteFile: VirtualFile,
        defaultVariant: CaosVariant
    ): CaosVariant {
        if (!BreedPartKey.isPartName(spriteFile.nameWithoutExtension)) {
            return defaultVariant
        }
        val extension = spriteFile.extension?.lowercase()
            ?: defaultVariant
        if (extension !in VALID_SPRITE_EXTENSIONS) {
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


class SpriteFileHolder(sprites: List<SpriteFile>, val fileName: String, private val bodyPart: Boolean = false) {


    val fileType by lazy {
        (PathUtil.getExtension(fileName) ?: fileName).uppercase().let {
            when (it) {
                "SPR" -> if (sprites.size > 0) SpriteType.SPR_SET else SpriteType.SPR
                "S16" -> S16
                "C16" -> C16
                "BLK" -> BLK
                else -> null
            }
        }
    }

    constructor(sprite: SpriteFile, fileName: String) : this(
        sprite.toListOf(), fileName
    )

    constructor(sprite: SpriteFile, fileName: String, bodyPart: Boolean) : this(
        sprite.toListOf(), fileName, bodyPart
    )

    private var mSpriteFile: List<SpriteFile> = sprites

    val bitmaps: List<List<Bitmap32>>
        get() = mSpriteFile.map {
            runBlocking {
                it.frames()
            }
        }

    val images: List<BufferedImage>
        get() = runBlocking {
            bitmaps[0].map(Bitmap32::toAwt)
        }

    val imageSets: List<List<BufferedImage>>
        get() = runBlocking {
            mSpriteFile.map { file ->
                file.frames().map {
                    it.toAwt()
                }
            }
        }

    val size: Int by lazy {
        runBlocking {
            mSpriteFile.firstOrNull()?.size() ?: 0
        }
    }

    fun getSize(spriteFileIndex: Int): Int? {
        return runBlocking { mSpriteFile.getOrNull(spriteFileIndex)?.size() }
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
    image = when (i) {
        in 0..3 -> get(i + 4)
        in 4..7 -> get(i - 4)
        else -> image
    }
    return image.flipHorizontal()
}