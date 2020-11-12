package com.badahori.creatures.plugins.intellij.agenteering.caos.generator

internal fun String?.nullIfEmpty():String? = if (this.isNullOrBlank()) null else this

internal fun <T> Collection<T>?.nullIfEmpty():Collection<T>? = if (this.isNullOrEmpty()) null else this