package com.badahori.creatures.plugins.intellij.agenteering.sprites.c16

import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.ColorEncoding
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteCompiler
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteCompilerException
import com.badahori.creatures.plugins.intellij.agenteering.utils.writeUInt16
import com.badahori.creatures.plugins.intellij.agenteering.utils.writeUint32
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.OutputStream

@Suppress("MemberVisibilityCanBePrivate")
object C16Compiler : SpriteCompiler {

    private const val SPRITE_HEADER_SIZE = 6 // 4 = format, 2 = Num images
    private const val IMAGE_HEADER_SIZE = 8 // 4 = offset to start of data, 2 = width, 2 = height
    private val DEFAULT_ENCODING = ColorEncoding.X_565

    override fun compileSprites(images: List<BufferedImage>): ByteArray {
        return compileSprites(images, DEFAULT_ENCODING)
    }

    fun compileSprites(images: List<BufferedImage>, colorEncoding: ColorEncoding): ByteArray {
        val frames = images.map {image ->
            getCompiledData(image, colorEncoding)
        }
        var imageDataOffset = SPRITE_HEADER_SIZE + (images.size * IMAGE_HEADER_SIZE) + frames.sumBy {
            (it.height - 1) * 4
        }
        val bufferSize =  imageDataOffset
        val buffer = ByteArrayOutputStream(bufferSize)
        buffer.writeUint32(colorEncoding.marker + 1)
        buffer.writeUInt16(images.size)
        for (frame in frames) {
            imageDataOffset = frame.writeHeader(buffer, imageDataOffset)
        }
        assert (buffer.size() == imageDataOffset) { "Image offset may be inaccurate in C16 file"}
        for (frame in frames) {
            frame.lines.forEach { runs ->
                runs.forEach { run ->
                    if (run is Run.Transparent) {
                        buffer.writeUInt16(0 or (run.pixelCount shl 1))
                    } else if (run is Run.Color) {
                        buffer.writeUInt16(1 or (run.pixelCount shl 1))
                        run.colors.forEach { color ->
                            buffer.writeUInt16(color)
                        }
                    }
                }
            }
        }
        return buffer.toByteArray()
    }


    override fun writeCompiledSprite(image: BufferedImage, buffer: OutputStream) {
        TODO("Cannot write C16 sprite frames into buffer directly")
    }

    private fun getCompiledData(image: BufferedImage, colorEncoding: ColorEncoding) : C16CompilerData {
        val width = image.width
        val height = image.height
        val rgbToInt = colorEncoding.toInt
        val lines = mutableListOf<List<Run>>()
        for (y in 0 until height) {
            val runs = mutableListOf<Run>()
            var run:Run? = null
            for (x in 0 until width) {
                val pixel = image.getRGB(x, y)
                val alpha: Int = (pixel shr 24) and 0xFF
                if (alpha < 127) {
                    if (run == null || run !is Run.Transparent) {
                        run = Run.Transparent(1).apply {
                            runs.add(this)
                        }
                    } else {
                        run.pixelCount++
                    }
                } else {
                    val red: Int = pixel shr 16 and 0xff
                    val green: Int = pixel shr 8 and 0xff
                    val blue: Int = pixel shr 0 and 0xff
                    if (red + green + blue == 0) {
                        if (run == null || run !is Run.Transparent) {
                            run = Run.Transparent(1).apply {
                                runs.add(this)
                            }
                        } else {
                            run.pixelCount++
                        }
                    } else {
                        if (run == null || run !is Run.Color) {
                            run = Run.Color().apply {
                                runs.add(this)
                            }
                        }
                        val subFrom: Int = arrayOf(255 - alpha, red - 2, green - 2, blue - 2).minOrNull()!!
                        val color = rgbToInt(red - subFrom, green - subFrom, blue - subFrom)
                        run.colors.add(color)
                    }
                }
            }
            lines.add(runs)
        }
        return C16CompilerData(width = image.width, height = width, lines = lines)
    }

}

private data class C16CompilerData(val width:Int, val height:Int, val lines:List<List<Run>>) {

    val size:Int by lazy {
        lines.sumBy { run -> run.sumBy { 2 + it.byteOffset } }
    }

    fun writeHeader(outputStream: OutputStream, offset:Int) : Int {
        outputStream.writeUint32(offset)
        outputStream.writeUInt16(width)
        outputStream.writeUInt16(height)
        if (width < 1 || height < 1)
            throw SpriteCompilerException("Width and height must be non-zero")
        if (lines.size != height) {
            throw SpriteCompilerException("Line data and height mismatch. Expected ${height} line data objects. Found: ${lines.size}")
        }
        val byteOffsets = lines.map { run -> run.sumBy { 2 + it.byteOffset }}
        var lineOffset = offset + byteOffsets.first()
        byteOffsets.drop(1).forEach {
            outputStream.write(lineOffset)
            lineOffset += it
        }
        return lineOffset
    }
}

private sealed class Run {
    abstract val byteOffset:Int
    abstract val pixelCount:Int
    data class Transparent(override var pixelCount:Int) : Run() {
        override val byteOffset = 2
    }
    data class Color(val colors:MutableList<Int> = mutableListOf()) : Run() {
        override val byteOffset by lazy { colors.size * 2 }
        override val pixelCount: Int
            get() = colors.size
    }
}