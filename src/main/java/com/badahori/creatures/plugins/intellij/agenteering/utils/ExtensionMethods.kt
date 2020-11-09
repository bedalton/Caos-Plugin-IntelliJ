package com.badahori.creatures.plugins.intellij.agenteering.utils


fun <T> T?.orElse(defaultValue:T) : T {
    return this ?: defaultValue
}

fun <T> T?.orElse(defaultValue:()->T) : T {
    return this ?: defaultValue()
}


fun Boolean?.orFalse() : Boolean {
    return this ?: false
}

fun Boolean?.orTrue() : Boolean {
    return this ?: true
}

val Any?.className:String get() = if (this == null) "NULL" else this::class.java.simpleName

val Any?.canonicalName:String get() = if (this == null) "NULL" else this::class.java.canonicalName

