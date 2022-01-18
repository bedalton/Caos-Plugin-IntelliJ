package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptQuoteStringLiteral
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptStringLike
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSubroutineName
import com.intellij.lang.refactoring.NamesValidator
import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.rename.inplace.VariableInplaceRenamer

class CaosScriptRenameRefactoringSupportProvider : RefactoringSupportProvider() {

    override fun isMemberInplaceRenameAvailable(
        element: PsiElement,
        context: PsiElement?,
    ): Boolean {
        return false //!isFileLocalString(element)
    }

    override fun isInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean {
        return element is CaosScriptSubroutineName || isFileLocalString(element)
    }

    override fun isSafeDeleteAvailable(element: PsiElement): Boolean {
        return false
    }

    private fun isFileLocalString(elementIn: PsiElement): Boolean {
        val element = (elementIn as? CaosScriptQuoteStringLiteral)
            ?: (elementIn.parent as? CaosScriptQuoteStringLiteral)
            ?: return false
        val parent = element.containingFile
        return element
            .reference
            .multiResolve(false)
        .all { it.element?.containingFile?.isEquivalentTo(parent) == true }
    }
}


class CaosScriptNameValidator : NamesValidator {
    override fun isKeyword(name: String, project: Project?): Boolean {
        return false
    }

    override fun isIdentifier(name: String, project: Project?): Boolean {
        return true
    }

}