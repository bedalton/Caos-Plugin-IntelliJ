package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler

import bedalton.creatures.bytes.MemoryByteStreamReader
import bedalton.creatures.sprite.parsers.SpriteParser
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser.VALID_SPRITE_EXTENSIONS
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.getAllBytes
import com.badahori.creatures.plugins.intellij.agenteering.utils.likeAny
import com.intellij.openapi.vfs.LocalFileSystem
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.format.BMP
import com.soywiz.korim.format.GIF
import com.soywiz.korim.format.PNG

internal val loadThumbnail: (pictureUrl: String) -> Bitmap32? = { pictureUrl: String ->
    try {
        val parts = Caos2CobUtil.getSpriteFrameInformation(pictureUrl)
        val url = parts.first
        val frameNumber = parts.second
        LocalFileSystem.getInstance().findFileByPath(url)?.let { virtualFile ->
            val bytes = virtualFile.getAllBytes()
            if (virtualFile.extension likeAny VALID_SPRITE_EXTENSIONS) {
                SpriteParser.parse(virtualFile.name, MemoryByteStreamReader(bytes), keepBlack = false)
                    .getOrNull(frameNumber)
                    ?: throw Caos2CobException("Failed to load Sprite frame after sprite de-compilation")
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