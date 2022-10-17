package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.badahori.creatures.plugins.intellij.agenteering.injector.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
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
        return values.joinToString(DELIMITER)
    }

    override fun fromString(value: String): List<String> {
        return value.split(DELIMITER).toList()
    }
    companion object {
        private const val DELIMITER = "\n"
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