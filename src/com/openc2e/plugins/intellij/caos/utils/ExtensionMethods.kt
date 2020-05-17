package com.openc2e.plugins.intellij.caos.utils

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import java.util.*

fun String?.nullIfEmpty(): String? {
    return if (this.isNullOrBlank())
        null
    else
        this
}

fun String?.isNotNullOrEmpty() : Boolean {
    return this != null && this.isNotEmpty()
}

fun String?.isNotNullOrBlank() : Boolean {
    return this != null && this.isNotBlank()
}

fun <T> T?.orElse(defaultValue:T) : T {
    return this ?: defaultValue
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

fun <PsiT : PsiElement> PsiElement.hasParentOfType(parentClass:Class<PsiT>) : Boolean {
    return PsiTreeUtil.getParentOfType(this, parentClass) != null;
}

fun <PsiT : PsiElement> PsiElement.isOrHasParentOfType(parentClass:Class<PsiT>) : Boolean {
    return parentClass.isInstance(this) || PsiTreeUtil.getParentOfType(this, parentClass) != null;
}

fun PsiElement.isNotEquivalentTo(otherElement:PsiElement): Boolean = this.isEquivalentTo(otherElement)

fun <T, R> Collection<T>.minus(elements: Collection<T>, selector: (T) -> R?)
        = filter{ t -> elements.none{ selector(it) == selector(t) } }

