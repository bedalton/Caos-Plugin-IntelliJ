package com.badahori.creatures.plugins.intellij.agenteering.att.editor

import com.bedalton.log.Log

object RelativeAttUtil {


    fun getRelative(part: Char, point: Int): Pair<Char, Int>? {
        return when (part.lowercaseChar()) {
            'a' -> {
                when (point) {
                    0 -> Pair('b', 0)
                    1 -> null
                    2 -> Pair('o', 0)
                    3 -> Pair('p', 0)
                    4 -> Pair('q', 0)
                    else -> null
                }
            }

            'b' -> {
                when (point) {
                    0 -> Pair('a', 0)
                    1 -> Pair('c', 0)
                    2 -> Pair('f', 0)
                    3 -> Pair('i', 0)
                    4 -> Pair('k', 0)
                    5 -> Pair('n', 0)
                    else -> null
                }
            }

            // Left Thigh
            'c' -> {
                when (point) {
                    0 -> Pair('b', 1) // Body
                    1 -> Pair('d', 0) // Shin
                    else -> null
                }
            }

            // Left Shin
            'd' -> {
                when (point) {
                    0 -> Pair('c', 1) // Thigh
                    1 -> Pair('e', 0) // Foot
                    else -> null
                }
            }

            // Left Foot
            'e' -> {
                when (point) {
                    0 -> Pair('d', 1) // Shin
                    else -> null
                }
            }

            // Right Leg
            'f' -> {
                when (point) {
                    0 -> Pair('b', 2) // Body
                    1 -> Pair('g', 0) // Shin
                    else -> null
                }
            }

            // Right Shin
            'g' -> {
                when (point) {
                    0 -> Pair('f', 1) // Thigh
                    1 -> Pair('h', 0) // Foot
                    else -> null
                }
            }

            // Right Foot
            'h' -> {
                when (point) {
                    0 -> Pair('g', 1)
                    else -> null
                }
            }


            // Left Arm
            'i' -> {
                when (point) {
                    0 -> Pair('b', 3) // Body
                    1 -> Pair('j', 0) // Lower Arm
                    else -> null
                }
            }

            // Left Lower Arm
            'j' -> {
                when (point) {
                    0 -> Pair('i', 1)
                    else -> null
                }
            }

            // Right Arm
            'k' -> {
                when (point) {
                    0 -> Pair('b', 4) // Body
                    1 -> Pair('l', 0) // Lower Arm
                    else -> null
                }
            }

            // Right Lower Arm
            'l' -> {
                when (point) {
                    0 -> Pair('k', 1) // Upper arm
                    else -> null
                }
            }


            // Tail Base
            'm' -> {
                when (point) {
                    0 -> Pair('b', 5) // Body
                    1 -> Pair('n', 0) // Lower Arm
                    else -> null
                }
            }

            // Tail Root
            'n' -> {
                when (point) {
                    0 -> Pair('m', 1) // Upper arm
                    else -> null
                }
            }

            // Left Ear
            'o' -> {
                when (point) {
                    0 -> Pair('a', 2) // Upper arm
                    else -> null
                }
            }


            // Right Ear
            'p' -> {
                when (point) {
                    0 -> Pair('a', 3) // Upper arm
                    else -> null
                }
            }


            // Hair
            'q' -> {
                when (point) {
                    0 -> Pair('a', 4) // Upper arm
                    else -> null
                }
            }
            '0' -> null
            'z' -> null
            else -> {
                Log.e { "Cannot get relative ATT part for undefined part: $part" }
                null
            }
        }
    }


}