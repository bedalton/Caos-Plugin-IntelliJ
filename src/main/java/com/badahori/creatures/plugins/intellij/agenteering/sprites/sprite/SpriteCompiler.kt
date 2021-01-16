package com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite

import com.badahori.creatures.plugins.intellij.agenteering.sprites.ditherCopy
import com.badahori.creatures.plugins.intellij.agenteering.sprites.spr.SprCompiler
import com.badahori.creatures.plugins.intellij.agenteering.utils.writeUInt16
import com.badahori.creatures.plugins.intellij.agenteering.utils.writeUInt8
import com.badahori.creatures.plugins.intellij.agenteering.utils.writeUint32
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream
import java.awt.image.BufferedImage
import java.io.OutputStream

interface SpriteCompiler {
    fun compileSprites(images:List<BufferedImage>) : ByteArray
    fun writeCompiledSprite(image: BufferedImage, buffer: OutputStream)
}