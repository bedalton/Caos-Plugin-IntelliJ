package com.badahori.creatures.plugins.intellij.agenteering.indices

import bedalton.creatures.common.structs.BreedKey
import bedalton.creatures.common.structs.GameVariant
import com.bedalton.common.util.PathUtil
import com.badahori.creatures.plugins.intellij.agenteering.att.lang.getInitialVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.sprites.c16.C16FileType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.s16.S16FileType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.spr.SprFileType
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.lowercase
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileContent
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException
import java.util.*

object VariantIndexer : DataIndexer<CaosVariant, Void, FileContent> {
    const val VERSION = 1
    override fun map(file: FileContent): MutableMap<CaosVariant, Void?> {
        val variant = getInitialVariant(file.project, file.file)
            .nullIfUnknown()
            ?: return mutableMapOf()
        return if (variant.isC3DS) {
            Collections.singletonMap(CaosVariant.C3, null)
        } else
            Collections.singletonMap(variant, null)
    }
}


object BreedKeyIndexer : DataIndexer<BreedPartKey, Void, FileContent> {

    const val VERSION = 4

    override fun map(file: FileContent): MutableMap<BreedPartKey, Void> {
        var variant = getInitialVariant(file.project, file.file)
            .nullIfUnknown()
        if (variant == CaosVariant.DS)
            variant = CaosVariant.C3
        val breedKey = BreedPartKey.fromFileName(file.fileName, variant)
            ?: return Collections.emptyMap()
        return Collections.singletonMap(breedKey, null)
    }
}

@Suppress("unused")
data class BreedPartKey(
    val variant: CaosVariant? = null,
    val genus: Int? = null,
    val gender: Int? = null,
    val breed: Char? = null,
    val ageGroup: Int? = null,
    val part: Char? = null,
) {

    val code: String?
        get() = if (genus != null && gender != null && breed != null && ageGroup != null) {
            val first = (gender * 4) + genus
            "${part ?: ""}$first$ageGroup$breed"
        } else {
            null
        }

    operator fun get(index: Int): Char? {
        if (index !in 0..3)
            return null
        return when (index) {
            0 -> part?.lowercase()
            1 -> if (genus == null || gender == null) null else '0' + (genus + (gender * 4))
            2 -> ageGroup?.let { '0' + it }
            3 -> breed?.lowercase()
            else -> null
        }
    }

    fun copyWithVariant(variant: CaosVariant?): BreedPartKey {
        return copy(
            variant = variant
        )
    }

    fun copyWithGender(gender: Int?): BreedPartKey {
        return copy(
            gender = gender
        )
    }


    fun copyWithGenus(genus: Int?): BreedPartKey {
        return copy(
            genus = genus
        )
    }

    fun copyWithBreed(breed: Char?): BreedPartKey {
        return copy(
            breed = breed?.lowercase()
        )
    }


    fun copyWithAgeGroup(ageGroup: Int?): BreedPartKey {
        return copy(
            ageGroup = ageGroup
        )
    }

    fun copyWithPart(part: Char?): BreedPartKey {
        return copy(
            part = part?.lowercase()
        )
    }

    @Suppress("MemberVisibilityCanBePrivate")
    val gameVariant: GameVariant? by lazy {
        val caosVariant = variant
            ?: return@lazy null
        GameVariant.fromString(caosVariant.code)
    }

    val breedKey by lazy {
        BreedKey(
            gameVariant,
            genus,
            gender,
            breed,
            ageGroup
        )
    }

    val unreliableHash: Int
        get() {
            var result = genus ?: 0
            result = 31 * result + (gender ?: 0)
            result = 31 * result + (breed?.lowercase()?.hashCode() ?: 0)
            result = 31 * result + (ageGroup ?: 0)
            result = 31 * result + (part?.lowercase()?.hashCode() ?: 0)
            return result
        }

    fun distance(other: BreedPartKey?): Int? {
        if (other == null) {
            return null
        }
        if (part != null && other.part != null && part != other.part) {
            return null
        }

        var distance = 0

        // Look down and then up
        if (this.genus != null && other.genus != null) {
            distance += if (other.genus <= this.genus) {
                this.genus - other.genus
            } else {
                other.genus
            }
        }

        distance *= 256

        // Calculate distance based on breed
        if (breed != null && other.breed != null) {
            val otherBreed: Int = if (other.breed in '0'..'9') { other.breed - '0' } else { other.breed - 'a' }
            val breed: Int = if (breed in '0'..'9') (breed - '0') else (breed - 'a')
            distance += if (otherBreed <= breed) {
                breed - otherBreed
            } else {
                otherBreed
            }
        }

        // Calculate distance based on sex
        distance *= 2
        if (other.gender != this.gender) {
            distance += 1
        }

        // Distance by age
        distance *= 10
        if (this.ageGroup != null && other.ageGroup != null) {
            val otherAge = other.ageGroup
            distance += if (otherAge <= ageGroup) {
                ageGroup - otherAge
            } else {
                otherAge
            }
        }
        return distance
    }


    companion object {
        const val VERSION: Int = 6
        const val allowZ: Boolean = false

        @JvmStatic
        fun fromFileName(fileName: String, variant: CaosVariant? = null): BreedPartKey? {
            if (!isPartName(fileName, variant)) {
                return null
            }

            val chars = PathUtil.getFileNameWithoutExtension(fileName)?.lowercase()?.toCharArray()
                ?: return null

            return BreedPartKey(
                variant = variant,
                genus = ((chars[1] - '0') % 4),
                gender = if (chars[1] - '0' < 4) 0 else 1,
                ageGroup = (chars[2] - '0'),
                breed = chars[3],
                part = chars[0]
            )
        }

        @JvmStatic
        fun isPartName(fileName: String, variant: CaosVariant? = null): Boolean {
            val chars = PathUtil.getFileNameWithoutExtension(fileName)?.lowercase()?.toCharArray()
                ?: return false
            if (chars.size != 4) {
                return false
            }
            if (chars[0] !in 'a'..'q' && (allowZ && chars[0] != 'z')) {
                return false
            }
            if (chars[1] !in '0'..'7') {
                return false
            }
            if (chars[2] !in '0'..'9') {
                return false
            }
            return when (variant) {
                CaosVariant.C1 -> chars[3] in '0'..'9'
                null -> chars[3] in 'a'..'z' || chars[3] in '0'..'9'
                else -> chars[3] in 'a'..'z'
            }
        }

        /**
         * Checks for a generic match, where a wild-card value is a null
         */
        @JvmStatic
        fun isGenericMatch(val1: BreedPartKey?, val2: BreedPartKey?): Boolean {

            // If both null, then they are equal?
            if (val1 == null && val2 == null)
                return true

            // If one is null and not the other, cannot be equal
            if (val1 == null || val2 == null)
                return false

            if (val1 == val2)
                return true


            // Make sure if non-null variants are equal
            if (val1.variant != null && val2.variant != null && val1.variant.code != val2.variant.code) {
                if (val1.variant.isC3DS != val2.variant.isC3DS || !val1.variant.isC3DS) {
                    return false
                }
            }

            // Make sure if non-null genus are equal
            if (val1.genus != null && val2.genus != null && val1.genus != val2.genus)
                return false

            // Make sure if non-null gender are equal
            if (val1.gender != null && val2.gender != null && val1.gender != val2.gender)
                return false

            // Make sure if non-null breed are equal
            if (val1.breed != null && val2.breed != null && val1.breed.lowercase() != val2.breed.lowercase())
                return false

            // Make sure if non-null age-group are equal
            if (val1.ageGroup != null && val2.ageGroup != null && val1.ageGroup != val2.ageGroup)
                return false

            // Make sure if non-null parts are equal
            if (val1.part != null && val2.part != null && val1.part.lowercase() != val2.part.lowercase())
                return false
            return true
        }
    }
}

