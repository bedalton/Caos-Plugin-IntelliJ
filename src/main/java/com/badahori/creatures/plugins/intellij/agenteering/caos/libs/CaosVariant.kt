package com.badahori.creatures.plugins.intellij.agenteering.caos.libs

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings
import icons.CaosScriptIcons
import javax.swing.Icon

sealed class CaosVariant(open val code: String, open val fullName: String, open val index: Int, open val icon: Icon) {
    object C1 : CaosVariant("C1", "Creatures 1", 1, CaosScriptIcons.C1)
    object C2 : CaosVariant("C2", "Creatures 2", 2, CaosScriptIcons.C2)
    object CV : CaosVariant("CV", "Creatures Village", 3, CaosScriptIcons.CV)
    object C3 : CaosVariant("C3", "Creatures 3", 4, CaosScriptIcons.C3)
    object DS : CaosVariant("DS", "Docking Station", 5, CaosScriptIcons.DS)
    object SM : CaosVariant("SM", "Sea Monkeys", 6, CaosScriptIcons.SM)
    object UNKNOWN : CaosVariant("??", "Unknown", -1, CaosScriptIcons.MODULE_ICON)


    data class OTHER internal constructor(
        val base: CaosVariant,
        override val code: String,
        override val fullName: String,
        override val icon: Icon = CaosScriptIcons.MODULE_ICON
    ) : CaosVariant(code, fullName, -1, icon) {
        init {
            others.add(this)
        }
    }

    companion object {
        private val others: MutableList<CaosVariant> = mutableListOf()
        fun fromVal(variant: String?): CaosVariant {
            return when (variant?.uppercase()) {
                "C1" -> C1
                "C2" -> C2
                "CV" -> CV
                "C3" -> C3
                "DS" -> DS
                "SM" -> SM
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

        @Suppress("unused")
        fun registerVariant(
            base: CaosVariant,
            code: String,
            fullName: String,
            icon: Icon = CaosScriptIcons.MODULE_ICON
        ): CaosVariant {
            val existing = fromVal(code)
            if (existing != UNKNOWN)
                return existing
            return OTHER(base, code, fullName, icon)
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
    return if (this == null || this == CaosVariant.UNKNOWN)
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

fun CaosVariant?.orDefault(): CaosVariant? {
    if (this != null)
        return this
    return CaosScriptProjectSettings.variant
}

val VARIANT_OLD = listOf(C1, C2)

typealias GameVariant = CaosVariant


val CaosVariant.injectorInterfaceName: String?
    get() {
        return when (this) {
            C1, C2 -> "Vivarium"
            CV -> "Creatures Village"
            CaosVariant.C3 -> "Creatures 3"
            CaosVariant.DS -> "Docking Station"
            CaosVariant.SM -> "Sea-Monkeys"
            else -> null
        }
    }

infix fun CaosVariant?.like(other: CaosVariant?): Boolean {
    val variant = if (this is CaosVariant.OTHER)
        this.base
    else
        this
    val otherVariant = if (other is CaosVariant.OTHER)
        other.base
    else
        other

    if (variant == null && otherVariant == null) {
        return true
    }
    if (variant == null || otherVariant == null)
        return false

    if (variant == otherVariant)
        return true

    return variant.isC3DS && otherVariant.isC3DS
}

infix fun CaosVariant?.likeOrNull(other: CaosVariant?): Boolean {
    val variant = if (this is CaosVariant.OTHER)
        this.base
    else
        this
    val otherVariant = if (other is CaosVariant.OTHER)
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
    val variant = if (this is CaosVariant.OTHER)
        this.base
    else
        this
    val otherVariant = if (other is CaosVariant.OTHER)
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
    val variant = if (this is CaosVariant.OTHER)
        this.base
    else
        this
    val otherVariant = if (other is CaosVariant.OTHER)
        other.base
    else
        other

    if (variant == null || otherVariant == null)
        return false
    if (variant == otherVariant)
        return false
    return !(variant.isC3DS && otherVariant.isC3DS)
}


val CaosVariant?.validSpriteExtensions: List<String> get() {
    return when(this) {
        C1 -> listOf("spr")
        C2 -> listOf("s16")
        CV, C3, DS -> listOf("s16", "c16")
        else -> listOf("spr", "s16", "c16")
    }
}