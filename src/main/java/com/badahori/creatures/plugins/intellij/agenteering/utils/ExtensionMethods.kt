package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import java.util.*
import kotlin.contracts.contract

fun String?.nullIfEmpty(): String? {
    return if (this.isNullOrBlank())
        null
    else
        this
}

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

fun String?.isNotNullOrEmpty() : Boolean {
    contract {
        returns(true) implies (this@isNotNullOrEmpty != null)
    }
    return this != null && this.isNotEmpty()
}

fun String?.isNotNullOrBlank() : Boolean {
    contract {
        returns(true) implies (this@isNotNullOrBlank != null)
    }
    return this != null && this.isNotBlank()
}

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

fun now():Long {
    return Date().time
}
val now:Long get(){
    return Date().time
}


fun <T> Collection<T>?.isNotNullOrEmpty() : Boolean {
    contract {
        returns(true) implies (this@isNotNullOrEmpty != null)
    }
    return this != null && this.isNotEmpty()
}

fun <PsiT : PsiElement> PsiElement.hasParentOfType(parentClass:Class<PsiT>) : Boolean {
    return PsiTreeUtil.getParentOfType(this, parentClass) != null;
}

fun <PsiT : PsiElement> PsiElement.isOrHasParentOfType(parentClass:Class<PsiT>) : Boolean {
    return parentClass.isInstance(this) || PsiTreeUtil.getParentOfType(this, parentClass) != null;
}

fun PsiElement.isNotEquivalentTo(otherElement:PsiElement): Boolean = this.isEquivalentTo(otherElement)

fun <T, R> Collection<T>.minus(elements: Collection<T>, selector: (T) -> R?)
        = filter{ t -> elements.none{ selector(it) == selector(t) } }

val Any.className:String? get() = this.javaClass.className


val Any.canonicalName:String? get() = this.javaClass.canonicalName