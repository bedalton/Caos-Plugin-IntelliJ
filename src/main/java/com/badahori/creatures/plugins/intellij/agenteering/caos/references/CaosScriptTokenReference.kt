package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptArgument
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.StringStubKind
import com.badahori.creatures.plugins.intellij.agenteering.indices.CaseInsensitiveFileIndex
import com.badahori.creatures.plugins.intellij.agenteering.utils.getSelfOrParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.toNavigableElement
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

class CaosScriptTokenReference(private val element: CaosScriptToken) :
    PsiPolyVariantReferenceBase<CaosScriptToken>(element, TextRange(0, element.textLength)) {

    private val parentFileType by lazy {
        val argument = element.getSelfOrParentOfType(CaosScriptArgument::class.java)
            ?: return@lazy null
        StringStubKind.fromPsiElement(argument)
    }

    private val EMPTY_ARRAY get() = ResolveResult.EMPTY_ARRAY

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (element.variant?.isOld != true) {
            return EMPTY_ARRAY
        }
        val project = element.project
        if (project.isDisposed) {
            return EMPTY_ARRAY
        }
        if (DumbService.isDumb(project)) {
            return EMPTY_ARRAY
        }
        val type = parentFileType
            ?: return EMPTY_ARRAY


        val extensions = type.extensions
            ?: return EMPTY_ARRAY

        val name = element.stringValue
            .nullIfEmpty()
            ?: return EMPTY_ARRAY

        val results = CaseInsensitiveFileIndex.findWithFileNameAndExtensions(project, name, extensions)
            .map {
                it.toNavigableElement(project)
            }

        return if (results.isEmpty()) {
            EMPTY_ARRAY
        } else {
            PsiElementResolveResult.createResults(results)
        }
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        if (newElementName.length != 4) {
            return element
        }
        return element.setName(newElementName)
    }

}