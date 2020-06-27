package com.openc2e.plugins.intellij.caos.sprites.spr

import com.intellij.openapi.vfs.VirtualFile
import com.openc2e.plugins.intellij.caos.psi.util.LOGGER
import com.openc2e.plugins.intellij.caos.utils.*
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
        val numImages = bytesBuffer.uInt16BE
        LOGGER.info("Number of Images: <$numImages>")
        val rawData = (0 until numImages).map {
            val offsetForData = bytesBuffer.uInt32BE
            if (offsetForData < 0) {
                LOGGER.info("OffsetForData returned negative number. $offsetForData")
                return@map null
            }
            val width = bytesBuffer.uInt16BE
            val height = bytesBuffer.uInt16BE
            if (width < 1 || height < 1) {
                LOGGER.info("OffsetForData Width Height invalid. <${width}x${height}>")
                return@map null
            }
            val numBytes = width * height
            val endByte = (offsetForData + (width * height))
            if (numRawBytes < endByte || endByte < 0) {
                LOGGER.info("Invalid byte range requested. Total Bytes: ${numRawBytes}; Offset: $offsetForData, Width:$width, Height:$height; EndByte: $endByte}")
                return@map null
            }
            SpriteData(offset = offsetForData, width = width, height = height)
        }
        return rawData.map { imageData ->
            LOGGER.info("ImageData: $imageData")
            if (imageData == null)
                null
            else {
                bytesBuffer.position(imageData.offset.toInt())
                val pixels = (0 until (imageData.width * imageData.height)).map pixels@{_ ->
                    bytesBuffer.uInt8.let {
                        if (it < 0 || it > 256) {
                            LOGGER.info("Color value '$it' is invalid")
                            return@pixels 0
                        }
                        colors[it]
                    }
                }
                parseImage(imageData.width, imageData.height, pixels)
            }
        }
    }

    private fun parseImage(width:Int, height:Int, pixels:List<Int>) : BufferedImage? {
        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val alphaRaster = bufferedImage.alphaRaster
        val black = Color(0,0,0).rgb
        val solid = intArrayOf(255,255,255,255)
        val transparent = intArrayOf(0,0,0,0)
        (0 until height).map {y ->
            val base = y * width
            (0 until width).mapIndexed { i, x ->
                val rgb = pixels[base+i]
                bufferedImage.setRGB(x,y,rgb)
                val alpha = if (rgb == black) transparent else solid
                alphaRaster.setPixel(x,y,alpha)
            }
        }
        return bufferedImage
    }
    private val colors:List<Int> by lazy {
        val pathToPalette = CaosFileUtil.PLUGIN_HOME_DIRECTORY?.findFileByRelativePath("support/palette.dta")
        if (pathToPalette == null) {
            LOGGER.info("Path to palette is null")
            return@lazy (0..(255*3)).map { 128 }
        }
        val bytes = pathToPalette.contentsToByteArray()
        (0..255).map {
            val start = it * 3
            val r = bytes[start] * 4
            val g = bytes[start+1] * 4
            val b = bytes[start+2] * 4
            Color(r,g,b).rgb
        }
    }
}

data class SpriteData(val offset:Long, val width:Int, val height:Int)