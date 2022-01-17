package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.nullIfUndefOrBlank
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.RenamePsiElementProcessor

class CaosScriptRenameElementProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean {
        return element is CaosScriptSubroutine
                || element is CaosScriptSubroutineName
                || element is CaosScriptNamedGameVar
                || element is CaosScriptQuoteStringLiteral
                || element is CaosScriptStringText
                || element is CaosScriptStringLike
    }

    override fun isInplaceRenameSupported(): Boolean {
        return true
    }

    override fun findReferences(
        element: PsiElement,
        searchScope: SearchScope,
        searchInCommentsAndStrings: Boolean
    ): Collection<PsiReference> {
        return super.findReferences(element, searchScope, searchInCommentsAndStrings)
            .distinct()

    }

    override fun substituteElementToRename(element: PsiElement, editor: Editor?): PsiElement? {
        if (element is CaosScriptSubroutineName) {
            val name = element.name.nullIfUndefOrBlank()
                    ?: return null
            val containingScript = element.getParentOfType(CaosScriptScriptElement::class.java)
                    ?: return null
            return PsiTreeUtil.collectElementsOfType(containingScript, CaosScriptSubroutine::class.java)
                    .filter {
                        it.name == name
                    }
                    .mapNotNull {
                        it.subroutineHeader.subroutineName
                    }
                    .firstOrNull()
        }
        if (element is CaosScriptSubroutine) {
            return element.subroutineHeader.subroutineName
        }
        if (element is CaosScriptQuoteStringLiteral)
            return element
        return super.substituteElementToRename(element, editor)
    }


}