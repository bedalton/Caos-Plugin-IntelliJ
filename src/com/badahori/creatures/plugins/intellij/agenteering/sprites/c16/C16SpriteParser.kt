package com.badahori.creatures.plugins.intellij.agenteering.sprites.c16

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.uInt16BE
import com.badahori.creatures.plugins.intellij.agenteering.utils.uInt32BE
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.*
import com.intellij.openapi.vfs.VirtualFile
import java.awt.image.BufferedImage
import java.nio.ByteBuffer


/**
 * Parses Creatures C16 sprite file
 * Based on c2ephp by telyn
 * @see https://github.com/telyn/c2ephp
 */
class C16SpriteFile(file:VirtualFile) : SpriteFile<C16SpriteFrame>(SpriteType.C16) {

    init {
        val rawBytes = file.contentsToByteArray()
        val bytesBuffer = ByteBuffer.wrap(rawBytes)
        val buffer = bytesBuffer.uInt32BE
        val encoding = if (buffer and 1L == 1L) ColorEncoding.x565 else ColorEncoding.x555
        if (buffer and 2L == 0L) {
            throw Exception("C16 parse exception. This file is probably a S16 masquerading as a C16!")
        } else if (buffer > 3) {
            throw Exception("File encoding not recognised. ('$buffer')")
        }
        val numImages = bytesBuffer.uInt16BE
        _frames =  (0 until numImages).map {
            val offsetForData = bytesBuffer.uInt32BE
            val width = bytesBuffer.uInt16BE
            val height = bytesBuffer.uInt16BE
            (0 until height - 1).forEach { _ ->
                bytesBuffer.uInt32BE
            }
            C16SpriteFrame(
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

class C16SpriteFrame private constructor(width:Int, height:Int, private val encoding:ColorEncoding) : SpriteFrame<C16SpriteFrame>(width, height, SpriteType.C16) {

    private lateinit var getImage:()->BufferedImage?

    constructor(bytes:ByteBuffer, offset:Long, width:Int, height:Int, encoding: ColorEncoding) : this(width, height, encoding){
        getImage = {
            decode(bytes, offset)
        }
    }

    constructor(image: BufferedImage, encoding: ColorEncoding = ColorEncoding.x565) : this(image.width, image.height, encoding) {
        getImage = { image }
    }

    override fun decode() : BufferedImage? {
        if (this::getImage.isInitialized)
            return getImage()
        return null
    }

    private fun decode(bytes:ByteBuffer, offset:Long) : BufferedImage? {
        val bytesBuffer = bytes.duplicate()
        bytesBuffer.position(offset.toInt())
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until height) {
            var x = 0
            while (x < width) {
                val run = bytesBuffer.uInt16BE
                val runLength = (run and 0x7FFF) shr 1
                val z = x+runLength
                if (z > width) {
                    LOGGER.severe("RunLength ($runLength) + X($x) is greater than width($width)")
                    return null
                }
                if ((run and 0x0001) <= 0) {
                    while (x < z) {
                        image.alphaRaster.setPixel(x,y, SpriteColorUtil.transparent)
                        x++
                    }
                } else {
                    while (x < z) {
                        val color = SpriteColorUtil.getColor(bytesBuffer.uInt16BE, encoding)
                        image.raster.setPixel(x,y,color)
                        image.alphaRaster.setPixel(x, y, SpriteColorUtil.solid)
                        x++
                    }
                }
            }
            if (x == width) {
                bytesBuffer.uInt16BE
            }
        }
        return image
    }

    override fun encode(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun copy(): C16SpriteFrame {
        val myGetImage = this.getImage
        return C16SpriteFrame(width, height, encoding).apply {
            getImage = myGetImage
        }
    }

    fun copy(encoding: ColorEncoding): C16SpriteFrame {
        val myGetImage = this.getImage
        return C16SpriteFrame(width, height, encoding).apply {
            getImage = myGetImage
        }
    }
}