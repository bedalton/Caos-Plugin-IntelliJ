package com.badahori.creatures.plugins.intellij.agenteering.caos.exceptions

open class CaosLibException(message:String, throwable: Throwable? = null)
    : kotlin.Exception(message, throwable)

open class CaosInvalidTokenLengthException(val chars:CharArray, message:String, throwable: Throwable? = null)
    : Exception(message, throwable) {
    constructor(chars:CharArray, throwable: Throwable? = null) : this(chars, "Invalid CAOS token length. Expected 4, found ${chars.size} in token: '${chars.joinToString("")}'", throwable)
}

internal fun Exception.messageOrNoneText(): String = message ?: "<none>"