package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.att.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSubroutineName
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.badahori.creatures.plugins.intellij.agenteering.utils.isOrHasParentOfType
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenameInputValidator
import com.intellij.util.ProcessingContext

class CaosScriptRenameInputValidator: RenameInputValidator {

    override fun getPattern(): ElementPattern<out PsiElement> {
        return StandardPatterns.instanceOf(CaosScriptCompositeElement::class.java)
    }

    override fun isInputValid(newName: String, element: PsiElement, context: ProcessingContext): Boolean {
        if (newName.isBlank()) {
            return false
        }

        if (element.isOrHasParentOfType(CaosScriptSubroutineName::class.java)) {
            return isSubroutineNameValid(element.variant, newName)
        }
        return true
    }

    private fun isSubroutineNameValid(variant: CaosVariant?, newNameString: String): Boolean {
        if (variant == null) {
            return false
        }
        val subroutineNameRegex = if (variant.isOld) CaosScriptPsiElementFactory.C1E_SUBROUTINE_NAME_REGEX else CaosScriptPsiElementFactory.C2E_SUBROUTINE_NAME_REGEX
        return newNameString.matches(subroutineNameRegex)
    }
}