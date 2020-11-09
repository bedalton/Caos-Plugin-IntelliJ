package com.badahori.creatures.plugins.intellij.agenteering.utils

import kotlin.contracts.contract


fun <T> Collection<T>.filterNotNull() : List<T> {
    return this.mapNotNull { it }
}
fun <R, T:Collection<R>> T?.nullIfEmpty(): T? {
    if (this == null || isEmpty())
        return null
    return this
}

fun <R, T:Collection<R>> T?.nullIfEmpty(filterNullsFirst:Boolean): T? {
    if (this == null)
        return null
    val isEmpty = if (filterNullsFirst)
        this.filterNotNull().isEmpty()
    else
        this.isEmpty()
    return if (isEmpty) null else this
}

fun <K,V> Map<K,V>?.isNullOrEmpty() : Boolean {
    contract {
        returns(true) implies (this@isNullOrEmpty != null)
    }
    return this == null || this.isEmpty()
}

fun <K,V> Map<K,V>?.isNotNullOrEmpty() : Boolean {
    contract {
        returns(true) implies (this@isNotNullOrEmpty != null)
    }
    return this != null && this.isNotEmpty()
}

fun <T> Collection<T>?.isNullOrEmpty() : Boolean {
    contract {
        returns(true) implies (this@isNullOrEmpty != null)
    }
    return this == null || this.isEmpty()
}

fun <T> Collection<T>?.isNotNullOrEmpty() : Boolean {
    contract {
        returns(true) implies (this@isNotNullOrEmpty != null)
    }
    return this != null && this.isNotEmpty()
}

fun <T, R> Collection<T>.minus(elements: Collection<T>, selector: (T) -> R?)
        = filter{ t -> elements.none{ selector(it) == selector(t) } }


public fun <T : Any> mutableListOfNotNull(vararg elements: T?): MutableList<T> = elements.filterNotNull().toMutableList()