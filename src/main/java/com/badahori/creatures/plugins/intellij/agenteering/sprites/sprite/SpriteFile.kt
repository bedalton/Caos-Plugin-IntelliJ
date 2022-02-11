package com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite

import com.badahori.creatures.plugins.intellij.agenteering.sprites.blk.BlkSpriteFrame
import com.badahori.creatures.plugins.intellij.agenteering.sprites.c16.C16SpriteFrame
import com.badahori.creatures.plugins.intellij.agenteering.sprites.s16.S16SpriteFrame
import com.badahori.creatures.plugins.intellij.agenteering.sprites.spr.SprSpriteFrame
import java.awt.Color
import java.awt.image.BufferedImage

abstract class SpriteFile<SpriteT:SpriteFrame<SpriteT>>(private val type:SpriteType<SpriteT>) {

    protected val mFrames: List<SpriteT?> by lazy {
        buildFrames()
    }
    val size: Int get() = mFrames.size

    protected abstract fun buildFrames(): List<SpriteT?>

    protected abstract fun close(): Boolean

    open val images: List<BufferedImage?> by lazy {
        val images = mFrames.mapIndexed { _, it ->
            it?.image
        }
        close()
        images
    }

    fun getFrame(frameNumber: Int): SpriteT? = mFrames.getOrNull(frameNumber)

//    fun removeFrame(frame:SpriteT) {
//        val frames = mFrames.toMutableList()
//        frames.remove(frame)
//        mFrames = frames
//    }
//
//    fun removeFrame(frameIndex:Int) : SpriteT? {
//        if (frameIndex > mFrames.lastIndex)
//            return null
//        val frames = mFrames.toMutableList()
//        val frame = frames.removeAt(frameIndex)
//        mFrames = frames
//        return frame
//    }
//
//    fun addFrame(index:Int, frameIn:SpriteFrame<*>) : Boolean {
//        val frames = mFrames.toMutableList()
//        val converted = type.convert(frameIn)
//                ?: return false
//        frames.add(index, converted)
//        mFrames = frames
//        return true
//    }
//
//    fun addFrame(frameIn:SpriteT): Boolean {
//        val converted = type.convert(frameIn)
//                ?: return false
//        mFrames = mFrames + converted
//        return true
//    }
//
//
//    private fun convertFrameIfNeeded(frameIn:SpriteFrame<*>) : SpriteT? {
//        return type.convert(frameIn)
//    }

    abstract fun compile(): ByteArray


    operator fun get(index: Int): SpriteT? {
        return mFrames[index]
    }
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

    object BLK:SpriteType<BlkSpriteFrame>("blk", convert@{ sprite:SpriteFrame<*> ->
        if (sprite is BlkSpriteFrame)
            return@convert sprite.copy()
        sprite.image?.let {
            BlkSpriteFrame(it)
        }

    })
}