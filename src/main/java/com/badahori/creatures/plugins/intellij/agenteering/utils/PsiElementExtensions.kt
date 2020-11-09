package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil


fun <PsiT : PsiElement> PsiElement.hasParentOfType(parentClass:Class<PsiT>) : Boolean {
    return PsiTreeUtil.getParentOfType(this, parentClass) != null;
}

fun <PsiT : PsiElement> PsiElement.isOrHasParentOfType(parentClass:Class<PsiT>) : Boolean {
    return parentClass.isInstance(this) || PsiTreeUtil.getParentOfType(this, parentClass) != null;
}

fun PsiElement.isNotEquivalentTo(otherElement: PsiElement): Boolean = this.isEquivalentTo(otherElement)
