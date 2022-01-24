package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.openapi.editor.ex.FoldingModelEx
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType


val EMPTY_PSI_ARRAY = emptyArray<PsiElement>()

val EMPTY_PSI_LIST = emptyList<PsiElement>()

/**
 * Tests whether this element's parent is of a given type
 */
fun <PsiT : PsiElement> PsiElement.hasParentOfType(parentClass:Class<PsiT>) : Boolean {
    return PsiTreeUtil.getParentOfType(this, parentClass) != null;
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
 * Inverts isEquivalentTo
 */
fun PsiElement.isNotEquivalentTo(otherElement: PsiElement): Boolean = this.isEquivalentTo(otherElement)

/**
 * Helper val to get element type for PSIElement
 */
val PsiElement.tokenType: IElementType get() = node?.elementType ?: TokenType.DUMMY_HOLDER

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


/**
 * Checks that an element is inside a folded region
 */
val PsiElement.isFolded: Boolean
    get() {
        val editor = editor
            ?: return false
        val startOffset = startOffset
        return try {
            (editor.foldingModel)
                .let {
                    (it as? FoldingModelEx)?.fetchTopLevel() ?: it.allFoldRegions
                }
                .any {
                    !it.isExpanded && startOffset in it.startOffset..it.endOffset
                }
        } catch(e:Exception) {
            return false
        }
    }

/**
 * Checks that an element is not inside a folded region
 */
val PsiElement.isNotFolded: Boolean
    get() {
        val editor = editor
            ?: return false
        return try {
            (editor.foldingModel)
                .let {
                    (it as? FoldingModelEx)?.fetchTopLevel() ?: it.allFoldRegions
                }
                .none {
                    /*!it.isExpanded &&*/ (startOffset in it.startOffset..it.endOffset || endOffset in it.startOffset..it.endOffset)
                }
        } catch(e:Exception) {
            return false
        }
    }

val PsiElement.textUppercase:String get() = text.uppercase()

