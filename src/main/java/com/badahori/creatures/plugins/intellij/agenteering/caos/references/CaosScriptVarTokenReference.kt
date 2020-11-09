package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommand
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandWord
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVarToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptVarTokenGroup
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

/**
 * Find command variable reference
 */
class CaosScriptVarTokenReference(element: CaosScriptVarToken) : PsiPolyVariantReferenceBase<CaosScriptVarToken>(element, TextRange.create(0, element.text.length), true) {

    private val name:String by lazy {
        element.text.toUpperCase()
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element is CaosDefCommandWord || element is CaosDefCommand) {
            return when (element.text.toUpperCase()) {
                "VARX" -> myElement.varGroup == CaosScriptVarTokenGroup.VARx
                "VAXX" -> myElement.varGroup == CaosScriptVarTokenGroup.VAxx
                "OBVX" -> myElement.varGroup == CaosScriptVarTokenGroup.OBVx
                "OVXX" -> myElement.varGroup == CaosScriptVarTokenGroup.OVxx
                "MVXX" -> myElement.varGroup == CaosScriptVarTokenGroup.MVxx
                else -> false
            }
        }
        if (element.text.toUpperCase() != name)
            return false
        return element.containingFile.isEquivalentTo(myElement.containingFile)
    }
/*
    override fun resolve(): PsiElement? {
        val key = myElement.varGroup.value
        val variant = myElement.containingCaosFile?.variant?.let {
            if (it != CaosVariant.UNKNOWN)
                it
            else
                null
        } ?: return null
        return CaosDefCommandElementsByNameIndex
                .Instance[key, element.project].firstOrNull {
                    it.isVariant(variant)
                }
    }
*/
    override fun multiResolve(partial: Boolean): Array<ResolveResult> {
        val key = myElement.varGroup.value
        val variant = myElement.variant
                ?: return  PsiElementResolveResult.EMPTY_ARRAY
        val commands = CaosDefCommandElementsByNameIndex
                .Instance[key, element.project]
                .filter {
                    it.isVariant(variant)
                }
                .map { it.command }
        return PsiElementResolveResult.createResults(commands)
    }
}