package com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite

import java.awt.image.BufferedImage
import java.io.OutputStream

interface SpriteCompiler {
    fun compileSprites(images:List<BufferedImage>) : ByteArray
    fun writeCompiledSprite(image: BufferedImage, buffer: OutputStream)
}