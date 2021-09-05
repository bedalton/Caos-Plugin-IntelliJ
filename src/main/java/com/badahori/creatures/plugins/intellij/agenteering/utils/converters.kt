package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.badahori.creatures.plugins.intellij.agenteering.caos.action.GameInterfaceName
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
        return values.toString()
    }

    override fun fromString(value: String): GameInterfaceName? {
        return GameInterfaceName.fromString(value)
    }
    companion object {
        private const val DELIMITER = ":;;::;;:"
    }
}

internal class GameInterfaceListConverter : Converter<List<GameInterfaceName>>() {
    override fun toString(values: List<GameInterfaceName>): String {
        return values.joinToString(DELIMITER)
    }

    override fun fromString(value: String): List<GameInterfaceName> {
        return value
            .split(DELIMITER)
            .mapNotNull {
                GameInterfaceName.fromString(it)
            }
    }
    companion object {
        private const val DELIMITER = ":;;::;;:"
    }
}