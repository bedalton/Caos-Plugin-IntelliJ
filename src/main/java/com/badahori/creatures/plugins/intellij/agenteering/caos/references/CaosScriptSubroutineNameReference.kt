package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.att.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptScriptElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSubroutine
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSubroutineHeader
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSubroutineName
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.badahori.creatures.plugins.intellij.agenteering.utils.hasSharedContextOfTypeStrict
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException

class CaosScriptSubroutineNameReference(element: CaosScriptSubroutineName) : PsiReferenceBase<CaosScriptSubroutineName>(element, TextRange.create(0, element.textLength)) {

    private val isDeclaration by lazy {
        myElement.parent is CaosScriptSubroutineHeader
    }

    override fun isReferenceTo(element: PsiElement): Boolean {

        // If we are a declaration, we cannot be referencing another subroutine declaration
        if (isDeclaration) {
            return false
        }

        // Make sure other element is a subroutine name in declaration
        val nameElement = (element as? CaosScriptSubroutineName)
            ?: element.parent as? CaosScriptSubroutineName
            ?: return false

        // Make sure that the two elements have the same text, otherwise, they cannot be matches
        if (nameElement.text != myElement.text) {
            return false
        }

        // Now make sure they share a script
        return myElement.hasSharedContextOfTypeStrict(element, CaosScriptScriptElement::class.java)
    }

    override fun resolve(): PsiElement? {
        if (myElement.parent is CaosScriptSubroutineHeader)
            return null
        val name = myElement.name

        return myElement.getParentOfType(CaosScriptScriptElement::class.java)?.let { scriptElement ->
            PsiTreeUtil.collectElementsOfType(scriptElement, CaosScriptSubroutine::class.java)
                    .mapNotNull { it.subroutineHeader.subroutineName }
                    .firstOrNull {
                        it.text == name
                    }
        }
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        if (myElement.variant?.isOld == true) {
            if (!CaosScriptPsiElementFactory.C1E_SUBROUTINE_NAME_REGEX.matches(newElementName)) {
                throw IncorrectOperationException("Subroutine name invalid. SUBR name should be 4 characters in length")
            }
        } else {
            if (!CaosScriptPsiElementFactory.C2E_SUBROUTINE_NAME_REGEX.matches(newElementName)) {
                throw IncorrectOperationException("Subroutine name invalid. SUBR should start with a letter")
            }
        }
        return myElement.setName(newElementName)
    }
}