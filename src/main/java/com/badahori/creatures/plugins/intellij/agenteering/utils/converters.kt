package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.injector.GameInterfaceName
import com.intellij.util.xmlb.Converter


internal class CaosVariantConverter : Converter<CaosVariant?>() {
    override fun toString(value: CaosVariant): String {
        return value.code
    }

    override fun fromString(value: String): CaosVariant? {
        return CaosVariant.fromVal(value).nullIfUnknown()
    }
}


internal class StringListConverter : Converter<List<String>>() {
    override fun toString(values: List<String>): String {
        var delimiterEscape = DELIMITER_ESCAPE

        // Delimiter escape is variable in case a value in the list contains the escape sequence
        while (values.any { it.contains(delimiterEscape) }) {
            delimiterEscape = "xx@$delimiterEscape@xx"
        }
        // Format for key is {delimiter marker} {delimiter} {delimiter_escape} {Delimiter_marker}
        val delimiterKey = DELIMITER_DELIMITER + DELIMITER + delimiterEscape + DELIMITER_DELIMITER
        return delimiterKey + values.joinToString(DELIMITER) { it.replace(DELIMITER, delimiterEscape) }
    }

    override fun fromString(value: String): List<String> {

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
        return values.serialize()
    }

    override fun fromString(value: String): GameInterfaceName? {
        return GameInterfaceName.fromString(value)
    }
}

internal class GameInterfaceListConverter : Converter<List<GameInterfaceName>>() {
    override fun toString(values: List<GameInterfaceName>): String {
        var delimiter = DELIMITER
        val serialized = values.map { it.serialize() }
        while (serialized.any { it.contains(delimiter) }) {
            delimiter = "#_" + delimiter + "_#"
        }
        return delimiter + DELIMITER_VALUES_DELIMITER + values.joinToString(delimiter) { it.serialize() }
    }

    override fun fromString(rawSerialized: String): List<GameInterfaceName> {
        val delimiterValuesSplit = rawSerialized.split(DELIMITER_VALUES_DELIMITER, limit = 2)
        if (delimiterValuesSplit.size < 2) {
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

    companion object {
        private const val DELIMITER_VALUES_DELIMITER = "|||"
        private const val DELIMITER = ":++__;;:!!:;;__++:"
    }
}