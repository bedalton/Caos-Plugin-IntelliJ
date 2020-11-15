package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil


/**
 * Helper val to get element type for PSIElement
 */
val PsiElement.elementType: IElementType get() = node.elementType


/**
 * Get this element or one of its parents if of a given type
 */
fun <PsiT : PsiElement> PsiElement.getSelfOrParentOfType(parentClass: Class<PsiT>): PsiT? {
    if (parentClass.isInstance(this))
        return parentClass.cast(this)
    return PsiTreeUtil.getParentOfType(this, parentClass)
}

/**
 * Get a parent of this element of a given type
 */
fun <PsiT : PsiElement> PsiElement.getParentOfType(parentClass: Class<PsiT>): PsiT? {
    return PsiTreeUtil.getParentOfType(this, parentClass)
}

/**
 * Get the first direct descendent element of type
 */
fun <PsiT : PsiElement> PsiElement.getChildOfType(parentClass: Class<PsiT>): PsiT? {
    return PsiTreeUtil.getChildOfType(this, parentClass)
}

/**
 * Get the direct descendent elements of type
 */
fun <PsiT : PsiElement> PsiElement.getChildrenOfType(parentClass: Class<PsiT>): List<PsiT> {
    return PsiTreeUtil.getChildrenOfType(this, parentClass)?.filterNotNull().orEmpty()
}

/**
 * Tests whether this element, or one of its parents is of a given type
 */
fun <PsiT : PsiElement> PsiElement.isOrHasParentOfType(parentClass: Class<PsiT>): Boolean {
    if (parentClass.isInstance(this))
        return true
    return PsiTreeUtil.getParentOfType(this, parentClass) != null
}

/**
 * Gets the end offset of an element in its parent
 */
val PsiElement.endOffsetInParent: Int
    get() {
        return startOffsetInParent + textLength
    }

/**
 * Shorthand to get an elements start offset in a file
 */
val PsiElement.startOffset get() = textRange.startOffset

/**
 * Shorthand to get an elements end offset in a file
 */
val PsiElement.endOffset get() = textRange.endOffset