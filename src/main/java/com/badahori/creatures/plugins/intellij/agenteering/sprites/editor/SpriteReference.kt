package com.badahori.creatures.plugins.intellij.agenteering.sprites.editor

import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser
import com.badahori.creatures.plugins.intellij.agenteering.utils.randomString
import com.badahori.creatures.plugins.intellij.agenteering.utils.rethrowAnyCancellationException
import com.badahori.creatures.plugins.intellij.agenteering.utils.toPngByteArray
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.awt.image.BufferedImage
import javax.imageio.ImageIO


/**
 * Holds data to pull a sprite out of the VFS
 */
data class SpriteReference(
    val name: String,
    val cacheDirectory: String,
    val imageCount: Int,
    val frames: Map<Int, SpriteReferenceFrame?>,
    val padding: Int
) {

    fun images(virtualFile: VirtualFile): List<BufferedImage?>? {
        return restore(virtualFile)
    }

    private fun restore(virtualFile: VirtualFile): List<BufferedImage?>? {
        return try {
            frames
                .keys
                .sorted()
                .map { i ->
                    getFrameImage(virtualFile, cacheDirectory, i, padding)
                }
        } catch (e: Exception) {
            e.rethrowAnyCancellationException()
            null
        }
    }

    fun delete(originalSpriteFile: VirtualFile) {
        frames
            .keys
            .sorted()
            .forEach { i ->
                getChildSpriteFile(originalSpriteFile, cacheDirectory, i, padding)
                    .delete(this)
            }
        systemImageCacheDirectory.findFileByRelativePath(cacheDirectory)
            ?.delete(this)
    }



    companion object {

        private val systemImageCacheDirectory: VirtualFile by lazy {
            CaosVirtualFileSystem.instance.getOrCreateRootChildDirectory("ImageCache")
        }

        /**
         * Caches images in a VFS, and returns a reference to the cache paths
         */
        fun create(originalSpriteFile: VirtualFile, frames: List<BufferedImage?>): SpriteReference {
            val name = originalSpriteFile.name
            val prefix = name  + '_'
            var thisSpriteDirectory = prefix + randomString(8)
            while (VfsUtil.findRelativeFile(systemImageCacheDirectory, *thisSpriteDirectory.split('/').toTypedArray()) != null) {
                thisSpriteDirectory = prefix + randomString(8)
            }
            val parent = systemImageCacheDirectory.createChildDirectory(null, thisSpriteDirectory)
            val padding = "${frames.size}".length
            val out = (frames.indices).associateWith map@{ i ->
                val fileName = getFileName(originalSpriteFile, i, padding)
                PathManager.getSystemPath()
                val frame = frames.getOrNull(i)
                    ?: return@map null
                val child = parent.createChildData(this, fileName)
                child.setBinaryContent(frame.toPngByteArray())
                SpriteReferenceFrame(
                    index = i,
                    width = frame.width,
                    height = frame.height
                )
            }
            return SpriteReference(
                originalSpriteFile.name,
                cacheDirectory = thisSpriteDirectory,
                imageCount = frames.size,
                frames = out,
                padding = padding
            )
        }

        private fun getFrameImage(originalSpriteFile: VirtualFile, path: String, index: Int, padding: Int): BufferedImage? {
            val imageFile = getChildSpriteFile(originalSpriteFile, path, index, padding)
            return ImageIO.read(imageFile.inputStream)
        }

        /**
         * Gets the Virtual file for a frame of this sprite
         */
        private fun getChildSpriteFile(originalSpriteFile: VirtualFile, path: String, index: Int, padding: Int): VirtualFile {
            val name = getFileName(originalSpriteFile, index, padding)
            val parent = systemImageCacheDirectory.findFileByRelativePath(path) as VirtualFile
            return parent.findFileByRelativePath(name)!!
        }

        /**
         * Builds the file name for a sprite frame
         */
        private fun getFileName(originalSpriteFile: VirtualFile, index: Int, padding: Int): String {
            return originalSpriteFile.name + ("$index".padStart(padding, ' ')) + ".png"
        }
    }

}


data class SpriteReferenceFrame(
    val index: Int,
    val width: Int,
    val height: Int
)