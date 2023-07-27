package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosApplicationSettingsService.CaosApplicationSettings
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosProjectSettingsService
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosProjectSettingsService.CaosProjectSettings
import com.badahori.creatures.plugins.intellij.agenteering.injector.GameInterfaceName
import com.bedalton.common.util.className
import com.bedalton.io.bytes.decodeBase64
import com.bedalton.io.bytes.toBase64
import com.intellij.util.io.decodeBase64
import com.intellij.util.xmlb.Converter
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
}

internal class CaosVariantConverter : Converter<CaosVariant?>() {
    override fun toString(value: CaosVariant): String {
        return value.code
    }

    override fun fromString(value: String): CaosVariant? {
        return CaosVariant.fromVal(value).nullIfUnknown()
    }
}

internal class ProjectSettingsConverter: JsonToXMLStringConverter<CaosProjectSettings>() {
    override val serializer: SerializationStrategy<CaosProjectSettings>
        get() = CaosProjectSettings.serializer()
    override val deserializer: DeserializationStrategy<CaosProjectSettings>
        get() = CaosProjectSettings.serializer()

}


internal class ApplicationSettingsConverter: JsonToXMLStringConverter<CaosApplicationSettings>() {
    override val deserializer: DeserializationStrategy<CaosApplicationSettings>
        get() = CaosApplicationSettings.serializer()
    override val serializer: SerializationStrategy<CaosApplicationSettings>
        get() = CaosApplicationSettings.serializer()
}


internal class StringListConverter : Converter<List<String>>() {
    override fun toString(values: List<String>): String {
        val rawJSON = GameInterfaceName.json.encodeToString<Array<String>>(values.toTypedArray())
        return rawJSON.encodeToByteArray().toBase64()
    }

    override fun fromString(value: String): List<String>? {
        val decoded = try {
            value.trim().decodeBase64().decodeToString()
        } catch (e: Exception) {
            value.trim()
        }

        if (decoded.startsWith('[') && decoded.endsWith(']')) {
            return try {
                json.decodeFromString<List<String>>(decoded).toList()
            } catch (e: Exception) {
                LOGGER.severe("Failed to deserialize JSON string; ${e.className}:${e.message}")
                null
            }
        }
        return try {
            fromStringOld(decoded)
        } catch (e: Exception) {
            fromStringOld(value)
        }
    }

    private fun fromStringOld(value: String): List<String> {

        // Get delimiter components
        val components: List<String?> = if (value.startsWith(DELIMITER_DELIMITER)) {
            value.substring(DELIMITER_DELIMITER.length)
                .split(DELIMITER_DELIMITER, limit = 2)
                .let {
                    if (it.size != 2) {
                        listOf(null, value)
                    } else {
                        it
                    }
                }
        } else {
            listOf(null, value)
        }

        // Get delimiter and delimiter escape strings
        val delimiter = "" + (components[0]?.getOrNull(0) ?: DELIMITER)
        val delimiterEscape: String? = components[0]?.substring(1)

        // Split string with delimiter
        val raws = (components[1] ?: value)
            .split(delimiter)

        // Unescape and return
        return if (delimiterEscape == null) {
            raws
        } else {
            raws.map { it.replace(delimiterEscape, delimiter, false) }
        }
    }

    companion object {
        private const val DELIMITER = "\n"
        private const val DELIMITER_DELIMITER = "@;_;_;@"
        private const val DELIMITER_ESCAPE = "xx(@@(\$_\$_\$_$)@@)xx"
    }
}

internal class GameInterfaceConverter : Converter<GameInterfaceName?>() {
    override fun toString(values: GameInterfaceName): String {
        return values.toJSON().toByteArray(Charsets.UTF_8).toBase64()
    }

    override fun fromString(value: String): GameInterfaceName? {
        val decoded = try {
            decodeBase64(value).decodeToString()
        } catch (_: Exception) {
            value
        }
        return GameInterfaceName.fromString(decoded)
            ?: GameInterfaceName.fromString(value)
    }
}

internal object GameInterfaceListConverter : Converter<List<GameInterfaceName>>() {


    private const val DELIMITER_VALUES_DELIMITER = "|||"
    private const val DELIMITER = ":++__;;:!!:;;__++:"

    override fun toString(values: List<GameInterfaceName>): String? {
        val rawJSON = GameInterfaceName.json.encodeToString<Array<GameInterfaceName>>(values.toTypedArray())
        return rawJSON.encodeToByteArray().toBase64()
    }

    override fun fromString(rawSerialized: String): List<GameInterfaceName> {
        val decoded = try {
            decodeBase64(rawSerialized).decodeToString()
        } catch (_: Exception) {
            rawSerialized
        }
        if (decoded.startsWith('[') && decoded.contains('{') && decoded.endsWith(']')) {
            try {
                return GameInterfaceName.json.decodeFromString<Array<GameInterfaceName>>(decoded).toList()
            } catch (e: Exception) {
                LOGGER.info("Failed to deserialize array of game interfaces: ${e.className}: ${e.message}")
            }
        }
        val delimiterValuesSplit = decoded.split(DELIMITER_VALUES_DELIMITER, limit = 2)
        if (delimiterValuesSplit.size < 2) {
            LOGGER.info("Bad Raw Serialized CAOS INJECTOR game name Data: <$decoded>")
            return emptyList()
        }
        if (delimiterValuesSplit[0].isBlank()) {
            return emptyList()
        }
        return delimiterValuesSplit[1]
            .split(delimiterValuesSplit[0])
            .mapNotNull {
                GameInterfaceName.fromString(it)
            }
    }
}



abstract class JsonToXMLStringConverter<T>: Converter<T>() {

    abstract val serializer: SerializationStrategy<T>
    abstract val deserializer: DeserializationStrategy<T>

    override fun fromString(value: String): T? {
        val raw = value
            .trim()
            .nullIfEmpty()
            ?: return null
        val jsonString: String = when {
            raw.startsWith('{') && raw.endsWith('}') -> raw
            raw.startsWith('[') && raw.endsWith(']') -> raw
            else -> try {
                value.decodeBase64().decodeToString()
            } catch (e: Exception) {
                LOGGER.severe("Failed to decode BASE64 encoded string. Proceeding as plain text; ${e.className}:${e.message}\n${e.stackTraceToString()}")
                return null
            }
        }
        return try {
            json.decodeFromString(deserializer, jsonString)
        } catch (e: Exception) {
            LOGGER.severe("Failed to decode json using ${deserializer.className}\n${e.className}:${e.message}\n${e.stackTraceToString()}")
            null
        }
    }

    override fun toString(value: T & Any): String? {
        return try {
            json.encodeToString(serializer, value)
                .also {
                    LOGGER.info("Serializing with ${this.className} With JSON;\n$it")
                }
                .encodeToByteArray()
                .toBase64()
        } catch (e: Exception) {
            LOGGER.severe("Failed to encode ${value.className} to JSON -> BASE64 encoded string.\n${e.className}:${e.message}\n${e.stackTraceToString()}")
            null
        }
    }

}