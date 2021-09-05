package com.badahori.creatures.plugins.intellij.agenteering.caos.action

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.injectorInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
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
        variant.injectorInterfaceName!!,
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

    val storageKey get() = name + delimiter + url

    fun keyMatches(key: String): Boolean {
        return key.startsWith(name + delimiter) || key.endsWith(delimiter + url)
    }

    fun keyMatches(first: String, second: String): Boolean {
        return first == name || second == url
    }

    companion object {
        private val BASIC_REGEX = "([^:]+\\s*:)?\\s*([^\\[]+)(?:\\[\\s*([^]]+)]\\s*)?".toRegex()
        private const val delimiter = "x;;|||;;x"

        internal fun keyParts(key: String): Pair<String, String>? {
            val parts = key.split(delimiter, limit = 2)
            if (parts.size != 2)
                return null
            return Pair(parts[0], parts[1])
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
    val (name, url) = GameInterfaceName.keyParts(key)
        ?: return null
    val interfaces = this
        .filter { it.isVariant(variant) }
    return interfaces
        .firstOrNull {
            it.name == name && it.url == url
        } ?: interfaces.firstOrNull {
        it.name == name
    } ?: interfaces
        .firstOrNull {
            it.url == url
        }
}