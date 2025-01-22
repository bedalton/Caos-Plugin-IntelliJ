package com.badahori.creatures.plugins.intellij.agenteering.att.lang

object PartNames {
    
    @JvmStatic
    fun getPartName(part: Char): String {
        return when (part.lowercaseChar()) {
            'a' -> AttMessages.message("head")
            'b' -> AttMessages.message("body")
            'c' -> AttMessages.message("left-thigh")
            'd' -> AttMessages.message("left-shin")
            'e' -> AttMessages.message("left-foot")
            'f' -> AttMessages.message("right-thigh")
            'g' -> AttMessages.message("right-shin")
            'h' -> AttMessages.message("right-foot")
            'i' -> AttMessages.message("left-humerus")
            'j' -> AttMessages.message("left-radius")
            'k' -> AttMessages.message("right-humerus")
            'l' -> AttMessages.message("right-radius")
            'm' -> AttMessages.message("tail-base")
            'n' -> AttMessages.message("tail-tip")
            'o' -> AttMessages.message("left-ear")
            'p' -> AttMessages.message("right-ear")
            'q' -> AttMessages.message("hair")
            else -> throw IndexOutOfBoundsException("Invalid part $part for getPartName()")
        }
    }
    
}