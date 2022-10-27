package com.badahori.creatures.plugins.intellij.agenteering.injector

import bedalton.creatures.util.OS
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle.message
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.injectorInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import java.net.URL

/**
 * Base class for data required by the supported CAOS injectors
 */
sealed class GameInterfaceName {
    abstract val code: String
    abstract val gameName: String?
    abstract val path: String?
    abstract val kind: String
    protected abstract val nickname: String?

    open val name: String get()  {
        val code = if (code == "AL" || code == "ANY") {
            "*"
        } else {
            code
        }
        if (nickname != null) {
            return "$code: $nickname"
        }
        if (gameName != null) {
            return "$code: $gameName"
        }
        val variant = variant
        if (variant != null && variant.isBase) {
            return "$kind: $variant"
        }
        return "$kind: $code"
    }

    val variant get() = CaosVariant.fromVal(code).nullIfUnknown()

    fun isVariant(aVariant: CaosVariant?): Boolean {
        val variant = variant

        if (aVariant == null || variant == null)
            return true

        if (aVariant == CaosVariant.ANY || variant == CaosVariant.ANY) {
            return true
        }
        return variant == aVariant || (variant.isC3DS && aVariant.isC3DS)
    }

    fun keyMatches(kind: String?, code: String, gameName: String?, path: String?, nickname: String?): Int {
        if (kind != null && kind != this.kind) {
            return 0
        }
        var out = 0
        val aVariant = CaosVariant.fromVal(code)
        if (isVariant(aVariant)) {
            out += 1
        }
        if (gameName == null || gameName == this.gameName) {
            out += 2
        }
        if (nickname == this.nickname) {
            out += 2
        }
        if (path == this.path) {
            out += 2
        }
        return out
    }

    abstract fun withCode(code: String): GameInterfaceName

    fun serialize(): String {
        return kind + TYPE_DELIMITER + asSerial()
    }

    fun getKey(): String = asSerial()

    protected abstract fun asSerial(): String

    companion object {
        private val BASIC_REGEX = "([^:]+\\s*):\\s*([^\\[]+)\\s*(?:\\[\\s*([^\\]]+)]\\s*)?".toRegex()
        private const val TYPE_DELIMITER = "__;;;;;;;;;;__"
        private const val GAME_NAME_PATH_DELIMITER = "<__;;x;;;;;;x;;__>"
        internal const val ADDITIONAL_DATA_DELIMITER = "##__xx__##"
        internal const val NULL = ";;@@null@@;;"

        /**
         * Creates a Game interface from its previously serialized string
         */
        fun fromString(text: String): GameInterfaceName? {
            if (!text.contains(TYPE_DELIMITER)) {
                return fromStringOld(text)
            }
            val parts = text.split(TYPE_DELIMITER, limit = 2)
            val kind = parts[0]
            val serial = parts.getOrNull(1)
                ?: return null
            return when (kind) {
                NativeInjectorInterface.KIND -> NativeInjectorInterface.fromString(serial)
                WineInjectorInterface.KIND -> WineInjectorInterface.fromString(serial)
                PostInjectorInterface.KIND -> PostInjectorInterface.fromString(serial)
                TCPInjectorInterface.KIND -> TCPInjectorInterface.fromString(serial)
                CorruptInjectorInterface.KIND -> CorruptInjectorInterface(serial)
                else -> null
            }
        }

        /**
         * Deserializes the legacy format for GameInterface names
         */
        private fun fromStringOld(text: String): GameInterfaceName? {
            val (code, gameName, rawPath, nickname) = defaultComponents(text)
                ?: return null

            // If no path, assume it is a native injector
            if (rawPath == null) {
                return if (OS.isWindows) {
                    NativeInjectorInterface(
                        code,
                        gameName,
                        path = null,
                        nickname,
                        false
                    )
                } else {
                    null
                }
            }

            // Naive URL test regex
            val regex = "(?:https?://)?([^:]+)(:\\d+)?(.*)".toRegex()


            if (regex.matches(rawPath)) {
                return NativeInjectorInterface(
                    code = code,
                    gameName = gameName,
                    path = rawPath,
                    nickname,
                    false
                )
            }

            return PostInjectorInterface(
                code = code,
                gameName = gameName,
                path = rawPath,
                null, // Original interface did not allow parameter names
                nickname = nickname
            )
        }

        /**
         * Takes a full serial and returns the default components without the interface type
         */
        internal fun decompose(serial: String): DefaultComponents? {
            val data = serial.split(TYPE_DELIMITER, limit = 2)
                .getOrNull(1)
                ?: return null
            return defaultComponents(data)
        }

        /**
         * Breaks up the data portion of a serialized interface.
         * The data portion all text after the type and type delimiter
         */
        internal fun defaultComponents(data: String): DefaultComponents? {
            val parts = BASIC_REGEX.matchEntire(data)
                ?.groupValues
                ?: return null
            val code = parts[1]
                .trim()
                .nullIfEmpty()
                ?: return null
            val pathParts = parts[2]
                .trim()
                .split(GAME_NAME_PATH_DELIMITER)
            val nickname = parts.getOrNull(3)
                ?.trim()
                ?.nullIfEmpty()
            val gameName = pathParts[0].trim().let { if (it == NULL) null else it }
            val path = pathParts.getOrNull(1)?.trim().let { if (it == NULL) null else it }
            return DefaultComponents(code, gameName, path, nickname)
        }

        internal fun defaultSerial(
            code: String,
            gameName: String?,
            path: String?,
            nickname: String?,
        ): String {
            val combinedPath = (gameName ?: NULL) + (path?.let { GAME_NAME_PATH_DELIMITER + it } ?: "")
            val builder = StringBuilder()
            builder.append(code).append(":")
            builder.append(combinedPath)
            if (nickname != null) {
                builder.append('[').append(nickname).append(']')
            }
            return builder.toString()
        }
    }

}

