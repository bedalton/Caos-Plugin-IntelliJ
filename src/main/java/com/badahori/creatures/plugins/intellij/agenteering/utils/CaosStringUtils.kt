package com.badahori.creatures.plugins.intellij.agenteering.utils

import kotlin.math.max

object CaosStringUtil {

    @JvmOverloads
    fun notNull(string: String?, defaultVal: String = ""): String {
        return string ?: defaultVal
    }

    fun upperCaseFirstLetter(string: String?): String? {
        if (string == null) {
            return null
        }
        return if (string.length < 2) {
            string.toUpperCase()
        } else string.substring(0, 1).toUpperCase() + string.substring(1)
    }

    fun substringFromEnd(string: String, start: Int, fromEnd: Int): String =
            string.substring(start, max(string.length - fromEnd, start))


    private val MULTI_SPACE_REGEX = "\\s+".toRegex()
    private val SPACES_AROUND_COMMAS = "\\s*,\\s*".toRegex()

    fun sanitizeCaosString(text: String): String {
        return text.split("\n")
                .joinToString("\n") {
                    val lineStartIndex = it.indexOfFirstNonWhitespaceCharacter()
                    val prefix: String
                    var thisText: String
                    if (lineStartIndex > 0) {
                        prefix = it.substring(0, lineStartIndex)
                        thisText = it.substring(lineStartIndex)
                    } else {
                        prefix = ""
                        thisText = it
                    }
                    thisText = thisText
                            .replace(MULTI_SPACE_REGEX, " ")
                            .replace(SPACES_AROUND_COMMAS, ",")
                    prefix + thisText.trim()
                }
                .trim()
    }
}

fun String.upperCaseFirstLetter(): String {
    return if (this.length < 2) {
        this.toUpperCase()
    } else this.substring(0, 1).toUpperCase() + this.substring(1)
}

@Suppress("unused")
fun String.substringFromEnd(start: Int, subtractFromEnd: Int): String =
        CaosStringUtil.substringFromEnd(this, start, subtractFromEnd)

fun String.repeat(times: Int): String {
    val stringBuilder = StringBuilder()
    for (i in 1..times) {
        stringBuilder.append(this)
    }
    return stringBuilder.toString()
}

internal val uppercaseSplitRegex: Regex = "(?=\\p{Lu})".toRegex()

@Suppress("unused")
fun String.splitOnUppercase(): List<String> {
    return this.split(uppercaseSplitRegex)
}

fun String?.startsAndEndsWith(start: String?, end: String?): Boolean {
    return this != null && (start == null || this.startsWith(start)) && (end == null || this.endsWith(end))
}

fun String.trimFromBeginning(vararg prefixes: String, repeatedly: Boolean = true): String {
    return this.trimFromBeginning(prefixes.toList(), repeatedly)
}

fun String.trimFromBeginning(prefixes: List<String>, repeatedly: Boolean = true): String {
    var out = this
    var changed: Boolean
    do {
        changed = false
        prefixes.forEach foreach@{ prefix ->
            if (prefix.isEmpty())
                return@foreach
            if (!out.startsWith(prefix))
                return@foreach
            out = out.substring(prefix.length)
            changed = true
        }
    } while (changed && repeatedly)
    return out
}

fun String.afterLast(sequence: String, offset: Int = 0, ignoreCase: Boolean = false): String {
    if (endsWith(sequence))
        return ""
    val lastIndex = this.lastIndexOf(sequence, offset, ignoreCase)
    if (lastIndex < 0)
        return this
    return this.substring(lastIndex + sequence.length)
}

fun String.equalsIgnoreCase(otherString: String): Boolean {
    return this.equals(otherString, true)
}

fun String.endsWithIgnoreCase(otherString: String): Boolean {
    return this.toLowerCase().endsWith(otherString.toLowerCase())
}

fun String.startsWithIgnoreCase(otherString: String): Boolean {
    return this.toLowerCase().startsWith(otherString.toLowerCase())
}

