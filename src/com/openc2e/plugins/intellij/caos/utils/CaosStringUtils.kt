package com.openc2e.plugins.intellij.caos.utils


object Strings {

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

    fun substringFromEnd(string:String, start:Int, fromEnd:Int) : String =
            string.substring(start, string.length - fromEnd)

}

fun String.upperCaseFirstLetter() : String {
    return if (this.length < 2) {
        this.toUpperCase()
    } else this.substring(0, 1).toUpperCase() + this.substring(1)
}

@Suppress("unused")
fun String.substringFromEnd(start:Int, subtractFromEnd:Int) : String =
        Strings.substringFromEnd(this, start, subtractFromEnd)

fun String.repeat(times:Int) : String {
    val stringBuilder = StringBuilder()
    for(i in 1..times) {
        stringBuilder.append(this)
    }
    return stringBuilder.toString()
}

internal val uppercaseSplitRegex:Regex = "(?=\\p{Lu})".toRegex()

@Suppress("unused")
fun String.splitOnUppercase() : List<String> {
    return this.split(uppercaseSplitRegex)
}

fun String?.startsAndEndsWith(start: String?, end: String?): Boolean {
    return this != null && (start == null || this.startsWith(start)) && (end == null || this.endsWith(end))
}

fun String.trimFromBeginning(vararg prefixes:String, repeatedly:Boolean = true) : String {
    return this.trimFromBeginning(prefixes.toList(), repeatedly)
}

fun String.trimFromBeginning(prefixes:List<String>, repeatedly:Boolean = true) : String {
    var out = this
    var changed:Boolean
    do {
        changed = false
        prefixes.forEach foreach@{prefix ->
            if (prefix.isEmpty())
                return@foreach
            if (!out.startsWith(prefix))
                return@foreach
            out = out.substring(prefix.length)
            changed = true
        }
    } while(changed && repeatedly)
    return out
}

fun String.afterLast(sequence:String, offset:Int = 0, ignoreCase:Boolean = false) : String {
    if (endsWith(sequence))
        return ""
    val lastIndex = this.lastIndexOf(sequence, offset, ignoreCase)
    if (lastIndex < 0)
        return this
    return this.substring(lastIndex + sequence.length)
}

fun String.equalsIgnoreCase(otherString:String) : Boolean {
    return this.equals(otherString, true)
}

fun String.notEqualsIgnoreCase(otherString:String) : Boolean {
    return !this.equals(otherString, true)
}

fun String.notEquals(otherString:String, ignoreCase:Boolean) : Boolean {
    return !this.equals(otherString, ignoreCase)
}