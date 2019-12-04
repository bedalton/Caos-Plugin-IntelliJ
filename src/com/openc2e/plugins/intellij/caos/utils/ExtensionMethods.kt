package brightscript.intellij.utils

import com.intellij.psi.PsiElement
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

fun PsiElement.isNotEquivalentTo(otherElement:PsiElement): Boolean = this.isEquivalentTo(otherElement)

fun <T, R> Collection<T>.minus(elements: Collection<T>, selector: (T) -> R?)
        = filter{ t -> elements.none{ selector(it) == selector(t) } }