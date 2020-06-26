package com.openc2e.plugins.intellij.caos.sprites.spr

import com.intellij.openapi.vfs.VirtualFile
import com.openc2e.plugins.intellij.caos.psi.util.LOGGER
import com.openc2e.plugins.intellij.caos.utils.uByte
import com.openc2e.plugins.intellij.caos.utils.uInt
import com.openc2e.plugins.intellij.caos.utils.uShort
import java.awt.Color
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.nio.ByteBuffer

/**
 * Parses Creatures 1 SPR sprite files
 * Based on python code by Shee's Lost Knowledge
 *
 * @see http://sheeslostknowledge.blogspot.com/2014/11/parsing-creatures-1-spr-files.html
 */
object SprParser {

    internal fun parseSprite(file:VirtualFile) : List<Image?> {
        val rawBytes = file.contentsToByteArray()
        val numRawBytes = rawBytes.size
        val bytesBuffer = ByteBuffer.wrap(rawBytes)
        val numImages = bytesBuffer.uShort
        val rawData = (0 until numImages).map {
            val offsetForData = bytesBuffer.uInt
            if (offsetForData < 0) {
                LOGGER.info("OffsetForData returned negative number. $offsetForData")
                return@map null
            }
            val width = bytesBuffer.uShort
            val height = bytesBuffer.uShort
            if (width < 1 || height < 1) {
                LOGGER.info("OffsetForData Width Height invalid. <${width}x${height}>")
                return@map null
            }
            val numBytes = width * height
            val endByte = (offsetForData + (width * height) + 1)
            if (numRawBytes < endByte || endByte < 0) {
                LOGGER.info("Invalid byte range requested. Total Bytes: ${numRawBytes}; Offset: $offsetForData, Width:$width, Height:$height; EndBye: ${(offsetForData + (width * height) + 1)}")
                return@map null
            }

            val bytes = ByteArray(numBytes)
            bytesBuffer.get(bytes, offsetForData.toInt(), numBytes)
            SpriteData(width = width.toInt(), height = height.toInt(), bytes = bytes)
        }
        return rawData.map {
            LOGGER.info("ImageData: $it")
            if (it == null)
                null
            else
                parseImage(it.width, it.height, it.bytes)
        }
    }

    private fun parseImage(width:Int, height:Int, bytes:ByteArray) : BufferedImage? {
        if (width * height != bytes.size) {
            LOGGER.info("Failed to parse image. Bytes does not equal width * height. Expected: <${width * height}>; Found: <${bytes.size}>")
            return null
        }
        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        (0..height).map {y ->
            (0..width).map { x ->
                val colorIndex = bytes[x*y].toInt()
                bufferedImage.setRGB(x,y,colors[colorIndex])
            }
        }
        return bufferedImage
    }
    private val colors:List<Int> by lazy {
        val pathToPalette = javaClass.getResource("support/palette.dta").toExternalForm()
        val bytes = File(pathToPalette).readBytes()
        (0..256).map {
            val start = it * 3
            val r = bytes[start] * 4
            val g = bytes[start+1] * 4
            val b = bytes[start+2] * 4
            val alpha = if (r == 0 && g == 0 && b == 0) 0 else 1
            Color(r,g,b,alpha).rgb
        }
    }
}

data class SpriteData(val width:Int, val height:Int, val bytes:ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpriteData) return false

        if (width != other.width) return false
        if (height != other.height) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width.hashCode()
        result = 31 * result + height
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}