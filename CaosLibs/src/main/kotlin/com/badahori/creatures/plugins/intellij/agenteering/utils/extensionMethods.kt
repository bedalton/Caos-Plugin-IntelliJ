package com.badahori.creatures.plugins.intellij.agenteering.utils

fun String?.nullIfEmpty():String? = if (this.isNullOrBlank()) null else this

fun <T> Collection<T>?.nullIfEmpty():Collection<T>? = if (this.isNullOrEmpty()) null else this