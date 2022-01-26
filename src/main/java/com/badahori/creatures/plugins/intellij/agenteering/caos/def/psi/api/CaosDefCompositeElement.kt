package com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api

import com.intellij.psi.PsiElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.impl.containingCaosDefFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil

interface CaosDefCompositeElement : PsiElement {
    val tokenType: IElementType
}

val CaosDefCompositeElement.variants get() = containingCaosDefFile.variants

fun CaosDefCompositeElement.variantsIntersect(otherVariants:List<CaosVariant>) : Boolean {
    return variants.intersect(otherVariants).isNotEmpty()
}

fun CaosDefCompositeElement.isVariant(variant: CaosVariant) : Boolean {
    return variant in variants
}