data class NativeInjectorInterface constructor(
    override val code: String,
    override val gameName: String?,
    override val path: String?,
    override val nickname: String?,
    val isDefault: Boolean,
) : GameInterfaceName() {

    override val kind get() = KIND

    override fun withCode(code: String): NativeInjectorInterface {
        return copy(code = code)
    }

    override fun asSerial(): String {
        return defaultSerial(code, gameName, path, nickname)
    }

    companion object {
        internal const val KIND = "native"
        fun fromString(serial: String): NativeInjectorInterface? {
            val (code, gameName, path, nickname) = defaultComponents(serial)
                ?: return null
            return NativeInjectorInterface(
                code.let { if (it == "AL" || it == "ANY") "*" else it },
                gameName,
                path,
                nickname,
                false
            )
        }

        fun simple(variant: CaosVariant): GameInterfaceName {
            if (variant == CaosVariant.ANY) {
                throw Exception("Cannot create a simple injector for wildcard variant")
            }
            return NativeInjectorInterface(
                variant.code,
                variant.injectorInterfaceName!!,
                null,
                variant.fullName,
                true
            )
        }
    }
}

data class WineInjectorInterface constructor(
    override val code: String,
    override val gameName: String?,
    val prefix: String,
    val creaturesDirectory: String?,
    override val nickname: String?,
) : GameInterfaceName() {

    override val kind get() = KIND


    override val path: String
        get() = prefix

    override fun asSerial(): String {
        return prefix + DELIMITER + defaultSerial(code, gameName, creaturesDirectory, nickname)
    }

    override fun withCode(code: String): GameInterfaceName {
        return copy(code = code)
    }

    companion object {
        internal const val KIND = "wine"

        private const val DELIMITER = ";;xx;;"
        fun fromString(serial: String): WineInjectorInterface? {
            val parts = serial.split(DELIMITER, limit = 2)
            val prefix = parts[0]
            val data = parts.getOrNull(1)
                ?: return null
            val (code, gameName, creaturesDirectory, nickname) = defaultComponents(data)
                ?: return null
            return WineInjectorInterface(
                code = code,
                gameName = gameName,
                prefix = prefix,
                creaturesDirectory = creaturesDirectory,
                nickname = nickname
            )
        }

        fun ensureWineExec() {

        }
    }
}

interface IsNet {


    fun getURL(variant: CaosVariant?): URL?

    companion object {
        val CODE_PLACEHOLDER = message("caos.injector.url.placeholders.code")
        val GAME_NAME_PLACEHOLDER = message("caos.injector.url.placeholders.game-name")

        /**
         * Check url for basic validity
         */
        @JvmStatic
        fun getErrorMessageIfAny(url: String): String? {
            if (url.count { it == '{' } != url.count { it == '}' }) {
                return "Unmatched curly braces in URL"
            }
            val regex = "([^:]+)://.+?".toRegex()
            if (!regex.matches(url)) {
                return message("caos.injector.dialog.url.malformed")
            }
            return null
        }

        internal fun getUrl(variant: CaosVariant? = null, rawUrl: String, gameName: String?): URL? {
            val url = substitute(variant, rawUrl, gameName)
            try {
                val temp = URL(url)
                if (temp.protocol.nullIfEmpty() != null) {
                    return temp
                }
            } catch (_: Exception) {}
            return try {
                URL("http://$url")
            } catch (_: Exception) {
                null
            }
        }

        private fun substitute(variant: CaosVariant?, thePath: String, gameNameIn: String?): String {
            if (thePath.count { it == '{' } != thePath.count { it == '}' }) {
                throw CaosConnectionException("Unmatched curly braces in URL")
            }
            // Check if path needs substitution
            if (!thePath.contains('{')) {
                return thePath
            }
            var path = thePath

            // Check for non-specific/wildcard variant
            val isWildCardVariant = variant is CaosVariant.ANY || variant is CaosVariant.UNKNOWN

            // Replace {code} with the variant code
            if (path.contains("{code}")) {
                if (isWildCardVariant || variant == null) {
                    throw CaosConnectionException("URL has wildcard components, but was requested without a concrete variant")
                }
                path = path.replace(CODE_PLACEHOLDER, variant.code)
            }
            // Replace {name} with the game name
            if (path.contains(GAME_NAME_PLACEHOLDER)) {
                val gameName = gameNameIn
                    ?: variant?.fullName?.replace("\\s".toRegex(), "")
                    ?: throw CaosConnectionException("Could not replace {name} without concrete game name")
                path = path.replace(GAME_NAME_PLACEHOLDER, gameName)
            }
            return path
        }
    }
}