object BreedPartDescriptor : KeyDescriptor<BreedPartKey> {

    const val VERSION = 3

    override fun getHashCode(value: BreedPartKey): Int {
        return value.hashCode()
    }

    override fun isEqual(val1: BreedPartKey, val2: BreedPartKey): Boolean {
        return val1 == val2 || BreedPartKey.isGenericMatch(val1, val2)
    }

    @Throws(IOException::class)
    override fun save(storage: DataOutput, value: BreedPartKey) {
        value.apply {
            IOUtil.writeUTF(storage, variant?.code ?: "")
            storage.writeInt(genus ?: -1)
            storage.writeInt(gender ?: -1)
            storage.writeInt(breed?.lowercase()?.code ?: -1)
            storage.writeInt(ageGroup ?: -1)
            storage.writeInt(part?.lowercase()?.code ?: -1)
        }
    }

    @Throws(IOException::class)
    override fun read(storage: DataInput): BreedPartKey {
        val variantString = IOUtil.readUTF(storage)
        val variant = variantString.nullIfEmpty()?.let { CaosVariant.fromVal(it) }
        val genus = storage.readInt().let { if (it < 0) null else it }
        val gender = storage.readInt().let { if (it < 0) null else it }
        val breed = storage.readInt().let { if (it < 0) null else it.toChar() }
        val ageGroup = storage.readInt().let { if (it < 0) null else it }
        val part = storage.readInt().let { if (it < 0) null else it.toChar() }
        return BreedPartKey(
            variant, genus, gender, breed, ageGroup, part
        )
    }
}

class BreedFileInputFilter(private val fileTypes: List<FileType>) : FileBasedIndex.InputFilter {

    private val fileExtensions by lazy {
        fileTypes.map { it.defaultExtension.lowercase() }.apply {
            "Sprite File extensions: [${this.joinToString(",")}]"
        }
    }

    override fun acceptInput(file: VirtualFile): Boolean {
        if (fileTypes.isNotEmpty() && file.fileType !in fileTypes && file.extension?.lowercase() !in fileExtensions)
            return false

        // Get breed sprite chars
        val nameChars = file.nameWithoutExtension.lowercase().toCharArray()

        // File name has correct length
        if (nameChars.size != 4)
            return false

        // Part number in range
        if (nameChars[0] !in 'a'..'q')
            return false

        // Genus/Gender in range
        if (nameChars[1] !in '0'..'7')
            return false

        // Correct life stage
        if (nameChars[2] !in '0'..'9')
            return false

        // Breed is in range
        return nameChars[3] in '0'..'9' || (BreedPartKey.allowZ && nameChars[3] in 'a'..'z')
    }


    @Suppress("unused")
    private val spriteFileTypes = listOf(
        SprFileType,
        S16FileType,
        C16FileType
    )

    companion object {
        const val VERSION = 4
    }

}