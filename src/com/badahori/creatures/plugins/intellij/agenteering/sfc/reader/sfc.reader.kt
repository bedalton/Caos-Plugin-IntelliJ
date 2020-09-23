package com.badahori.creatures.plugins.intellij.agenteering.sfc.reader

import com.badahori.creatures.plugins.intellij.agenteering.PointerSfc.PointerSfcData
import com.badahori.creatures.plugins.intellij.agenteering.PointerSfc.PointerSfcMacro
import com.badahori.creatures.plugins.intellij.agenteering.PointerSfc.PointerSfcMapData
import com.badahori.creatures.plugins.intellij.agenteering.PointerSfc.Ptr
import com.badahori.creatures.plugins.intellij.agenteering.PointerSfc.Ptr.SfcObjectPtr
import com.badahori.creatures.plugins.intellij.agenteering.PointerSfc.Ptr.SfcSceneryPtr
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant.C1
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant.C2
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.sfc.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer


internal class SfcReader(internal val byteBuffer: ByteBuffer, private val sfcFilePath: String? = null) {
    lateinit var variant: CaosVariant
    val storage: MutableList<Ptr<*>?> = mutableListOf()
    val types: MutableMap<Int, SfcType> = mutableMapOf()

    internal var readingScenery: Boolean = false
    internal var readingCompoundObject = false

    fun readFile(): SfcFile {
        byteBuffer.position(0)
        val mapData = (readClass(SfcType.MAP_DATA)!!.pointed as PointerSfcMapData)
        assert(this::variant.isInitialized) { "Variant should have been set at this point" }
        var x = 0
        while (x == 0)
            x = uInt8
        byteBuffer.position(byteBuffer.position() - 1)
        val objects = readList {
            readClass(SfcType.OBJECT) as SfcObjectPtr<*>?
        }
        val scenery = readList {
            readClass(SfcType.SCENERY) as? SfcSceneryPtr
        }.filterNotNull()
        val scripts = readList {
            readScript()
        }
        val scrollPosition = vector2
        assert(uInt16 == 0)
        val favoritePlaces = readFavoritePlaces()
        val speechHistory = readList16 {
            sfcString
        }
        if (variant == C2) {
            // Location strings are listed again
            readList16 {
                sfcString
            }
        }
        val macros = readList {
            readClass(SfcType.MACRO)?.pointed as? PointerSfcMacro
        }.filterNotNull()
        return SfcFile(
                variant = variant,
                mapData = mapData.point(),
                objects = objects.mapNotNull { it?.pointed!!.point() },
                scenery = scenery.pointed,
                scripts = scripts,
                macros = macros.map { it.point() },
                scrollPosition = scrollPosition,
                favoritePlaces = favoritePlaces,
                speechHistory = speechHistory
        ).apply {
            if (sfcFilePath != null)
                File("$sfcFilePath.dump.json").writeText("$this")
        }
    }

    private fun readFavoritePlaces(): List<SfcFavoritePlace> {
        val favoritePlaces = mutableListOf<SfcFavoritePlace>()
        var hasNext = true
        while (hasNext && favoritePlaces.size < 6) {
            val favoritePlaceName = sfcString
            val favoritePlacePosition = Vector2(uInt16, uInt16)
            favoritePlaces.add(SfcFavoritePlace(favoritePlaceName, favoritePlacePosition))
            // Peak if C1 as it is possible that the next one is part of Speech History
            hasNext = (if (variant == C1) peakUInt8() else uInt8) != 0
        }
        val emptySlots = 6 - favoritePlaces.size
        val toSkip = (emptySlots * 5) + (if (variant == C2) 1 else 0)
        skip(toSkip)
        return favoritePlaces
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
    fun peakUInt8(): Int = byteBuffer.peakUInt8()
    val uInt16 get() = byteBuffer.uInt16
    val uInt32 get() = byteBuffer.uInt32.toInt()
    fun skip(bytes: Int) = byteBuffer.skip(bytes)
    fun variantSkip(c1Bytes: Int, c2Bytes: Int) = byteBuffer.skip(if (variant == C1) c1Bytes else c2Bytes)
    fun string(length: Int): String = byteBuffer.cString(length).trim(' ', 0x00.toChar())
    val sfcString: String
        get() {
            return byteBuffer.cString(sfcStringLength).trim(' ', 0x00.toChar())
        }

    private val sfcStringLength: Int
        get() {
            return uInt8.let { small ->
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
        }

    companion object {
        fun readFile(bytes: ByteArray, sfcPath: String? = null): SfcFile {
            val byteBuffer = ByteBuffer.wrap(bytes).littleEndian()
            val dumper = SfcReader(byteBuffer, sfcPath)
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

internal val SfcReader.fileNameToken get() = string(4)

internal val SfcReader.vector2 get() = Vector2(uInt32, uInt32)


internal open class SfcReadException(message: String) : IOException(message)

internal class OutOfVariantException(val variant: CaosVariant, message: String = "Invalid variant for SFC. Found '${variant.code}' expected: [C1,C2]") : SfcReadException(message)

private val <SfcT : SfcData, T : PointerSfcData<SfcT>> List<Ptr<T>>.pointed: List<SfcT> get() = this.mapNotNull { it.pointed?.point() }