@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.badahori.creatures.plugins.intellij.agenteering.utils.flipHorizontal
import com.badahori.creatures.plugins.intellij.agenteering.utils.lowercase
import com.badahori.creatures.plugins.intellij.agenteering.utils.mapAsync
import com.badahori.creatures.plugins.intellij.agenteering.utils.rethrowAnyCancellationException
import com.badahori.creatures.plugins.intellij.agenteering.vfs.VirtualFileStreamReader
import com.bedalton.common.util.PathUtil
import com.bedalton.common.util.formatted
import com.bedalton.common.util.toListOf
import com.bedalton.creatures.sprite.parsers.*
import com.bedalton.creatures.sprite.util.SpriteType
import com.bedalton.creatures.sprite.util.SpriteType.*
import com.bedalton.io.bytes.ByteStreamReader
import com.bedalton.log.Log
import com.bedalton.log.iIf
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.serviceContainer.AlreadyDisposedException
import korlibs.image.awt.toAwt
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import java.awt.image.BufferedImage
import java.util.concurrent.CompletableFuture

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
                    e.rethrowAnyCancellationException()
                    Log.iIf(SPR_SHORT_DEBUG_LOGGING) { "Parsing SPR short: $fileName" }
                    try {
                        SprSetDecompiler.decompileSets(
                            stream,
                            keepBlack = false,
                            null,
                            null,
                        )!!
                    } catch (e2: Exception) {
                        e2.rethrowAnyCancellationException()
                        throw e
                    }
                }

            S16 -> S16SpriteFile(stream, false, null, progressEvery, callback)
                .toListOf()

            C16 -> C16SpriteFile(stream, false, null, progressEvery, callback)
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
            val size = bytesBuffer.uInt16()
            bytesBuffer.close()
            size
        } catch (e: Exception) {
            e.rethrowAnyCancellationException()
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
        defaultVariant: CaosVariant,
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
            e.rethrowAnyCancellationException()
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
        val theSprites = mSpriteFile
            ?: throw AlreadyDisposedException("Sprite holder is already disposed")
        (PathUtil.getExtension(fileName) ?: fileName).uppercase().let {
            when (it) {
                "SPR" -> if (theSprites.isNotEmpty()) {
                    SPR_SET
                } else {
                    SPR
                }

                "S16" -> S16
                "C16" -> C16
                "BLK" -> BLK
                else -> null
            }
        }
    }

    private var mSpriteFile: List<SpriteFile>? = sprites

    private val mAsyncFrames: List<Deferred<List<SpriteFrameAsync>>> by lazy {
        (mSpriteFile ?: throw AlreadyDisposedException("Sprite holder is already disposed"))
            .map {
                GlobalScope.async {
                    it.getAsyncFrames()
                }
            }
    }

    private val mBitmaps: List<Deferred<List<Bitmap32>>> by lazy {
        mAsyncFrames.map { deferred ->
            GlobalScope.async {
                try {
                    deferred.await().mapAsync {
                        try {
                            it.image()
                        } catch (e: Exception) {
                            e.rethrowAnyCancellationException()
                            Log.e("Failed to async frame to Bitmap32; ${e.formatted(true)}")
                            throw e
                        }
                    }
                } catch (e: Exception) {
                    e.rethrowAnyCancellationException()
                    Log.e("Failed to convert list of AsyncFrames to list of Bitmap32s; ${e.formatted(true)}")
                    throw e
                }
            }
        }
    }

    val bitmaps: CompletableFuture<List<List<Bitmap32>>> by lazy {
        GlobalScope.async {
            mBitmaps.map {
                it.await()
            }
        }.asCompletableFuture()
    }

    private val mImagesDeferred: Deferred<List<BufferedImage>> by lazy {
        GlobalScope.async {
            val images = mBitmaps.getOrNull(0)
                ?: throw IndexOutOfBoundsException("No bitmap lists found in bitmaps list of lists")
            val bitmaps = try {
                images.await()
            } catch (e: Exception) {
                e.rethrowAnyCancellationException()
                Log.e("Failed to convert deferred images list to actual image list; ${e.formatted(true)}")
                throw e
            }
            try {
                bitmaps.map(Bitmap32::toAwt)
            } catch (e: Exception) {
                e.rethrowAnyCancellationException()
                Log.e("Failed to convert korim.Bitmap32's into Java BufferedImage; ${e.formatted(true)}")
                throw e
            }
        }
    }

    val images: CompletableFuture<List<BufferedImage>> by lazy {
        GlobalScope.async {
            mImagesDeferred.await()
        }.asCompletableFuture()
    }

    val imageSets: List<List<BufferedImage>> by lazy {
        runBlocking {
            mBitmaps.mapAsync { images ->
                images.await().map(Bitmap32::toAwt)
            }
        }
    }

    val size: Int by lazy {
        if (mSpriteFile?.size == 0) {
            return@lazy 0
        }
        mSpriteFile
            ?.firstOrNull()
            ?.size()
            ?: throw AlreadyDisposedException("Sprite holder is already disposed")
    }


    constructor(sprite: SpriteFile, fileName: String) : this(
        sprite.toListOf(), fileName
    )

    constructor(sprite: SpriteFile, fileName: String, bodyPart: Boolean) : this(
        sprite.toListOf(), fileName, bodyPart
    )

    suspend fun imagesAsync(): List<BufferedImage> {
        return mImagesDeferred.await()
    }

    suspend fun bitmapsAsync(): List<List<Bitmap32>> {
        return mBitmaps.awaitAll()
    }

    fun getSize(spriteFileIndex: Int): Int? {
        return runBlocking {
            (mSpriteFile ?: throw AlreadyDisposedException("Sprite holder is already disposed"))
                .getOrNull(spriteFileIndex)?.size()
        }
    }

    operator fun get(frame: Int): BufferedImage? {
        return runBlocking {
            val frames = mAsyncFrames[0].await()
            if (bodyPart) {
                frames.getBodyImageAt(frame)
            } else {
                frames.getOrNull(frame)?.toAwt()
            }
        }
    }

    operator fun get(spriteFileIndex: Int, frame: Int): BufferedImage? {
        return imageSets.getOrNull(spriteFileIndex)?.getOrNull(frame)
    }


    fun closeSpriteFiles() {
        mSpriteFile?.map { it.close() }
        mSpriteFile = null
    }


}

internal val transparentBlack = RGBA(0, 0, 0, 0)
//internal val solidBlack = RGBA(0,0,0)


private fun List<SpriteFrameAsync>.getBodyImageAt(i: Int): BufferedImage {
    var image = get(i).toAwt()
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
        in 0..3 -> get(i + 4).image().toAwt()
        in 4..7 -> get(i - 4).image().toAwt()
        else -> image
    }
    return image.flipHorizontal()
}


fun SpriteFrameAsync.toAwt(): BufferedImage {
    return image().toAwt()
}