package com.badahori.creatures.plugins.intellij.agenteering.caos.utils

import com.badahori.creatures.plugins.intellij.agenteering.caos.exceptions.CaosInvalidTokenLengthException
import kotlin.math.pow

@Throws(CaosInvalidTokenLengthException::class)
internal fun token(token: String): Int {
    val chars = token.toCharArray()
    if (chars.isEmpty() || chars.size > 4)
        throw CaosInvalidTokenLengthException(chars)
    return token(chars[0].toInt(), chars[1].toInt(), chars[2].toInt(), chars[3].toInt())
}

internal fun token(c1: Char, c2: Char, c3: Char, c4: Char): Int = token(c1.toInt(), c2.toInt(), c3.toInt(), c4.toInt())
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