fun String.notEqualsIgnoreCase(otherString: String): Boolean {
    return !this.equals(otherString, true)
}

fun String.notEquals(otherString: String, ignoreCase: Boolean): Boolean {
    return !this.equals(otherString, ignoreCase)
}

fun String.indexOfFirstNonWhitespaceCharacter(): Int {
    val characters = toCharArray()
    for (i in 0 until length) {
        if (Character.isWhitespace(characters[i]))
            continue
        else
            return i
    }
    return -1
}

fun String.matchCase(stringToMatch: String): String {
    return when (stringToMatch.case) {
        Case.UPPER_CASE -> toUpperCase()
        Case.LOWER_CASE -> toLowerCase()
        Case.CAPITAL_FIRST -> upperCaseFirstLetter()
    }
}

fun String.matchCase(case:Case): String {
    return when (case) {
        Case.UPPER_CASE -> toUpperCase()
        Case.LOWER_CASE -> toLowerCase()
        Case.CAPITAL_FIRST -> upperCaseFirstLetter()
    }
}

val String.case: Case
    get() {
        val chars = toCharArray()
        if (chars.size < 2)
            return Case.LOWER_CASE
        if (chars[0] == chars[0].toLowerCase()) {
            return Case.LOWER_CASE
        }
        if (chars[1] == chars[1].toLowerCase()) {
            return Case.CAPITAL_FIRST
        }
        return Case.UPPER_CASE
    }

fun String.escapeHTML(): String {
    return this.replace("<", "&lt;").replace(">", "&gt;")
}

private val numberRegex = "[-+]?[0-9]+".toRegex()

fun String.toIntSafe(): Int? {
    return try {
        this.toInt()
    } catch (e: Exception) {
        null
    }
}

enum class Case {
    UPPER_CASE,
    LOWER_CASE,
    CAPITAL_FIRST
}

infix fun String?.like(other: String?): Boolean {
    return this != null && other != null && this.equalsIgnoreCase(other)
}

infix fun String?.likeAny(others: Collection<String>): Boolean {
    return others.any { this like it }
}

infix fun String?.likeAny(others: Array<String>): Boolean {
    return others.any { this like it }
}

infix fun String?.likeNone(others: Collection<String>): Boolean {
    return others.none { this like it }
}

infix fun String?.likeNone(others: Array<String>): Boolean {
    return others.none { this like it }
}

infix fun String?.notLike(other: String?): Boolean {
    return this == null || other == null || !this.equalsIgnoreCase(other)
}

fun String.wrap(maxLength: Int, newLinePrefixIn: String = ""): String {
    val joinText = "\n$newLinePrefixIn"
    return splitByLength(maxLength).joinToString(joinText)
}

fun String.splitByLength(maxLength: Int): List<String> {
    val chunks = mutableListOf<String>()
    var textLeft = this
    while (textLeft.isNotEmpty()) {
        if (textLeft.length <= maxLength)                    //if remaining string is less than length, add to list and break out of loop
        {
            chunks.add(textLeft)
            break
        }

        var chunk = textLeft.substring(0, maxLength)     //Get maxLength chunk from string.

        //if next char is a space, we can use the whole chunk and remove the space for the next line
        if (Character.isWhitespace(textLeft[maxLength])) {
            chunks.add(chunk)
            //Remove chunk plus space from original string
            textLeft = textLeft.substring(chunk.length + 1)
        } else {
            //Find last space in chunk.
            val splitIndex = chunk.lastIndexOf(' ')
            //If space exists in string,
            if (splitIndex != -1) {
                //  remove chars after space.
                chunk = chunk.substring(0, splitIndex)
            }
            //Remove chunk plus space (if found) from original string
            textLeft = textLeft.substring(chunk.length + (if (splitIndex == -1) 0 else 1))
            //Add to list
            chunks.add(chunk)
        }
    }
    return chunks
}