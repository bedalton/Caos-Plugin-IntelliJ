package com.badahori.creatures.plugins.intellij.agenteering.sprites.spr

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.sprites.ditherCopy
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.util.io.toByteArray
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream
import org.apache.commons.imaging.palette.Dithering
import org.apache.commons.imaging.palette.Palette
import org.apache.commons.imaging.palette.SimplePalette
import java.awt.Color
import java.awt.color.ColorSpace
import java.awt.image.BufferedImage
import java.io.OutputStream
import java.nio.ByteBuffer
import kotlin.math.abs


object SprCompiler {
    private const val IMAGE_HEADER_SIZE = 8
    private const val HEADER_NUM_IMAGES_SIZE = 2
    val blackIntArray = intArrayOf(0, 0, 0, 1)
    var black:Int = 0
    val blackColors = arrayOf(0,243,244,245)
    val blackColorRGB = Color(0,0,0).rgb
    private val colors: List<IntArray> by lazy {
        val pathToPalette = CaosFileUtil.PLUGIN_HOME_DIRECTORY?.findFileByRelativePath("support/palette.dta")
        if (pathToPalette == null) {
            LOGGER.severe("Path to palette is null")
            return@lazy (0..(255 * 3)).map { blackIntArray }
        }
        val bytes = pathToPalette.contentsToByteArray()
        (0..255).map {
            val start = it * 3
            val r = bytes[start] * 4
            val g = bytes[start + 1] * 4
            val b = bytes[start + 2] * 4
            intArrayOf(r, g, b)
        }
    }

    private val colorRGBs:List<Int> by lazy {
        colors.map {
            Color(it[0],it[1],it[2]).rgb
        }
    }

    private val ditherPalette by lazy {
        val colorsAsInt:List<Int> = colors.map {rgb ->
            var out: Int = rgb[0]
            out = (out shl 8) + rgb[1]
            out = (out shl 8) + rgb[2]
            out
        }
        SimplePalette(colorsAsInt.toIntArray())
    }


    private fun compareColors(colorA: IntArray, red:Int, green:Int, blue:Int): Int {
        return abs(colorA[0] - red) + abs(colorA[1] - green) + abs(colorA[2] - blue)
    }

    private fun getColorIndex(red:Int, green:Int, blue:Int, allowBlack:Boolean) : Int {
        var selectedColor = black
        var deviation:Int = Int.MAX_VALUE

        if (red > 250 && green == 250 && blue > 250) {
            return 255
        }
        for(i in 0 .. colors.lastIndex) {
            val currentDeviation = compareColors(colors[i], red, green, blue)
            if (currentDeviation < deviation && (allowBlack || i !in blackColors)) {
                deviation = currentDeviation
                selectedColor = i
            }
        }
        return selectedColor
    }

    fun compileSprites(images:List<BufferedImage>, dither:Boolean = false) : ByteArray {
        val imagesBytes = images.sumBy {
            it.width * it.height
        }
        val bufferSize = HEADER_NUM_IMAGES_SIZE + (images.size * IMAGE_HEADER_SIZE) + imagesBytes
        val buffer = ByteOutputStream(bufferSize)
        var imageOffset = HEADER_NUM_IMAGES_SIZE + (images.size * IMAGE_HEADER_SIZE)
        buffer.writeUInt16(images.size)
        for (image in images) {
            buffer.writeUint32(imageOffset.toLong())
            val width = image.width
            val height = image.height
            buffer.writeUInt16(width)
            buffer.writeUInt16(height)
            imageOffset += width * height
        }
        for (image in images) {
            compileSprite(image, buffer, dither)
        }
        //val byteArray = ByteArray(bufferSize)
        return buffer.bytes
    }

    fun compileSprite(imageIn: BufferedImage, buffer: OutputStream, dither: Boolean) {
        val image = if (dither)
            imageIn.ditherCopy(ditherPalette)
        else
            imageIn
        val width = image.width
        val height = image.height
        val alphaValues = mutableSetOf<Int>()
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = image.getRGB(x, y)
                val alpha: Int = (pixel shr 24) and 0xFF
                alphaValues.add(alpha)
                if (alpha < 127) {
                    buffer.writeUInt8(black)
                } else {
                    val red: Int = pixel shr 16 and 0xff
                    val green: Int = pixel shr 8 and 0xff
                    val blue: Int = pixel shr 0 and 0xff
                    val color = if (red + green + blue == 0)
                        black
                    else {
                        val subFrom: Int = arrayOf(255 - alpha, red - 2, green - 2, blue - 2).min()!!
                        getColorIndex(red - subFrom, green - subFrom, blue - subFrom, false)
                    }
                    buffer.writeUInt8(color)
                }
            }
        }
        LOGGER.info("Alpha values: $alphaValues")
    }

    @JvmStatic
    fun compileForPreview(imageIn:BufferedImage, dither:Boolean = false) : BufferedImage {
        val image = if (dither)
            imageIn.ditherCopy(ditherPalette)
        else
            imageIn
        val outImage = BufferedImage(image.width, image.height, ColorSpace.TYPE_RGB)
        val width = image.width
        val height = image.height
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = image.getRGB(x, y)
                val alpha: Int = pixel shr 24 and 0xff
                if (alpha > 1 || alpha < 0) {
                    outImage.setRGB(x,y, blackColorRGB)
                } else {
                    val red: Int = pixel shr 16 and 0xff
                    val green: Int = pixel shr 8 and 0xff
                    val blue: Int = pixel shr 0 and 0xff
                    val color = getColorIndex(red, green, blue, false)
                    outImage.setRGB(x,y, colorRGBs[color])
                }
            }
        }
        return outImage
    }
}