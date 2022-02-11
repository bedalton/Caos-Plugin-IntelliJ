package com.badahori.creatures.plugins.intellij.agenteering.sfc.reader

import bedalton.creatures.bytes.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant.C1
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant.C2
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.sfc.*
import com.badahori.creatures.plugins.intellij.agenteering.sfc.lang.SFC_DECOMPILED_DATA_KEY
import com.badahori.creatures.plugins.intellij.agenteering.sfc.lang.SfcDecompiledFilePropertyPusher
import com.badahori.creatures.plugins.intellij.agenteering.sfc.reader.Ptr.SfcObjectPtr
import com.badahori.creatures.plugins.intellij.agenteering.sfc.reader.Ptr.SfcSceneryPtr
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.io.IOException


/**
 * A reader class for SFC Files.
 */
internal class SfcReader(internal val byteBuffer: ByteStreamReader, private val sfcFilePath: String? = null) {
    lateinit var variant: CaosVariant
    // Ptr objects are required as objects are accessed by child creation calls
    // while still being constructed by decompiler.
    // As I chose to use immutable data structures, this was problematic.
    // Non-immutable objects would have probably made deserializing much nicer though
    // And that may need to be explored next
    val storage: MutableList<Ptr<*>?> = mutableListOf()

    // Maps PID values to their SFC type
    val types: MutableMap<Int, SfcType> = mutableMapOf()

    internal var readingScenery: Boolean = false
    internal var readingCompoundObject = false

    private var dataHolder:SfcFileDataHolder? = null

    /**
     * Responsible for reading the Bytes into an SFC data structure
     */
    fun readFile(): SfcFile? {
        dataHolder?.let { return it.data }
        // Ensure buffer is at zero in case of repeated reads
        byteBuffer.position(0)
        val mapData = (readClass(SfcType.MAP_DATA)!!.pointed as PointerSfcMapData)
        assert(this::variant.isInitialized) { "Variant should have been set at this point" }
        // Skip to objects start
        seekToObjectsBlock()

        // Read in all Objects
        val objects = readList {
            readClass(SfcType.OBJECT) as SfcObjectPtr<*>?
        }

        // Read in Scenery
        val scenery = readList {
            readClass(SfcType.SCENERY) as? SfcSceneryPtr
        }.filterNotNull()

        // Read in Scripts
        val scripts = readList {
            readScript()
        }

        // Parse Start position
        val scrollPosition = vector2

        // Random assert found in OpenC2e parser
        assert(uInt16 == 0)

        // Read in favorite places
        val favoritePlaces = readFavoritePlaces()

        // Read in Speech history
        val speechHistory = readList16 {
            sfcString
        }

        // If variant is C2,
        // Skip redundant favorite places name string list
        if (variant == C2) {
            // Location strings are listed again
            readList16 {
                sfcString
            }
        }
        // Read all macros
        val macros = readList {
            readClass(SfcType.MACRO)?.pointed as? PointerSfcMacro
        }.filterNotNull()

        // Output actual SFC file.
        return SfcFile(
                variantString = variant.code,
                mapData = mapData.point(),
                objects = objects.mapNotNull { it?.pointed!!.point() },
                scenery = scenery.pointed,
                scripts = scripts,
                macros = macros.map { it.point() },
                scrollPosition = scrollPosition,
                favoritePlaces = favoritePlaces,
                speechHistory = speechHistory
        ).apply {
            // Set the data holder for simplify future calls
            dataHolder = SfcFileDataHolder(this)

            // Write a dump file as needed.
            // Todo: Remove this before pushing plugin update
            if (sfcFilePath != null)
                File("$sfcFilePath.dump.json").writeText("$this")
        }
    }

    /**
     * Method to skip all empty blocks until start of Object data definitions
     */
    private fun seekToObjectsBlock() {
        var x = 0
        while (x == 0)
            x = uInt8
        byteBuffer.position(byteBuffer.position() - 1)
    }

    /**
     * Helper method to read list of favorite places.
     * There can be up to 6 favorite places in Creatures 1 and 2
     */
    private fun readFavoritePlaces(): List<SfcFavoritePlace> {
        val favoritePlaces = mutableListOf<SfcFavoritePlace>()
        var hasNext = true
        while (hasNext && favoritePlaces.size < 6) {
            val favoritePlaceName = sfcString
            val favoritePlacePosition = Vector2(uInt16, uInt16)
            favoritePlaces.add(SfcFavoritePlace(favoritePlaceName, favoritePlacePosition))
            // Peak if C1 as it is possible that the next one is part of Speech History
            hasNext = (if (variant == C1) (peakUInt8() ?: 0) else uInt8) != 0
        }
        val emptySlots = 6 - favoritePlaces.size
        val toSkip = (emptySlots * 5) + (if (variant == C2) 1 else 0)
        skip(toSkip)
        return favoritePlaces
    }

    /**
     * Helper function to read in a list of items
     * using an uInt32 for the number of object
     */
    private fun <T> readList(getter: SfcReader.() -> T): List<T> {
        return (0 until uInt32).map {
            this.getter()
        }
    }
    /**
     * Helper function to read in a list of items
     * using an uInt16 for the number of object
     */
    private fun <T> readList16(getter: SfcReader.() -> T): List<T> {
        return (0 until uInt16).map {
            this.getter()
        }
    }

