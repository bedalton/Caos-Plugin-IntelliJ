package com.openc2e.plugins.intellij.agenteering.caos.def.psi.api

import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.agenteering.caos.def.psi.impl.containingCaosDefFile
import com.openc2e.plugins.intellij.agenteering.caos.def.stubs.api.variants
import com.openc2e.plugins.intellij.agenteering.caos.lang.CaosVariant

interface CaosDefCompositeElement : PsiElement {
    fun <PsiT:PsiElement> getChildOfType(childType:Class<PsiT>):PsiT?
    fun <PsiT:PsiElement> getChildrenOfType(childType:Class<PsiT>):List<PsiT>
    fun <PsiT:PsiElement> getParentOfType(parentClass:Class<PsiT>):PsiT?
}

val CaosDefCompositeElement.variants get() = containingCaosDefFile.variants

fun CaosDefCompositeElement.variantsIntersect(otherVariants:List<CaosVariant>) : Boolean {
    return variants.intersect(otherVariants).isNotEmpty()
}

fun CaosDefCompositeElement.isVariant(variant:CaosVariant) : Boolean {
    return variant in variants
}