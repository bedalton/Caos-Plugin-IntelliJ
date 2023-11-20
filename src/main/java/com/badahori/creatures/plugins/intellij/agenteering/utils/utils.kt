package com.badahori.creatures.plugins.intellij.agenteering.utils

import java.util.*


internal const val INTELLIJ_LOG = "INTELLIJ_LOG"
internal const val ENABLE_INTELLIJ_LOGS = true

private val random: Random = Random()

fun rand(min: Int, max: Int): Int {
    val range = maxOf(min, max) - minOf(min,max)
    if (range == 0) {
        return 0
    }
    val value = minOf(min,max) + random.nextInt(range)
    if (value > max) {
        throw Exception("Invalid random in range calculation")
    }
    return value
}
