@file:OptIn(ExperimentalSerializationApi::class, ExperimentalSerializationApi::class)
@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.caos.libs

import bedalton.creatures.structs.like
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.settings
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import icons.CaosScriptIcons
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import javax.swing.Icon


@Serializable(with = CaosVariantSerializer::class)
sealed class CaosVariant(open val code: String, open val fullName: String, open val index: Int, open val icon: Icon) {
    object C1 : CaosVariant("C1", "Creatures 1", 1, CaosScriptIcons.C1)
    object C2 : CaosVariant("C2", "Creatures 2", 2, CaosScriptIcons.C2)
    object CV : CaosVariant("CV", "Creatures Village", 3, CaosScriptIcons.CV)
    object C3 : CaosVariant("C3", "Creatures 3", 4, CaosScriptIcons.C3)
    object DS : CaosVariant("DS", "Docking Station", 5, CaosScriptIcons.DS)
    object SM : CaosVariant("SM", "Sea Monkeys", 6, CaosScriptIcons.SM)
    object ANY : CaosVariant("AL", "Any Variant", -1, CaosScriptIcons.MODULE_ICON)
    object UNKNOWN : CaosVariant("??", "Unknown", -1, CaosScriptIcons.MODULE_ICON)


    data class OTHER internal constructor(
        internal val baseCode: String,
        override val code: String,
        override val fullName: String,
        val iconPath: String? = null,
    ) : CaosVariant(code, fullName, -1, iconPath?.let { IconLoader.getIcon(iconPath) } ?: CaosScriptIcons.MODULE_ICON) {


        val base: CaosVariant by lazy {
            fromVal(baseCode).nullIfUnknown()
                ?: throw Exception("Base variant not set to a known variant")
        }

        init {
            if (base is OTHER) {
                throw Exception("Cannot create CaosVariant.OTHER based on another non-standard OTHER variant")
            }
            others.add(this)
        }
    }

    fun isDefaultInjectorName(name: String): Boolean {
        return name  == this.injectorInterfaceName
    }

    fun isDefaultInjectorName(name: String?, ignoreCase: Boolean): Boolean {
        return name?.equals(this.injectorInterfaceName, ignoreCase) == true
    }

    fun getDefaultInjectorInterfaceName(): String? {
        return this.injectorInterfaceName
    }

    companion object {
        private val others: MutableList<CaosVariant> = mutableListOf()
        @JvmStatic
        fun fromVal(variant: String?): CaosVariant {
            return when (variant?.uppercase()) {
                "C1" -> C1
                "C2" -> C2
                "CV" -> CV
                "C3" -> C3
                "DS" -> DS
                "SM" -> SM
                "*", "ANY", "AL" -> ANY
                else -> others.firstOrNull { it.code == variant } ?: UNKNOWN
            }
        }


        val baseVariants = listOf(
            C1,
            C2,
            CV,
            C3,
            DS,
            DS
        )

        @JvmStatic
        @Suppress("unused")
        fun registerVariant(
            base: String,
            code: String,
            fullName: String,
            iconPath: String? = null,
        ): CaosVariant {
            val existing = fromVal(code)
            if (existing != UNKNOWN)
                return existing
            return OTHER(base, code, fullName, iconPath)
        }
    }

    val isOld: Boolean
        get() {
            return this in VARIANT_OLD
        }

    val isNotOld: Boolean
        get() {
            return this !in VARIANT_OLD
        }

    val isC3DS get() = this == C3 || this == DS


    val isBase: Boolean get() {
        return this !is ANY && this !is OTHER && this != UNKNOWN;
    }

    override fun toString(): String {
        return code
    }

    operator fun compareTo(version: Int): Int {
        return when {
            index - version < 0 -> -1
            index - version > 0 -> 1
            else -> 0
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CaosVariant) return false

        if (code != other.code)
            return false

        return true
    }

    override fun hashCode(): Int {
        return code.hashCode()
    }
}

fun CaosVariant?.nullIfUnknown(): CaosVariant? {
    return if (this == null || this == UNKNOWN)
        null
    else
        this
}


fun CaosVariant?.nullIfNotConcrete(): CaosVariant? {
    return if (this == null || this == UNKNOWN || this == ANY)
        null
    else
        this
}

fun <T> CaosVariant.ifOld(callback: CaosVariant.() -> T): T {
    return callback()
}

fun <T> CaosVariant.ifNew(callback: CaosVariant.() -> T): T {
    return callback()
}

fun CaosVariant?.orDefault(project: Project): CaosVariant? {
    if (this != null)
        return this
    return project.settings.let {
        it.lastVariant ?: it.defaultVariant
    }
}

val VARIANT_OLD = listOf(C1, C2)

typealias GameVariant = CaosVariant


val CaosVariant.injectorInterfaceName: String?
    get() {
        return when (this) {
            C1, C2 -> "Vivarium"
            CV -> "Creatures Village"
            C3 -> "Creatures 3"
            DS -> "Docking Station"
            SM -> "Sea-Monkeys"
            ANY -> null
            else -> null
        }
    }