/**
 * Interface for internet POST based injectors that pass CAOS in the POST body
 */
data class PostInjectorInterface constructor(
    override val code: String,
    override val gameName: String?,
    override val path: String,
    val parameterName: String?,
    override val nickname: String?,
) : GameInterfaceName(), IsNet {

    override val kind get() = KIND

    override fun getURL(variant: CaosVariant?): URL? = IsNet.getUrl(variant ?: this.variant, path, gameName)

    override fun asSerial(): String {
        val dataPath = path + (parameterName?.let { ADDITIONAL_DATA_DELIMITER + it } ?: "")
        return defaultSerial(code, gameName, dataPath, nickname)
    }

    override fun withCode(code: String): GameInterfaceName {
        return copy(code = code)
    }

    companion object {

        internal const val KIND = "post"
        internal const val defaultURL = "127.0.0.1"
        internal const val DEFAULT_PORT = 62351

        fun fromString(serial: String): GameInterfaceName? {
            val (code, gameName, rawPath, nickname) = defaultComponents(serial)
                ?: return null
            val urlParts = rawPath
                .nullIfEmpty()
                ?.split(ADDITIONAL_DATA_DELIMITER)
                ?: return null
            if (urlParts.size > 2) {
                LOGGER.severe("Too many components in serial: <$serial> component: <$rawPath>")
                return null
            }

            val url = urlParts[0]

            return PostInjectorInterface(
                code = code,
                gameName = gameName,
                path = url,
                parameterName = urlParts.getOrNull(2),
                nickname = nickname,
            )
        }
    }
}

/**
 * Gets an interface for 20kdc's TCP based injector
 */
data class TCPInjectorInterface constructor(
    override val code: String,
    override val gameName: String?,
    override val path: String,
    override val nickname: String?,
) : GameInterfaceName(), IsNet {

    override val kind get() = KIND

    override fun getURL(variant: CaosVariant?): URL? = IsNet.getUrl(variant ?: this.variant, path, gameName)

    override fun asSerial(): String {
        return defaultSerial(code, gameName, path, nickname)
    }

    override fun withCode(code: String): GameInterfaceName {
        return copy(code = code)
    }

    companion object {

        internal const val defaultURL = "127.0.0.1"
        internal const val DEFAULT_PORT = 19960

        internal const val KIND = "tcp"
        fun fromString(serial: String): GameInterfaceName? {
            val (code, gameName, rawPath, nickname) = defaultComponents(serial)
                ?: return null

            val url = rawPath
                ?: return null

            return TCPInjectorInterface(
                code = code,
                gameName = gameName,
                path = url,
                nickname = nickname
            )
        }
    }
}


/**
 * Gets the closest Game injector by its key
 */
internal fun List<GameInterfaceName>.forKey(variant: CaosVariant?, serial: String): GameInterfaceName? {
    // First simply try to deserialize the string
    GameInterfaceName.fromString(serial)?.let {
        return it
    }
    // Take the serial and return its base components broken up
    val (code, gameName, name, url) = GameInterfaceName.decompose(serial)
        // If decompose fails, try to decompose as if this is only the data portion of serial
        ?: GameInterfaceName.defaultComponents(serial)
        ?: return null

    // Map interfaces by match level and the interface itself
    val interfaces = this
        .map { gameInterface ->
            Pair(gameInterface.keyMatches(null, code, gameName, name, url), gameInterface)
        }
        .filter { it.first > 1 }
        .sortedByDescending { it.first }
        .map { it.second }

    // If there are not matching interfaces, return  null
    if (interfaces.isEmpty()) {
        return null
    }
    // If no variant, return the whatever the first item is, and hope for the best
    if (variant == null) {
        return interfaces.first()
    }

    // Get the closest match by variant
    return interfaces
        .firstOrNull { it.isVariant(variant) }
        // Nothing matches variant, so return "closest" match
        ?: interfaces.firstOrNull()
}

/**
 * Interface for internet POST based injectors that pass CAOS in the POST body
 */
class CorruptInjectorInterface(private val serial: String): GameInterfaceName() {

    // Static values for this interface
    override val code: String get() = "!"
    override val gameName: String get() = "CORRUPT"
    override val path: String? get() = null
    override val kind: String
        get() = KIND
    override val nickname: String? get() = null

    override fun withCode(code: String): GameInterfaceName {
        return CorruptInjectorInterface(serial)
    }

    override fun asSerial(): String {
        return serial
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CorruptInjectorInterface) return false

        if (serial != other.serial) return false

        return true
    }

    override fun hashCode(): Int {
        return serial.hashCode()
    }


    companion object {
        // The const
        internal const val KIND = "CORRUPT"
    }
}

internal data class DefaultComponents(
    val code: String,
    val gameName: String?,
    val path: String?,
    val nickName: String?,
)