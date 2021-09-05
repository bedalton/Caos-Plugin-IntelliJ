package com.badahori.creatures.plugins.intellij.agenteering.sprites.c16

import bedalton.creatures.bytes.ByteStreamWriter
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.ColorEncoding
import com.intellij.util.io.toByteArray
import java.awt.image.BufferedImage
import java.awt.image.Raster
import kotlin.math.ceil

@Suppress("MemberVisibilityCanBePrivate")
object BlkCompiler {

    private val SPRITE_HEADER_SIZE = 10 // 4 = format, 2 = Num images
    private val IMAGE_HEADER_SIZE = 8 // 4 = offset to start of data, 2 = width, 2 = height
    private val DEFAULT_ENCODING = ColorEncoding.X_565

    private const val black = 0

    fun compileSprites(image: BufferedImage, colorEncoding: ColorEncoding): ByteArray {

        val imagesBytes = image.width * image.height * 2
        val blocksWide = ceil(image.width / 128.0).toInt()
        val blocksHigh = ceil(image.height / 128.0).toInt()
        val numBlocks = blocksWide * blocksHigh
        var imageOffset = SPRITE_HEADER_SIZE + (numBlocks * IMAGE_HEADER_SIZE)
        val bufferSize =  imageOffset + imagesBytes
        val buffer = ByteStreamWriter(bufferSize)
        buffer.writeUInt32(1 - colorEncoding.marker)
        buffer.writeUInt16(blocksWide)
        buffer.writeUInt16(blocksHigh)
        buffer.writeUInt16(numBlocks)

        repeat (numBlocks) {
            buffer.writeUInt32(imageOffset)
            buffer.writeUInt16(128)
            buffer.writeUInt16(128)
            imageOffset += IMAGE_HEADER_SIZE
        }
        val trueWidth = image.width
        val trueHeight = image.height
        repeat (blocksHigh) { y ->
            repeat (blocksWide) { x ->
                writeCompiledSprite(image.raster, trueWidth, trueHeight, x, y, buffer, colorEncoding)
            }
        }
        return buffer.byteArray
    }

    private fun writeCompiledSprite(
        image: Raster,
        trueWidth:Int,
        trueHeight:Int,
        cellX:Int,
        cellY:Int,
        buffer: ByteStreamWriter,
        colorEncoding: ColorEncoding
    ) : Int {
        val offsetY = cellY * 128
        val offsetX = cellX * 128
        val rgbToInt = colorEncoding.toInt
        val base = buffer.position
        val rgb = IntArray(3)
        for (y in offsetY .. offsetY + 128) {
            for (x in offsetX .. offsetX + 128) {
                if (y >= trueHeight || x >= trueWidth) {
                    black
                } else {
                    val pixel = image.getPixel(x, y, rgb)
                    val alpha: Int = pixel[3]
                    if (alpha < 127) {
                        buffer.writeUInt16(black)
                    } else {
                        val red: Int = pixel[0]
                        val green: Int = pixel[1]
                        val blue: Int = pixel[2]
                        if (red + green + blue == 0) {
                            buffer.writeUInt16(black)
                        } else {
                            val subFrom = 0//arrayOf(((255 - alpha) * 0.7).toInt(), red - 2, green - 2, blue - 2).min()!!
                            val color = rgbToInt(red - subFrom, green - subFrom, blue - subFrom)
                            buffer.writeUInt16(color)
                        }
                    }
                }
            }
        }
        val written = buffer.position - base
        assert (written == (128 * 128 * 2)) { "Bytes written is incorrect. Expected: ${128 * 128 * 2}; Actual: $written" }
        return written
    }

}