infix fun CaosVariant?.like(other: CaosVariant?): Boolean {

    if (this == ANY || other == ANY) {
        return true
    }

    val variant = if (this is OTHER) {
        this.base
    } else {
        this
    }
    val otherVariant = if (other is OTHER) {
        other.base
    } else {
        other
    }

    if (variant == null && otherVariant == null) {
        return true
    }
    if (variant == null || otherVariant == null) {
        return false
    }

    if (variant == otherVariant) {
        return true
    }

    return variant.isC3DS && otherVariant.isC3DS
}

infix fun CaosVariant?.likeOrNull(other: CaosVariant?): Boolean {
    val variant = if (this is OTHER)
        this.base
    else
        this
    val otherVariant = if (other is OTHER)
        other.base
    else
        other

    if (variant == null || otherVariant == null)
        return true
    if (variant == otherVariant)
        return true
    return variant.isC3DS && otherVariant.isC3DS
}


infix fun CaosVariant?.notLike(other: CaosVariant?): Boolean {
    val variant = if (this is OTHER)
        this.base
    else
        this
    val otherVariant = if (other is OTHER)
        other.base
    else
        other

    if (variant == null && otherVariant == null) {
        return false
    }
    if (variant == null || otherVariant == null)
        return true

    if (variant == otherVariant)
        return false

    return !(variant.isC3DS && otherVariant.isC3DS)
}

infix fun CaosVariant?.notLikeOrNull(other: CaosVariant?): Boolean {
    val variant = if (this is OTHER)
        this.base
    else
        this
    val otherVariant = if (other is OTHER)
        other.base
    else
        other

    if (variant == null || otherVariant == null)
        return false
    if (variant == otherVariant)
        return false
    return !(variant.isC3DS && otherVariant.isC3DS)
}


val CaosVariant?.validSpriteExtensions: Set<String>
    get() {
        return when (this) {
            C1 -> setOf("spr")
            C2 -> setOf("s16")
            CV, C3, DS -> setOf("s16", "c16")
            else -> setOf("spr", "s16", "c16")
        }
    }


/**
 * CAOS Variant serializer
 * Allows me to serialize back to the original objects
 */
internal object CaosVariantSerializer : KSerializer<CaosVariant> {
    override val descriptor: SerialDescriptor by lazy {
        buildClassSerialDescriptor("CaosVariant") {
            element<String>("code")
            element<Int>("index")
            // These 3 only set on CaosVariant.OTHER
            element<String?>("fullName")
            element<String>("base")
            element<String?>("icon")
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): CaosVariant {
        return decoder.decodeStructure(descriptor) {

            // Set for everything
            var code: String? = null
            var index: Int? = null

            // These 3 only set on CaosVariant.OTHER
            var fullName: String? = null
            var baseCode: String? = null
            var iconPath: String? = null
            loop@ while (true) {
                when (val nextId = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    // Base information
                    0 -> code = decodeStringElement(descriptor, nextId)
                    1 -> index = decodeIntElement(descriptor, nextId)

                    // baseCode and Icon Path are only set with CaosVariant.OTHER
                    2 -> fullName = decodeStringElement(descriptor, nextId)
                    3 -> baseCode = decodeStringElement(descriptor, nextId)
                    4 -> iconPath = decodeNullableSerializableElement(descriptor, nextId, String.serializer().nullable)

                    // Catch unknown indices
                    // TODO see if we should just ignore this
                    else -> throw SerializationException("Unexpected index $index when deserializing CAOS Variant in CAOS Plugin")
                }
            }

            if (code == null) {
                throw SerializationException("Could not deserialize CAOS variant reference without code")
            }
            if (index == null) {
                throw SerializationException("Could not deserialize CAOS variant reference without variant index")
            }
            if (index !in -1..6) {
                throw SerializationException("Could not deserialize CAOS variant. Invalid variant index $index")
            }
            val variant = when (index) {
                1 -> C1
                2 -> C2
                3 -> CV
                4 -> C3
                5 -> DS
                6 -> SM
                else -> {
                    if (baseCode == null) {
                        throw SerializationException("Could not deserialize non-standard CAOS variant. Base VARIANT not set")
                    }
                    if (fullName == null) {
                        throw SerializationException("Could not deserialize non-standard CAOS variant. Full name cannot be null")
                    }
                    OTHER(
                        baseCode = baseCode,
                        code = code,
                        fullName = fullName,
                        iconPath = iconPath
                    )
                }
            }
            if (variant !is OTHER && variant.code != code) {
                throw SerializationException(
                    "Could not deserialize non-standard CAOS variant. " +
                            "Code and Index do not match. Code for index: ${variant.code}; Deserialized code value: $code"
                )
            }
            variant
        }
    }

    override fun serialize(encoder: Encoder, value: CaosVariant) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.code)
            encodeStringElement(descriptor, 1, value.fullName)
            encodeIntElement(descriptor, 2, value.index)
            if (value is OTHER) {
                encodeNullableSerializableElement(descriptor, 3, String.serializer().nullable, value.iconPath)
                encodeStringElement(descriptor, 4, value.baseCode)
            }
        }
    }

}