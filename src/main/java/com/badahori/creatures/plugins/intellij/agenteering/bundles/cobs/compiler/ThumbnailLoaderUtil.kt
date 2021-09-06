package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler

import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser
import com.badahori.creatures.plugins.intellij.agenteering.utils.likeAny
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

internal val loadThumbnail: (pictureUrl: String) -> BufferedImage? = { pictureUrl: String ->
    try {
        val parts = Caos2CobUtil.getSpriteFrameInformation(pictureUrl)
        val url = parts.first
        val frameNumber = parts.second
        LocalFileSystem.getInstance().findFileByPath(url)?.let { virtualFile ->
            if (virtualFile.extension likeAny SpriteParser.VALID_SPRITE_EXTENSIONS) {
                SpriteParser.parse(virtualFile).let {
                    it.getFrame(frameNumber)!!.decode()
                } ?: throw Caos2CobException("Failed to load Sprite frame after sprite de-compilation")
            } else {
                VfsUtil.virtualToIoFile(virtualFile).let { file ->
                    ImageIO.read(file)
                        ?: throw Caos2CobException("Failed to read image with ImageIO.read(file)")
                }
            }
        } ?: throw Caos2CobException("Failed to located virtual file for image: '$url'")
    } catch (e: Exception) {
        LOGGER.severe("PictureURL to image Exception: " + e.message)
        e.printStackTrace()
        throw e
    }
}