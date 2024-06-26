@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.utils

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlin.contracts.contract


fun <T> Iterable<T?>.filterNotNull(): List<T> {
    val list: MutableList<T> = mutableListOf()
    for (item in this)
        if (item != null)
            list.add(item)
    return list
}

fun <R, T : Collection<R>> T?.nullIfEmpty(): T? {
    if (this == null || isEmpty())
        return null
    return this
}

fun <R, T : Collection<R>> T?.nullIfEmpty(filterNullsFirst: Boolean): T? {
    if (this == null)
        return null
    val isEmpty = if (filterNullsFirst)
        this.filterNotNull().isEmpty()
    else
        this.isEmpty()
    return if (isEmpty) null else this
}

fun <K, V> Map<K, V>?.isNullOrEmpty(): Boolean {
    contract {
        returns(true) implies (this@isNullOrEmpty != null)
    }
    return this == null || this.isEmpty()
}

fun <K, V> Map<K, V>?.isNotNullOrEmpty(): Boolean {
    contract {
        returns(true) implies (this@isNotNullOrEmpty != null)
    }
    return this != null && this.isNotEmpty()
}

fun <T> Collection<T>?.isNotNullOrEmpty(): Boolean {
    contract {
        returns(true) implies (this@isNotNullOrEmpty != null)
    }
    return this != null && this.isNotEmpty()
}

inline fun <T, R> Collection<T>.minus(elements: Collection<T>, selector: (T) -> R?) =
    filter { t -> elements.none { selector(it) == selector(t) } }


fun <T : Any> mutableListOfNotNull(vararg elements: T?): MutableList<T> = elements.filterNotNull().toMutableList()

val <T : Any> Iterator<T>.values: List<T>
    get() {
        val out = mutableListOf<T>()
        while (hasNext())
            out.add(next())
        return out
    }


fun <T> List<T>.equalIgnoringOrder(other: List<T>): Boolean {
    if (other.size != size)
        return false
    val otherCopy: MutableList<T> = ArrayList(other)
    for (e in this) {
        if (e !in otherCopy)
            return false
        otherCopy.remove(e)
    }
    return true
}


fun <T> Collection<T>.equalIgnoringOrder(other: Array<T>): Boolean {
    if (other.size != size)
        return false
    val otherCopy: MutableList<T> = other.toMutableList()
    for (e in this) {
        if (e !in otherCopy)
            return false
        otherCopy.remove(e)
    }
    return true
}

fun <T> Array<T>.equalIgnoringOrder(other: Array<T>): Boolean {
    if (other.size != size)
        return false
    val otherCopy: MutableList<T> = other.toMutableList()
    for (e in this) {
        if (e !in otherCopy)
            return false
        otherCopy.remove(e)
    }
    return true
}

fun IntArray.equalIgnoringOrder(other: IntArray): Boolean {
    if (other.size != size)
        return false
    val otherCopy: MutableList<Int> = other.toMutableList()
    for (e in this) {
        if (e !in otherCopy)
            return false
        otherCopy.remove(e)
    }
    return true
}

infix fun <T> Collection<T>.likeAny(other: Collection<T>): Boolean {
    return this.intersect(other.toSet()).isNotEmpty()
}

infix fun <T> Collection<T>.likeAny(other: Array<T>): Boolean {
    return this.intersect(other.toSet()).isNotEmpty()
}


infix fun <T> Array<T>.likeAny(other: Collection<T>): Boolean {
    return this.intersect(other.toSet()).isNotEmpty()
}

infix fun <T> Array<T>.likeAny(other: Array<T>): Boolean {
    return this.intersect(other.toSet()).isNotEmpty()
}

internal val <T> Collection<T>.firstIfOnlyOne: T?
    get() {
        return if (this.size == 1)
            first()
        else
            null
    }

internal val <T> Array<T>.firstIfOnlyOne: T?
    get() {
        return if (this.size == 1)
            first()
        else
            null
    }

internal suspend fun <A, B> Array<A>.mapAsync(f: suspend (A) -> B): List<B> {
    return map { GlobalScope.async { f(it) } }.awaitAll()
}

internal suspend fun <A, B> Iterable<A>.mapAsync(f: suspend (A) -> B): List<B> {
    return map { GlobalScope.async { f(it) } }.awaitAll()
}

internal fun <K, V> MutableMap<K, MutableList<V>>.put(key: K, value: V) {
    set(key, value)
}

internal operator fun <K, V> MutableMap<K, MutableList<V>>.set(key: K, value: V) {
    get(key)?.let {
        it.add(value)
        return
    }
    put(key, mutableListOf(value))
}