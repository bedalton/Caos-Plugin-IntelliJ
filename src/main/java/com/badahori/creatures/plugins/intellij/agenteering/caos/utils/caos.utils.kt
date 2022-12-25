package com.badahori.creatures.plugins.intellij.agenteering.caos.utils

import bedalton.creatures.common.util.className
import com.badahori.creatures.plugins.intellij.agenteering.caos.exceptions.CaosInvalidTokenLengthException
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.intellij.psi.PsiElement
import kotlin.math.pow

@Throws(CaosInvalidTokenLengthException::class)
internal fun token(token: String): Int {
    val chars = token.toCharArray()
    if (chars.isEmpty() || chars.size > 4)
        throw CaosInvalidTokenLengthException(chars)
    return token(chars[0].code, chars[1].code, chars[2].code, chars[3].code)
}

internal fun token(token: Int): String? {
    return try {
        "${(token shr 24 and 0xFF).toChar() }${(token shr 16 and 0xFF).toChar() }${(token shr 8 and 0xFF).toChar() }${(token and 0xFF).toChar() }"
    } catch (_: Exception) {
        null
    }
}

internal fun token(c1: Char, c2: Char, c3: Char, c4: Char): Int = token(c1.code, c2.code, c3.code, c4.code)
internal fun token(a: Int, b: Int, c: Int, d: Int) = a shl 24 or (b shl 16) or (c shl 8) or d

internal fun binaryToInteger(binary: String): Long {
    val numbers = binary.toCharArray()
    var result = 0L
    for (i in numbers.indices.reversed()) if (numbers[i] == '1') {
        val next = 2.0f.pow((numbers.size - i - 1)).toLong()
        result += next
    }
    return result
}

internal fun PsiElement.toTokenOrNull(lowercase: Boolean = true): Int? {
    return if (textLength == 4) {
        try {
            token(if (lowercase) text.lowercase() else text)
        } catch (e: Exception) {
            LOGGER.severe("Element to token failed: ${e.className}: ${e.message}")
            null
        }
    } else {
        null
    }
}