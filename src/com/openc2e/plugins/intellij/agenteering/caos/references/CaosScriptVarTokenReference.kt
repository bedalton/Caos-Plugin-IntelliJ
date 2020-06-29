package com.openc2e.plugins.intellij.agenteering.caos.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.openc2e.plugins.intellij.agenteering.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.openc2e.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptNamedVar
import com.openc2e.plugins.intellij.agenteering.caos.psi.api.CaosScriptVarToken

class CaosScriptVarTokenReference(element:CaosScriptVarToken) : PsiPolyVariantReferenceBase<CaosScriptVarToken>(element, TextRange.create(0, element.textLength)) {

    private val name:String by lazy {
        element.text
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element !is CaosScriptNamedVar) {
            return false
        }
        if (element.name != name)
            return false
        return element.containingFile.isEquivalentTo(myElement.containingFile)
    }

    override fun multiResolve(partial: Boolean): Array<ResolveResult> {
        val key = myElement.varGroup.value
        val variant = myElement.containingCaosFile.variant.let {
            if (it != CaosVariant.UNKNOWN)
                it
            else
                null
        } ?: return emptyArray()
        val commands = CaosDefCommandElementsByNameIndex
                .Instance[key, element.project]
                .filter {
                    it.isVariant(variant)
                }
                .map { it.command }
        return PsiElementResolveResult.createResults(commands)
    }
}