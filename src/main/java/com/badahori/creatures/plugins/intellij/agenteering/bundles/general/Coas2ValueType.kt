package com.badahori.creatures.plugins.intellij.agenteering.bundles.general

import bedalton.creatures.util.stripSurroundingQuotes
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler.Caos2CobUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.NUMBER_REGEX
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.POS_NEG_NUMBER_REGEX
import com.badahori.creatures.plugins.intellij.agenteering.utils.FileNameUtils
import java.time.format.DateTimeFormatter

enum class CobTagFormat(val formatDescriptor: String, val validate: (value:String) -> Boolean) {
    STRING("string", ::isString),
    NUMBER("number", ::isNumber),
    SINGLE_IMAGE("image or sprite file with array access like sprite[0].spr", ::isSingleImage),
    DATE("date in the format of 2011-12-03 or 2011-12-03T10:15:30", ::isDate),
    EMAIL("email", ::isEmail),
    URL("URL", ::isURL),
    FILE("file reference", ::isFile)
}

private fun getFileName(path: String): String? {
    if (!path.contains('['))
        return path
    Caos2CobUtil.ARRAY_ACCESS_REGEX.matchEntire(path)?.groupValues?.let { groupValues ->
        return "${groupValues[1]}.${groupValues[2]}"
    }
    Caos2CobUtil.ARRAY_ACCESS_BEFORE_EXTENSION_REGEX.matchEntire(path)?.groupValues?.let { groupValues ->
        return "${groupValues[1]}.${groupValues[3]}"
    }
    return null
}

private fun isNumber(value: String): Boolean = POS_NEG_NUMBER_REGEX.matches(value)
private fun isString(value: String): Boolean = !POS_NEG_NUMBER_REGEX.matches(value)

internal fun isSingleImage(value: String): Boolean {
    val fileName = getFileName(value.stripSurroundingQuotes())
        ?: return false

    val extension = FileNameUtils
        .getExtension(fileName)
        ?.lowercase()

    // Check is sprite extension is valid
    return extension in listOf("gif", "jpeg", "jpg", "png", "spr", "s16", "c16", "bmp")
}

private fun isDate(value: String): Boolean {
    return try {
        DateTimeFormatter.ISO_LOCAL_DATE.parse(value.stripSurroundingQuotes())
        true
    } catch(e: Exception) {
        try {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(value.stripSurroundingQuotes())
            return true
        } catch (e: Exception) {
            false
        }
    }
}

val emailRegex = "[^@]+@[^.]+\\..+".toRegex()
private fun isEmail(value: String): Boolean {
    return emailRegex.matches(value.stripSurroundingQuotes())
}

fun isURL(value: String) = isString(value)


fun isFile(value: String) = getFileNameWithArrayAccess(value.stripSurroundingQuotes()) != null