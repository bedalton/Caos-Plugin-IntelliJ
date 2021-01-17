package com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite

object SpriteColorUtil {
    val solid = intArrayOf(255,255,255,255)
    val transparent = intArrayOf(0,0,0,0)
}


enum class ColorEncoding(
    val marker: Int,
    private val encodeBitMasks: Triple<Int, Int, Int>,
    private val decodeBitMask: Triple<Int, Int, Int>,
    private val bitShifts: Triple<Int, Int, Int>
) {
    X_555(
        0,
        Triple(0xF8, 0xF8, 0xF8),
        Triple(0x7C00, 0x03E0, 0x001F),
        Triple(7, 2, 3)
    ),
    X_565(
        1,
        Triple(0xF8, 0xFC, 0xF8),
        Triple(0xF800, 0x07E0, 0x001F),
        Triple(8, 3, 3)
    );

    val toInt:(red:Int,green:Int, blue:Int) -> Int by lazy {
        { red: Int, green: Int, blue: Int ->
            ((red and encodeBitMasks.first) shl bitShifts.first)  or
                    ((green and encodeBitMasks.second) shl bitShifts.second) or
                    ((blue and bitShifts.third) shr encodeBitMasks.third)
        }
    }

    val toRgb:(pixel:Int) -> IntArray by lazy {
        { pixel: Int ->
            val red = (pixel and decodeBitMask.first) shr bitShifts.first
            val green = (pixel and decodeBitMask.second) shr bitShifts.second
            val blue = (pixel and decodeBitMask.third) shl bitShifts.third
            intArrayOf(red, green, blue, 0)
        }
    }

}