    // ==== Convenience methods ==== //
    val uInt8 get() = byteBuffer.uInt8
    fun peakUInt8(): Int? = byteBuffer.peakUInt8()
    val uInt16 get() = byteBuffer.uInt16
    val uInt32 get() = byteBuffer.uInt32.toInt()
    fun skip(bytes: Int) = byteBuffer.skip(bytes)
    fun variantSkip(c1Bytes: Int, c2Bytes: Int) = byteBuffer.skip(if (variant == C1) c1Bytes else c2Bytes)

    /**
     * Reads in a string with a given length
     */
    fun string(length: Int): String = byteBuffer.string(length).trim(' ', 0x00.toChar())

    /**
     * Reads in a string given a variable string length
     */
    val sfcString: String
        get() {
            return byteBuffer.string(sfcStringLength).trim(' ', 0x00.toChar())
        }

    /**
     * SFC string lengths can be variable, defined by a byte, short or int
     * They are read cascading to the next numeric type as needed
     */
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

        /**
         * Reads an SFC virtual file into a data object.
         */
        fun readFile(virtualFile: VirtualFile, cache: Boolean = true, safe: Boolean = false): SfcFileDataHolder {
            // If desired, read from cache. This is preferred. As parsing takes a while
            if (cache) {
                val cached = virtualFile.getUserData(SFC_DECOMPILED_DATA_KEY)
                        ?: try { SfcDecompiledFilePropertyPusher.readFromStorage(virtualFile) } catch(e:Exception) { null }
                cached?.let {
                    return it
                }
            }
            // Check for valid SFC
            if (virtualFile.extension?.equalsIgnoreCase("sfc").orFalse() && isSfc(virtualFile))
                throw IOException("Cannot parse non-sfc file as sfc")

            // Parse or create data holder
            val holder = try {
                readRaw(virtualFile)
            } catch (ae:AssertionError) {
                if (safe) {
                    LOGGER.severe("Failed to parse SFC with error: ${ae.message}")
                    ae.printStackTrace()
                    SfcFileDataHolder(error = "Failed to parse SFC with error: ${ae.message}")
                } else throw SfcReadException(ae.localizedMessage)
            }catch (e: Exception) {
                if (safe) {
                    LOGGER.severe("Failed to parse SFC with error: ${e.message}")
                    e.printStackTrace()
                    SfcFileDataHolder(error = "Failed to parse SFC with error: ${e.message}")
                } else throw e
            }

            // Persists a data holder to ensure that if the holder exists,
            // then the data within is the actual parse result
            // IntelliJ keeps trying to parse the same invalid files
            // over and over again
            virtualFile.putUserData(SFC_DECOMPILED_DATA_KEY, holder)
            SfcDecompiledFilePropertyPusher.writeToStorage(virtualFile, holder)

            // Finally, return the data, whatever it may be
            return holder
        }

        private fun readRaw(virtualFile: VirtualFile): SfcFileDataHolder {
            val byteBuffer = MemoryByteStreamReader(virtualFile.contentsToByteArray())
            val dumper = SfcReader(byteBuffer, virtualFile.path)
            val data = dumper.readFile()
            return SfcFileDataHolder(data)
        }

        /**
         * Checks the first 13 bytes to see if they are a valid SFC file start
         */
        fun isSfc(virtualFile: VirtualFile): Boolean {
            return virtualFile.contentsToByteArray().let out@{ bytes ->
                (0 until 13).none { i ->
                    headerBytes[i].let { it != null && it != bytes[i] }
                }
            }
        }

        // A list of the initial header bytes for an SFC file
        // The 3rd and 4th bytes are discarded and could be anything
        private val headerBytes: List<Byte?> by lazy {
            listOf(
                    0xFF,
                    0xFF,
                    null,
                    null,
                    0x00,
                    0x07,
                    0x4D,
                    0x61,
                    0x70,
                    0x44,
                    0x61,
                    0x74,
                    0x61
            ).map { it?.toByte() }
        }
    }

}


/**
 * Convenience method to read in bounds objects
 */
internal val SfcReader.bounds
    get() = Bounds(
            left = uInt32,
            top = uInt32,
            right = uInt32,
            bottom = uInt32
    )

/**
 * Convenience method to read in file name tokens
 */
internal val SfcReader.fileNameToken get() = string(4)

/**
 * Convenience method to read in vector 2 objects
 */
internal val SfcReader.vector2 get() = Vector2(uInt32, uInt32)


/**
 * SFC exception class
 */
internal open class SfcReadException(message: String) : IOException(message)

/**
 * SFC exception for out of variant data
 */
internal class OutOfVariantException(val variant: CaosVariant, message: String = "Invalid variant for SFC. Found '${variant.code}' expected: [C1,C2]") : SfcReadException(message)

/**
 * Convenience method for turning a list of PTR objects to their corresponding actual objects
 */
private val <SfcT : SfcData, T : PointerSfcData<SfcT>> List<Ptr<T>?>.pointed: List<SfcT> get() = this.mapNotNull { it?.pointed?.point() }
