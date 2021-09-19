package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptSubroutineIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.hasSharedContextOfTypeStrict
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.PsiTreeUtil

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

        return CaosScriptSubroutineIndex.instance[name, myElement.project].firstOrNull { element ->
            myElement.hasSharedContextOfTypeStrict(element, CaosScriptScriptBodyElement::class.java)
        } ?: myElement.getParentOfType(CaosScriptScriptElement::class.java)?.let { scriptElement ->
            PsiTreeUtil.collectElementsOfType(scriptElement, CaosScriptSubroutine::class.java)
                    .mapNotNull { it.subroutineHeader.subroutineName }
                    .firstOrNull {
                        it.text == name
                    }
        }
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return myElement.setName(newElementName)
    }
}