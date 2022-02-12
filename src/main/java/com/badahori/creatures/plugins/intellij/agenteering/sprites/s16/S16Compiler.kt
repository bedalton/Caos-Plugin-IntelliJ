package com.badahori.creatures.plugins.intellij.agenteering.sprites.s16

import bedalton.creatures.sprite.util.ColorEncoding
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteCompiler
import com.badahori.creatures.plugins.intellij.agenteering.utils.writeUInt16
import com.badahori.creatures.plugins.intellij.agenteering.utils.writeUint32
import java.io.ByteArrayOutputStream
import java.awt.image.BufferedImage
import java.io.OutputStream

@Suppress("MemberVisibilityCanBePrivate")
object S16Compiler : SpriteCompiler {

    private val SPRITE_HEADER_SIZE = 6 // 4 = format, 2 = Num images
    private val IMAGE_HEADER_SIZE = 8 // 4 = offset to start of data, 2 = width, 2 = height
    private val DEFAULT_ENCODING = ColorEncoding.X_565

    private const val black = 0

    override fun compileSprites(images: List<BufferedImage>): ByteArray {
        return compileSprites(images, DEFAULT_ENCODING)
    }

    fun compileSprites(images: List<BufferedImage>, colorEncoding: ColorEncoding): ByteArray {
        val imagesBytes = images.sumOf {
            it.width * it.height
        }
        var imageOffset = SPRITE_HEADER_SIZE + (images.size * IMAGE_HEADER_SIZE)
        val bufferSize =  imageOffset + imagesBytes
        val buffer = ByteArrayOutputStream(bufferSize)
        buffer.writeUint32(colorEncoding.marker)
        buffer.writeUInt16(images.size)
        for (image in images) {
            buffer.writeUint32(imageOffset.toLong())
            val width = image.width
            val height = image.height
            buffer.writeUInt16(width)
            buffer.writeUInt16(height)
            imageOffset += width * height
        }
        assert (buffer.size() == imageOffset) { "Image offset may be inaccurate in S16 file"}
        for (image in images) {
            writeCompiledSprite(image, buffer, colorEncoding)
        }
        return buffer.toByteArray()
    }


    override fun writeCompiledSprite(image: BufferedImage, buffer: OutputStream) {
        writeCompiledSprite(image, buffer, DEFAULT_ENCODING)
    }

    fun writeCompiledSprite(image: BufferedImage, buffer: OutputStream, colorEncoding: ColorEncoding) {
        val width = image.width
        val height = image.height
        val rgbToInt = colorEncoding.toInt
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = image.getRGB(x, y)
                val alpha: Int = (pixel shr 24) and 0xFF
                if (alpha < 127) {
                    buffer.writeUInt16(black)
                } else {
                    val red: Int = pixel shr 16 and 0xff
                    val green: Int = pixel shr 8 and 0xff
                    val blue: Int = pixel and 0xff
                    if (red + green + blue == 0) {
                        buffer.writeUInt16(black)
                    } else {
                        val subFrom: Int = 0//arrayOf(((255 - alpha) * 0.7).toInt(), red - 2, green - 2, blue - 2).min()!!
                        val color = rgbToInt(red - subFrom, green - subFrom, blue - subFrom)
                        buffer.writeUInt16(color)
                    }
                }
            }
        }
    }

}