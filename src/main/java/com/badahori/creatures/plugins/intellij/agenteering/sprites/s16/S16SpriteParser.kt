package com.badahori.creatures.plugins.intellij.agenteering.sprites.s16

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.*
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteColorUtil.solid
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteColorUtil.transparent
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.vfs.VirtualFile
import java.awt.image.BufferedImage
import java.nio.ByteBuffer

/**
 * Parses Creatures S16 sprite file
 * Based on c2ephp by telyn
 * @url https://github.com/telyn/c2ephp
 */
class S16SpriteFile(rawBytes:ByteArray) : SpriteFile<S16SpriteFrame>(SpriteType.S16) {

    constructor(file: VirtualFile) : this(file.contentsToByteArray())

    init {
        val bytesBuffer = ByteBuffer.wrap(rawBytes).littleEndian()
        val encoding = when (val buffer = bytesBuffer.uInt32) {
            1L -> ColorEncoding.X_565
            0L -> ColorEncoding.X_555
            else -> throw Exception("File encoding not recognized. ('$buffer')")
        }
        val numImages = bytesBuffer.uInt16
        _frames = (0 until numImages).map {
            val offsetForData = bytesBuffer.uInt32
            val width = bytesBuffer.uInt16
            val height = bytesBuffer.uInt16
            S16SpriteFrame(
                    bytes = bytesBuffer,
                    offset = offsetForData,
                    width = width,
                    height = height,
                    encoding = encoding
            )
        }
    }

    override fun compile(): ByteArray {
        TODO("Not yet implemented")
    }
}

class S16SpriteFrame private constructor(width: Int, height: Int, private val encoding: ColorEncoding) : SpriteFrame<S16SpriteFrame>(width, height, SpriteType.S16) {

    private lateinit var getImage: () -> BufferedImage?

    constructor(bytes: ByteBuffer, offset: Long, width: Int, height: Int, encoding: ColorEncoding) : this(width, height, encoding) {
        getImage = {
            decode(bytes, offset)
        }
    }

    constructor(image: BufferedImage, encoding: ColorEncoding = ColorEncoding.X_565) : this(image.width, image.height, encoding) {
        getImage = { image }
    }

    override fun decode(): BufferedImage? {
        if (this::getImage.isInitialized)
            return getImage()
        return null
    }

    private fun decode(bytes: ByteBuffer, offset: Long): BufferedImage? {
        val bytesBuffer = bytes.duplicate()
        bytesBuffer.position(offset.toInt())
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bytesBuffer.uInt16
                val color = SpriteColorUtil.getColor(pixel, encoding)
                if (color[0] == 0 && color[1] == 0 && color[2] == 0)
                    image.alphaRaster.setPixel(x, y, transparent)
                else {
                    image.raster.setPixel(x, y, color)
                    image.alphaRaster.setPixel(x, y, solid)
                }
            }
        }
        return image
    }

    /*private fun decode(bytesBuffer: ByteBuffer, position:Int, encoding: ColorEncoding, width:Int, height:Int) : BufferedImage? {
        bytesBuffer.position(position)
        val image = UIUtil.createImage(width, height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = SpriteColorUtil.getColor(bytesBuffer.uInt16, encoding)
                if (color[0] == 0 && color[1] == 0 && color[2] == 0)
                    image.alphaRaster.setPixel(x, y, transparent)
                else {
                    image.raster.setPixel(x, y, color)
                    image.alphaRaster.setPixel(x, y, solid)
                }
            }
        }
        return image
    }*/

    override fun encode(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun copy(): S16SpriteFrame {
        val myGetImage = this.getImage
        return S16SpriteFrame(width, height, encoding).apply {
            getImage = myGetImage
        }
    }

    fun copy(encoding: ColorEncoding): S16SpriteFrame {
        val myGetImage = this.getImage
        return S16SpriteFrame(width, height, encoding).apply {
            getImage = myGetImage
        }
    }
}