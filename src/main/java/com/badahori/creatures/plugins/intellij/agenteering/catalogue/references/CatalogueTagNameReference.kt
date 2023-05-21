package com.badahori.creatures.plugins.intellij.agenteering.catalogue.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptStringLiteralIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCommandElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.commandStringUpper
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.parentParameter
import com.badahori.creatures.plugins.intellij.agenteering.caos.references.getStringNameRangeInString
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueItemName
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

class CatalogueTagNameReference(val element: CatalogueItemName) : PsiPolyVariantReferenceBase<CatalogueItemName>(element, getStringNameRangeInString(element.text)) {

    private val selfOnlyResult by lazy {
        if (myElement.isValid) {
            PsiElementResolveResult.createResults(myElement)
        } else {
            ResolveResult.EMPTY_ARRAY
        }
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {

        val project = myElement.project

        if (project.isDisposed) {
            return ResolveResult.EMPTY_ARRAY
        }

        if (DumbService.isDumb(project)) {
            return selfOnlyResult
        }

        val tagName = element.name
            .nullIfEmpty()
            ?: return ResolveResult.EMPTY_ARRAY

        val strings = CaosScriptStringLiteralIndex.instance[tagName, project]
            .filter { string ->
                val command = string.getParentOfType(CaosScriptCommandElement::class.java)
                    ?.commandStringUpper
                    ?: return@filter false
                if (command !in catalogueCommands) {
                    return@filter false
                }
                string.parentParameter?.index == 0
            }

        return if (strings.isEmpty()) {
            ResolveResult.EMPTY_ARRAY
        } else {
            PsiElementResolveResult.createResults(strings)
        }

    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return element.setName(newElementName)
    }

    companion object {
        private val catalogueCommands = listOf(
            "READ",
            "REAN",
            "REAQ"
        )
    }

}