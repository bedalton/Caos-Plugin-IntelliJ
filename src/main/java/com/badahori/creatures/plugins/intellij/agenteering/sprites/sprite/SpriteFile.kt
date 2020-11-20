package com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite

import com.badahori.creatures.plugins.intellij.agenteering.sprites.c16.C16SpriteFrame
import com.badahori.creatures.plugins.intellij.agenteering.sprites.s16.S16SpriteFrame
import com.badahori.creatures.plugins.intellij.agenteering.sprites.spr.SprSpriteFrame
import java.awt.Color
import java.awt.image.BufferedImage

abstract class SpriteFile<SpriteT:SpriteFrame<SpriteT>>(private val type:SpriteType<SpriteT>) {
    protected var _frames:List<SpriteT?> = listOf()

    val images:List<BufferedImage?> get() = _frames.map { it?.image }

    fun getFrame(frameNumber:Int) : SpriteT? = _frames.getOrNull(frameNumber)

    fun removeFrame(frame:SpriteT) {
        val frames = _frames.toMutableList()
        frames.remove(frame)
        _frames = frames
    }

    fun removeFrame(frameIndex:Int) : SpriteT? {
        if (frameIndex > _frames.lastIndex)
            return null
        val frames = _frames.toMutableList()
        val frame = frames.removeAt(frameIndex)
        _frames = frames
        return frame
    }

    fun addFrame(index:Int, frameIn:SpriteFrame<*>) : Boolean {
        val frames = _frames.toMutableList()
        val converted = type.convert(frameIn)
                ?: return false
        frames.add(index, converted)
        _frames = frames
        return true
    }

    fun addFrame(frameIn:SpriteT): Boolean {
        val converted = type.convert(frameIn)
                ?: return false
        _frames = _frames + converted
        return true
    }


    private fun convertFrameIfNeeded(frameIn:SpriteFrame<*>) : SpriteT? {
        return type.convert(frameIn)
    }

    abstract fun compile() : ByteArray



}

abstract class SpriteFrame<SpriteT:SpriteFrame<SpriteT>>(val width:Int, val height:Int, val type:SpriteType<*>) {
    val image: BufferedImage? by lazy {
        decode()
    }

    fun getPixel(x:Int, y:Int) : Color? {
        val image = image ?: return null
        return Color(image.getRGB(x, y))
    }

    @Throws
    abstract fun decode() : BufferedImage?

    abstract fun encode() : ByteArray

    abstract fun copy(): SpriteT
}

sealed class SpriteType<SpriteT:SpriteFrame<SpriteT>>(val fileExtension: String, val convert:(sprite:SpriteFrame<*>)->SpriteT?) {
    object Spr:SpriteType<SprSpriteFrame>("spr", convert@{ sprite:SpriteFrame<*> ->
        if (sprite is SprSpriteFrame) {
            return@convert sprite.copy()
        }
        sprite.image?.let {
            SprSpriteFrame(it)
        }
    })

    object C16:SpriteType<C16SpriteFrame>("c16", convert@{ sprite:SpriteFrame<*> ->
        if (sprite is C16SpriteFrame)
            return@convert sprite.copy()
        sprite.image?.let {
            C16SpriteFrame(it)
        }
    })
    object S16:SpriteType<S16SpriteFrame>("s16", convert@{ sprite:SpriteFrame<*> ->
        if (sprite is S16SpriteFrame)
            return@convert sprite.copy()
        sprite.image?.let {
            S16SpriteFrame(it)
        }
    })
}