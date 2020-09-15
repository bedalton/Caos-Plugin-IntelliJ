package com.badahori.creatures.plugins.intellij.agenteering.sprites

import com.intellij.util.ui.UIUtil
import java.awt.Image
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.RenderedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO


fun BufferedImage.createTransformed(at: AffineTransform) : BufferedImage {
    val newImage = UIUtil.createImage(
            width, height,
            BufferedImage.TYPE_INT_ARGB)
    val g = newImage.createGraphics()
    g.transform(at)
    g.drawImage(this, 0, 0, null)
    g.dispose()
    return newImage
}

fun BufferedImage.flipVertical() : BufferedImage? {
    val at = AffineTransform()
    at.concatenate(AffineTransform.getScaleInstance(1.0, -1.0))
    at.concatenate(AffineTransform.getTranslateInstance(0.0, -height.toDouble()))
    return createTransformed(at)
}
fun BufferedImage.flipHorizontal() : BufferedImage? {
    val at = AffineTransform()
    at.concatenate(AffineTransform.getScaleInstance(-1.0, 1.0))
    at.concatenate(AffineTransform.getTranslateInstance(0.0, -height.toDouble()))
    return createTransformed(at)
}

fun RenderedImage.toPngByteArray() : ByteArray {
    return toByteArray("png")
}

fun RenderedImage.toJpgByteArray() : ByteArray {
    return toByteArray("jpg")
}

fun RenderedImage.toByteArray(formatName:String) : ByteArray {
    ImageIO.getWriterFormatNames().let { formats ->
        assert(formatName in formats) {
            "Cannot convert image to byte array for format: '$formatName'. Available types: [${formats.joinToString()}]"
        }
    }
    val outputStream = ByteArrayOutputStream()
    ImageIO.write(this, formatName, outputStream)
    return outputStream.toByteArray()
}