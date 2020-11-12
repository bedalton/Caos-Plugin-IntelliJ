package com.badahori.creatures.plugins.intellij.agenteering.utils


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


infix fun String?.like(otherString:String?) : Boolean {
    if (this == null || otherString == null)
        return false
    return this.equals(otherString, true)
}

infix fun String?.notLike(otherString:String?) : Boolean {
    if (this == null || otherString == null)
        return true
    return !this.equals(otherString, true)
}

