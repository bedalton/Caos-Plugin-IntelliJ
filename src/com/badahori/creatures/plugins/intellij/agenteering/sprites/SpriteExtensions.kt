package com.badahori.creatures.plugins.intellij.agenteering.sprites

import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage


fun BufferedImage.createTransformed(at: AffineTransform) : BufferedImage {
    val newImage = BufferedImage(
            getWidth(), getHeight(),
            BufferedImage.TYPE_INT_ARGB)
    val g = newImage.createGraphics()
    g.transform(at)
    g.drawImage(this, 0, 0, null)
    g.dispose()
    return newImage
}

fun BufferedImage.flip() : BufferedImage? {
    val at = AffineTransform()
    at.concatenate(AffineTransform.getScaleInstance(1.0, -1.0))
    at.concatenate(AffineTransform.getTranslateInstance(0.0, -height.toDouble()))
    return createTransformed(at)
}