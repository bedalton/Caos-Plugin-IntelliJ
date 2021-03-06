package com.badahori.creatures.plugins.intellij.agenteering.sprites.spr

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteFile
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteFrame
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParserException
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteType
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.vfs.VirtualFile
import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.ByteBuffer


object SprParser {
    internal val colors: List<Int> by lazy {
        val pathToPalette = CaosFileUtil.PLUGIN_HOME_DIRECTORY?.findFileByRelativePath("support/palette.dta")

        if (pathToPalette == null) {
            LOGGER.severe("Path to palette is null")
            return@lazy (0..(255 * 3)).map { 128 }
        }
        val bytes = pathToPalette.contentsToByteArray()
        (0..255).map {
            val start = it * 3
            val r = bytes[start] * 4
            val g = bytes[start + 1] * 4
            val b = bytes[start + 2] * 4
            Color(r, g, b).rgb
        }
    }
}


/**
 * Parses Creatures 1 SPR sprite files
 * Based on python code by Shee's Lost Knowledge
 *a
 * @url http://sheeslostknowledge.blogspot.com/2014/11/parsing-creatures-1-spr-files.html
 */

class SprSpriteFile @Throws constructor(file: VirtualFile) : SpriteFile<SprSpriteFrame>(SpriteType.Spr) {

    init {
        val rawBytes = file.contentsToByteArray()
        val numRawBytes = rawBytes.size
        val bytesBuffer = ByteBuffer.wrap(rawBytes).littleEndian()
        val numImages = bytesBuffer.uInt16
        mFrames =  (0 until numImages).map {
            val offsetForData = bytesBuffer.uInt32
            if (offsetForData < 0) {
                throw SpriteParserException("OffsetForData returned negative number. $offsetForData")
            }
            val width = bytesBuffer.uInt16
            val height = bytesBuffer.uInt16
            if (width < 1 || height < 1) {
                throw SpriteParserException("OffsetForData Width Height invalid for frame $it. Found size: <${width}x${height}>")
            }
            val numBytes = width * height
            val endByte = (offsetForData + numBytes)
            if (numRawBytes < endByte || endByte < 0) {
                throw SpriteParserException("Invalid byte range requested. Total Bytes: ${numRawBytes}; Offset: $offsetForData, Width:$width, Height:$height; EndByte: $endByte}")
            }
            SprSpriteFrame(bytes = bytesBuffer, offset = offsetForData, width = width, height = height)
        }
    }

    override fun compile(): ByteArray {
        TODO("Not yet implemented")
    }
}

class SprSpriteFrame private constructor(width: Int, height: Int) : SpriteFrame<SprSpriteFrame>(width, height, SpriteType.Spr) {

    private var _image:BufferedImage? = null
    private val lock:Any = Object()
    private lateinit var getImage:()->BufferedImage?

    constructor(bytes: ByteBuffer, offset: Long, width: Int, height: Int) : this(width, height){
        getImage = {
            decode(bytes, offset)
        }
    }

    constructor(image: BufferedImage) : this(image.width, image.height) {
        getImage = { image }
    }

    override fun decode() : BufferedImage? {
        var image = _image
        if (_image == null) {
            synchronized(lock) {
                if (_image == null) {
                    image = getImage()?.apply {
                        _image = image
                    }
                }
            }
        }
        return image
    }

    private fun decode(bytes: ByteBuffer, offset: Long) : BufferedImage {
        val bytesBuffer = bytes.duplicate()
        bytesBuffer.position(offset.toInt())
        val pixels = (0 until (width * height)).map pixels@{ _ ->
            bytesBuffer.uInt8.let {
                if (it < 0 || it > 256) {
                    throw SpriteParserException("Color value '$it' is invalid")
                }
                if (it in 243..245)
                    -1
                else
                    SprParser.colors[it]
            }
        }
        return decode(pixels)
    }

    private fun decode(pixels: List<Int>) : BufferedImage {
        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val alphaRaster = bufferedImage.alphaRaster
        val black = Color(0, 0, 0).rgb
        val solid = intArrayOf(255, 255, 255, 255)
        val transparent = intArrayOf(0, 0, 0, 0)
        (0 until height).map { y ->
            val base = y * width
            (0 until width).mapIndexed { i, x ->
                val rgb = pixels[base + i]
                if (rgb == -1) {
                    bufferedImage.setRGB(x, y, black)
                    alphaRaster.setPixel(x, y, solid)
                } else {
                    bufferedImage.setRGB(x, y, rgb)
                    val alpha = if (rgb == black) transparent else solid
                    alphaRaster.setPixel(x, y, alpha)
                }
            }
        }
        return bufferedImage
    }

    override fun encode(): ByteArray {
        TODO("Implement SPR frame encoding")
    }

    override fun copy(): SprSpriteFrame {
        val myGetImage = this.getImage
        return SprSpriteFrame(width, height).apply {
            getImage = myGetImage
        }
    }

}