package com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite

object SpriteColorUtil {
    val solid = intArrayOf(255,255,255,255)
    val transparent = intArrayOf(0,0,0,0)
    fun getColor(pixel:Int, encoding:ColorEncoding) : IntArray {
        return if (encoding == ColorEncoding.X_565) {
            val red = (pixel and 0xF800) shr 8
            val green = (pixel and 0x07E0) shr 3
            val blue = (pixel and 0x001F) shl 3
            intArrayOf(red, green, blue, 0)
        } else {
            val red = (pixel and 0x7C00) shr 7
            val green = (pixel and 0x03E0) shr 2
            val blue = (pixel and 0x001F) shl 3
            intArrayOf(red, green, blue, 0)
        }
    }
}


enum class ColorEncoding {
    X_555,
    X_565
}