package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.ByteSequence


fun <T> T?.orElse(defaultValue: T): T {
    return this ?: defaultValue
}

fun <T> T?.orElse(defaultValue: () -> T): T {
    return this ?: defaultValue()
}


fun Boolean?.orFalse(): Boolean {
    return this ?: false
}

fun Boolean?.orTrue(): Boolean {
    return this ?: true
}

val Any?.canonicalName: String get() = if (this == null) "NULL" else this::class.java.canonicalName

operator fun CaosScriptFile.plus(list: List<CaosScriptFile>): List<CaosScriptFile> {
    return mutableListOf(this).apply { addAll(list) }
}

infix fun Int.hasFlag(flag: Int): Boolean = this and flag == flag

fun Int.removeFlag(flag: Int): Int = this and flag.inv()
fun Int.addFlag(flag: Int): Int = this or flag


val CaosVariant.selfOrC3IfDS: CaosVariant
    get() = if (this == CaosVariant.DS) CaosVariant.C3 else this


internal operator fun TextRange.minus(range:TextRange): TextRange? {
    val startOffset = startOffset - range.startOffset
    val endOffset = endOffset - range.endOffset
    return if (startOffset > endOffset)
        null
    else
        TextRange(startOffset, endOffset)
}

internal fun TextRange.isNeg(): Boolean {
    return startOffset < 0 || endOffset < 0
}

val Byte.char:Char get() = this.toInt().toChar()

fun ByteSequence.charAt(i: Int): Char = this.byteAt(i).toInt().toChar()

fun ByteSequence.charAtOrNull(i: Int): Char? = try {
    if (length() > i) {
        this.byteAt(i).toInt().toChar()
    } else {
        null
    }
} catch (e: Exception) {
    null
}