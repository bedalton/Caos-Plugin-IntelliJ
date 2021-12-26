package com.badahori.creatures.plugins.intellij.agenteering.sprites.s16

import bedalton.creatures.bytes.ByteStreamReader
import bedalton.creatures.bytes.uInt16
import bedalton.creatures.bytes.uInt32
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.ColorEncoding
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteColorUtil.solid
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteColorUtil.transparent
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteFile
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteFrame
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteType
import com.intellij.openapi.vfs.VirtualFile
import java.awt.image.BufferedImage

/**
 * Parses Creatures S16 sprite file
 * Based on c2ephp by telyn
 * @url https://github.com/telyn/c2ephp
 */
class S16SpriteFile(rawBytes:ByteArray, val filePath: String? = null) : SpriteFile<S16SpriteFrame>(SpriteType.S16) {



    constructor(file: VirtualFile) : this(file.contentsToByteArray(), file.path)

    init {
        val bytesBuffer = ByteStreamReader(rawBytes)
        val encoding = when (val buffer = bytesBuffer.uInt32) {
            1L -> ColorEncoding.X_565
            0L -> ColorEncoding.X_555
            else -> throw Exception("File encoding not recognized. ('$buffer') for file: $filePath")
        }
        val numImages = bytesBuffer.uInt16
        mFrames = (0 until numImages).map {
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

    constructor(bytes: ByteStreamReader, offset: Long, width: Int, height: Int, encoding: ColorEncoding) : this(width, height, encoding) {
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

    private fun decode(bytes: ByteStreamReader, offset: Long): BufferedImage {
        val bytesBuffer = bytes.duplicate()
        bytesBuffer.position(offset.toInt())
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val toRgb = encoding.toRgb
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bytesBuffer.uInt16
                val color = toRgb(pixel)
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

    /*private fun decode(bytesBuffer: ByteStreamReader, position:Int, encoding: ColorEncoding, width:Int, height:Int) : BufferedImage? {
        bytesBuffer.position(position)
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
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