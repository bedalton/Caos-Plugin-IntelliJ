package com.badahori.creatures.plugins.intellij.agenteering.caos.action

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.injectorInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.UNDEF
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.substringFromEnd


data class GameInterfaceName constructor(
    val code: String?,
    internal val variant: CaosVariant?,
    val url: String,
    private val nickname: String?
) {

    constructor(code: String?, url: String, nickname: String?) : this(
        code,
        code?.let { CaosVariant.fromVal(it).nullIfUnknown() },
        url,
        nickname
    )

    constructor(variant: CaosVariant) : this(
        variant.code,
        variant,
        variant.injectorInterfaceName ?: UNDEF,
        variant.fullName
    )

    val name: String = nickname ?: variant?.fullName ?: url


    fun isVariant(aVariant: CaosVariant?): Boolean {
        if (aVariant == null || variant == null)
            return true
        return variant == aVariant || (variant.isC3DS && aVariant.isC3DS)
    }

    override fun toString(): String {
        val builder = StringBuilder()
        if (code != null) {
            builder.append(code).append(":")
        }
        builder.append(url)
        if (nickname != null)
            builder.append('[').append(nickname).append(']')
        return builder.toString()
    }

    val storageKey get() = code.orEmpty() + delimiter + name + delimiter + url

    fun keyMatches(code: String, name: String, url: String): Int {
        var out = 0
        if (code == this.code)
            out += 1
        if (name == this.name)
            out += 2
        if (url == this.url) {
            out += 2
        }
        return out
    }

    companion object {
        private val BASIC_REGEX = "([^:]+\\s*:)?\\s*([^\\[]+)(?:\\[\\s*([^]]+)]\\s*)?".toRegex()
        private const val delimiter = "x;;|||;;x"

        internal fun keyParts(key: String): Triple<String, String, String>? {
            val parts = key.split(delimiter, limit = 3)
            if (parts.size < 2) {
                return null
            }
            if (parts.size == 2)
                return Triple("", parts[0], parts[1])
            return Triple(parts[0], parts[1], parts[2])
        }

        fun fromString(text: String): GameInterfaceName? {
            val parts = BASIC_REGEX.matchEntire(text.trim())
                ?.groupValues
                ?: return null
            val code = parts.getOrNull(1)
                ?.trim()
                ?.substringFromEnd(0, 1)
                ?.nullIfEmpty()
            val url = parts.getOrNull(2)
                ?.trim()
                ?.nullIfEmpty()
                ?: return null
            val nickname = parts.getOrNull(3)
                ?.trim()
                ?.nullIfEmpty()
            return GameInterfaceName(
                code,
                url,
                nickname
            )
        }
    }
}


internal fun List<GameInterfaceName>.forKey(variant: CaosVariant?, key: String): GameInterfaceName? {
    val (code, name, url) = GameInterfaceName.keyParts(key)
        ?: return null
    return this
        .map { gameInterface ->
            Pair(gameInterface.keyMatches(code, name, url), gameInterface)
        }
        .filter { it.first > 1 }
        .nullIfEmpty()
        ?.sortedByDescending { it.first }
        ?.let { interfaces ->
            if (variant == null)
                return interfaces.first().second
            interfaces.firstOrNull()?.second?.apply {
                if (variant == this.variant || (this.variant?.isC3DS == true && variant.isC3DS))
                    return this
            }
            interfaces.firstOrNull { it.second.variant == variant }?.second
                ?: interfaces.firstOrNull { it.second.variant?.isC3DS == true && variant.isC3DS}?.second
                ?: interfaces.first().second
        }
}