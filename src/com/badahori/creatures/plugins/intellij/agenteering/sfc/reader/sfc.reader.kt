package com.badahori.creatures.plugins.intellij.agenteering.sfc.reader

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant.C1
import com.badahori.creatures.plugins.intellij.agenteering.sfc.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import java.io.IOException
import java.nio.ByteBuffer


internal class SfcReader(internal val byteBuffer: ByteBuffer) {
    lateinit var variant: CaosVariant
    val storage: MutableList<SfcData?> = mutableListOf()
    val types: MutableMap<Int, Int> = mutableMapOf()

    internal var readingScenery: Boolean = false
    internal var readingCompoundObject = false

    fun readFile(): SfcFile {
        byteBuffer.position(0)
        val mapData = readClass(TYPE_MAPDATA) as SfcMapData
        assert(this::variant.isInitialized) { "Variant should have been set at this point" }
        var x = 0
        while (x == 0)
            x = uInt8
        byteBuffer.position(byteBuffer.position() - 1)
        val objects = readList {
            readClass(TYPE_OBJECT) as SfcObject
        }
        val scenery = readList {
            readClass(TYPE_SCENERY) as SfcScenery
        }
        val scripts = readList {
            readScript()
        }

        val scrollPosition = vector2
        assert(uInt16 == 0)
        val favoritePlaceName = sfcString
        val favoritePlacePosition = Vector2(uInt16, uInt16)

        variantSkip(25, 29)
        val speechHistory = readList16 {
            sfcString
        }
        val macros = readList {
            readClass(TYPE_MACRO) as? SfcMacro
        }.filterNotNull()
        return SfcFile(
                variant = variant,
                mapData = mapData,
                objects = objects,
                scenery = scenery,
                scripts = scripts,
                macros = macros,
                scrollPosition = scrollPosition,
                favoritePlaceName = favoritePlaceName,
                favoritePlacePosition = favoritePlacePosition,
                speechHistory = speechHistory
        )
    }

    private fun <T> readList(getter: SfcReader.() -> T): List<T> {
        return (0 until uInt32).map {
            getter()
        }
    }

    private fun <T> readList16(getter: SfcReader.() -> T): List<T> {
        return (0 until uInt16).map {
            getter()
        }
    }

    val uInt8 get() = byteBuffer.uInt8
    val uInt16 get() = byteBuffer.uInt16
    val uInt32 get() = byteBuffer.uInt32.toInt()
    fun skip(bytes: Int) = byteBuffer.skip(bytes)
    fun variantSkip(c1Bytes: Int, c2Bytes: Int) = byteBuffer.skip(if (variant == C1) c1Bytes else c2Bytes)
    val sfcString: String
        get() {
            val length = uInt8.let { small ->
                if (small == 0xFF)
                    uInt16.let { medium ->
                        if (medium == 0xFFFF)
                            uInt32
                        else
                            medium
                    }
                else
                    small
            }
            return byteBuffer.cString(length)
        }

    companion object {
        fun readFile(bytes: ByteArray): SfcFile {
            val byteBuffer = ByteBuffer.wrap(bytes).littleEndian()
            val dumper = SfcReader(byteBuffer)
            return dumper.readFile()
        }
    }

}


internal val SfcReader.bounds
    get() = Bounds(
            left = uInt32,
            top = uInt32,
            right = uInt32,
            bottom = uInt32
    )

internal fun SfcReader.string(length: Int): String = byteBuffer.cString(length)
internal val SfcReader.fileNameToken get() = string(4)

internal val SfcReader.vector2 get() = Vector2(uInt32, uInt32)


internal open class SfcReadException(message: String) : IOException(message)

internal class OutOfVariantException(val variant: CaosVariant, message: String = "Invalid variant for SFC. Found '${variant.code}' expected: [C1,C2]") : SfcReadException(message)