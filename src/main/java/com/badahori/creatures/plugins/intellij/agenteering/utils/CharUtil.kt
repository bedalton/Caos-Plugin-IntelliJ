package com.badahori.creatures.plugins.intellij.agenteering.utils

object CharUtil {

    @JvmStatic
    fun toCharArray(original: CharArray): Array<Char> {
        return original.toTypedArray()
    }

    @JvmStatic
    fun toCharArray(original: Array<Char>): CharArray {
        return original.toCharArray()
    }

    @JvmStatic
    fun append(original: Array<Char>, vararg chars: Char): CharArray {
        return (original + chars.toTypedArray()).toCharArray()
    }

    @JvmStatic
    fun appendUnique(original: Array<Char>, vararg chars: Char): CharArray {
        return (original + chars.toList()).toSet().toCharArray()
    }

    @JvmStatic
    fun append(original: CharArray, vararg chars: Char): CharArray {
        return (original.toList() + chars.toList()).toCharArray()
    }

    @JvmStatic
    fun appendUnique(original: CharArray, vararg chars: Char): CharArray {
        return (original.toList() + chars.toList()).toSet().toCharArray()
    }

    @JvmStatic
    @JvmName("appendCharArray")
    fun append(original: CharArray, chars: CharArray): CharArray {
        return (original.toList() + chars.toList()).toCharArray()
    }

    @JvmStatic
    @JvmName("appendCharArrayUnique")
    fun appendUnique(original: CharArray, chars: Array<Char>): CharArray {
        return (original.toList() + chars.toList()).toSet().toCharArray()
    }
}