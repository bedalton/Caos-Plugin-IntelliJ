package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandWord
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVarToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

class CaosScriptVarTokenReference(element: CaosScriptVarToken) : PsiPolyVariantReferenceBase<CaosScriptVarToken>(element, TextRange.create(0, element.name.length), true) {

    private val name:String by lazy {
        element.text.toUpperCase()
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element is CaosDefCommandWord) {
            LOGGER.info("Check if reference to DEF element: ${element.text.toUpperCase()}")
            return element in multiResolve(false).map { it.element }
        }
        LOGGER.info("IsVarToken: $name");
        if (element.text.toUpperCase() != name)
            return false
        LOGGER.info("IsMatching: $name");
        return element.containingFile.isEquivalentTo(myElement.containingFile)
    }

    override fun multiResolve(partial: Boolean): Array<ResolveResult> {
        val key = myElement.varGroup.value
        val variant = myElement.containingCaosFile?.variant?.let {
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