package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler

import bedalton.creatures.sprite.parsers.SpriteParser
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser.VALID_SPRITE_EXTENSIONS
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.getAllBytes
import com.badahori.creatures.plugins.intellij.agenteering.utils.likeAny
import com.bedalton.io.bytes.MemoryByteStreamReader
import com.intellij.openapi.vfs.LocalFileSystem
import korlibs.image.bitmap.Bitmap32
import korlibs.image.format.BMP
import korlibs.image.format.GIF
import korlibs.image.format.PNG
import kotlinx.coroutines.runBlocking

internal val loadThumbnail: (pictureUrl: String) -> Bitmap32? = { pictureUrl: String ->
    try {
        val parts = Caos2CobUtil.getSpriteFrameInformation(pictureUrl)
        val url = parts.first
        val frameNumber = parts.second
        LocalFileSystem.getInstance().findFileByPath(url)?.let { virtualFile ->
            val bytes = virtualFile.getAllBytes()
            if (virtualFile.extension likeAny VALID_SPRITE_EXTENSIONS) {
                runBlocking {
                    SpriteParser.parse(virtualFile.name, MemoryByteStreamReader(bytes), keepBlack = false)
                        .getOrNull(frameNumber)
                } ?: throw Caos2CobException("Failed to load Sprite frame after sprite de-compilation")
            } else {
                when (virtualFile.extension?.lowercase()) {
                    "png" -> PNG.decode(bytes).toBMP32IfRequired()
                    "gif" -> GIF.decode(bytes).toBMP32IfRequired()
                    "bmp" -> BMP.decode(bytes).toBMP32IfRequired()
                    else -> throw Caos2CobException("Failed to read image with ImageIO.read(file)")
                }
            }

        } ?: throw Caos2CobException("Failed to located virtual file for image: '$url'")
    } catch (e: Exception) {
        LOGGER.severe("PictureURL to image Exception: " + e.message)
        e.printStackTrace()
        throw e
    }
}