package com.badahori.creatures.plugins.intellij.agenteering.sprites.spr

import com.bedalton.io.bytes.MemoryByteStreamReader
import com.bedalton.creatures.sprite.parsers.readSprFrame
import com.bedalton.creatures.sprite.util.ColorPalette
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteCompiler
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import korlibs.image.awt.toAwt
import org.apache.commons.imaging.palette.SimplePalette
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.math.abs


@Suppress("MemberVisibilityCanBePrivate")
object SprCompiler : SpriteCompiler {
    private const val IMAGE_HEADER_SIZE = 8
    private const val HEADER_NUM_IMAGES_SIZE = 2
    private val blackIntArray = intArrayOf(0, 0, 0, 1)
    var black:Int = 0
    private val blackColors = arrayOf(0,243,244)
    private val colors: List<IntArray> by lazy {
        val pathToPalette = CaosFileUtil.PLUGIN_HOME_DIRECTORY?.findFileByRelativePath("support/palette.dta")
        if (pathToPalette == null) {
            LOGGER.severe("Path to palette is null")
            return@lazy (0..(255 * 3)).map { blackIntArray }
        }
        val bytes = pathToPalette.contentsToByteArray()
        (0 until 255).map {
            val start = it * 3
            val r = bytes[start] * 4
            val g = bytes[start + 1] * 4
            val b = bytes[start + 2] * 4
            intArrayOf(r, g, b)
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

        if (red > 245 && green > 245 && blue > 245) {
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

    override fun compileSprites(images: List<BufferedImage>): ByteArray {
        return compileSprites(images, null)
    }

    fun compileSprites(images:List<BufferedImage>, dither:Boolean?) : ByteArray {
        val imagesBytes = images.sumOf {
            it.width * it.height
        }
        val bufferSize = HEADER_NUM_IMAGES_SIZE + (images.size * IMAGE_HEADER_SIZE) + imagesBytes
        val buffer = ByteArrayOutputStream(bufferSize)
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
            writeCompiledSprite(image, buffer, dither)
        }
        //val byteArray = ByteArray(bufferSize)
        return buffer.toByteArray()
    }

    override fun writeCompiledSprite(image: BufferedImage, buffer: OutputStream) {
        writeCompiledSprite(image, buffer, null)
    }

    fun writeCompiledSprite(imageIn: BufferedImage, buffer: OutputStream, dither: Boolean?) {
        val image = if (dither == true)
            imageIn.ditherCopy(ditherPalette)
        else
            imageIn
        val width = image.width
        val height = image.height
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = image.getRGB(x, y)
                val alpha: Int = (pixel shr 24) and 0xFF
                if (alpha < 127) {
                    buffer.writeUInt8(black)
                } else {
                    val red: Int = pixel shr 16 and 0xff
                    val green: Int = pixel shr 8 and 0xff
                    val blue: Int = pixel shr 0 and 0xff
                    val color = if (red + green + blue == 0)
                        black
                    else {
                        val subFrom: Int = arrayOf(255 - alpha, red - 2, green - 2, blue - 2).minOrNull()!!
                        getColorIndex(red - subFrom, green - subFrom, blue - subFrom, false)
                    }
                    buffer.writeUInt8(color)
                }
            }
        }
    }

    @Suppress("unused")
    @JvmStatic
    @Throws
    fun previewCompilerResult(imageIn:BufferedImage, dither:Boolean) : BufferedImage {
        val bytes = ByteArrayOutputStream(imageIn.width * imageIn.height)
        writeCompiledSprite(imageIn, bytes, dither)
        return readSprFrame(
            bytesBuffer = MemoryByteStreamReader(bytes.toByteArray()),
            offset = 0L,
            width = imageIn.width,
            height = imageIn.height,
        )
            .withPalette(ColorPalette.C1TransparentBlack)
            .toAwt()
    }
}