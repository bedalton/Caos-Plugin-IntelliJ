package com.badahori.creatures.plugins.intellij.agenteering.utils

import kotlin.math.abs

@Suppress("unused")
object ArrayUtil {
    @JvmStatic
    fun inArray(array: ByteArray, item: Byte): Boolean {
        return item in array
    }

    @JvmStatic
    fun inArray(array: CharArray, item: Char): Boolean {
        return item in array
    }

    @JvmStatic
    fun intersects(array: CharArray, vararg item: Char): Boolean {
        return item.toSet().intersect(array.toSet()).isNotEmpty()
    }

    @JvmStatic
    fun intersects(array: CharArray, item: Array<Char>): Boolean {
        return item.toSet().intersect(array.toSet()).isNotEmpty()
    }

    @JvmStatic
    fun intersects(array: CharArray, item: List<Char>): Boolean {
        return item.toSet().intersect(array.toSet()).isNotEmpty()
    }

    @JvmStatic
    fun inArray(array: ShortArray, item: Short): Boolean {
        return item in array
    }

    @JvmStatic
    fun intersects(array: ShortArray, vararg item: Short): Boolean {
        return item.toSet().intersect(array.toSet()).isNotEmpty()
    }

    @JvmStatic
    fun inArray(array: IntArray, item: Int): Boolean {
        return item in array
    }

    @JvmStatic
    fun intersects(array: IntArray, vararg item: Int): Boolean {
        return item.toSet().intersect(array.toSet()).isNotEmpty()
    }

    @JvmStatic
    fun inArray(array: LongArray, item: Long): Boolean {
        return item in array
    }

    @JvmStatic
    fun intersects(array: LongArray, vararg item: Long): Boolean {
        return item.toSet().intersect(array.toSet()).isNotEmpty()
    }

    @JvmStatic
    fun inArray(array: DoubleArray, item: Double): Boolean {
        return array.any { it == item }
    }

    @JvmStatic
    fun intersects(array: DoubleArray, vararg item: Double): Boolean {
        return item.toSet().intersect(array.toSet()).isNotEmpty()
    }

    @JvmStatic
    fun intersectsWithError(array: DoubleArray, error: Double, vararg items: Double): Boolean {
        val arrayAsSet = array.toSet()
        val itemsAsSet = items.toSet()
        for (item in itemsAsSet) {
            for (value in arrayAsSet) {
                if (abs(value - item) < error) {
                    return true
                }
            }
        }
        return false
    }

    @JvmStatic
    fun inArray(array: DoubleArray, item: Double, error: Double): Boolean {
        for (value in array) {
            if (abs(value - item) < error) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun inArray(array: FloatArray, item: Float): Boolean {
        return array.any { it == item }
    }

    @JvmStatic
    fun inArray(array: FloatArray, item: Float, error: Float): Boolean {
        for (value in array) {
            if (abs(value - item) < error) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun intersectsWithError(array: FloatArray, error: Float, vararg items: Float): Boolean {
        val arrayAsSet = array.toSet()
        val itemsAsSet = items.toSet()
        for (item in itemsAsSet) {
            for (value in arrayAsSet) {
                if (abs(value - item) < error) {
                    return true
                }
            }
        }
        return false
    }


    @JvmStatic
    fun intersectsWithError(array: FloatArray, error: Double, vararg items: Float): Boolean {
        val arrayAsSet = array.toSet()
        val itemsAsSet = items.toSet()
        for (item in itemsAsSet) {
            for (value in arrayAsSet) {
                if (abs(value - item) < error) {
                    return true
                }
            }
        }
        return false
    }

    @JvmStatic
    inline fun <reified T> inArray(array: Array<T>, item: T): Boolean {
        return item in array
    }

    @JvmStatic
    inline fun <reified T> intersects(array: Array<T>, vararg items: T): Boolean {
        return items.toSet().intersect(array.toSet()).isNotEmpty()
    }


    @JvmStatic
    inline fun <reified T> intersects(array: List<T>, vararg items: T): Boolean {
        return items.toSet().intersect(array.toSet()).isNotEmpty()
    }

    @JvmStatic
    @JvmName("intersectsArray")
    inline fun <reified T> intersects(array: Array<T>, items: Array<T>): Boolean {
        return items.toSet().intersect(array.toSet()).isNotEmpty()
    }

    @JvmStatic
    @JvmName("intersectsArray")
    inline fun <reified T> intersects(array: List<T>, items: Array<T>): Boolean {
        return items.toSet().intersect(array.toSet()).isNotEmpty()
    }

    @JvmStatic
    inline fun <reified T> intersects(array: List<T>, items: List<T>): Boolean {
        return items.toSet().intersect(array.toSet()).isNotEmpty()
    }

    @JvmStatic
    inline fun <reified T> intersects(array: Array<T>, items: List<T>): Boolean {
        return items.toSet().intersect(array.toSet()).isNotEmpty()
    }


}