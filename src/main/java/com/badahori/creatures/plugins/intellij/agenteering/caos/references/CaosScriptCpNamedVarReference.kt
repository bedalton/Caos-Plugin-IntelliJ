package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptSubroutineIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.hasSharedContextOfTypeStrict
import com.badahori.creatures.plugins.intellij.agenteering.utils.hasParentOfType
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.PsiTreeUtil

class CaosScriptCpNamedVarReference(element: CaosScriptCpNamedVar) : PsiReferenceBase<CaosScriptCpNamedVar>(element, TextRange.create(1, element.textLength)) {

    private val isDeclaration by lazy {
        myElement.parent is CaosScriptCpNamedVarDeclaration
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (isDeclaration)
            return false
        if (element.text != myElement.text) {
            return false
        }
        if (element.hasParentOfType(CaosScriptScriptElement::class.java)) {
            return myElement.hasSharedContextOfTypeStrict(element, CaosScriptScriptElement::class.java)
        }
        return element.containingFile.isEquivalentTo(myElement.containingFile)
    }

    override fun resolve(): PsiElement? {
        //if (myElement.parent is CaosScriptSubroutineHeader)
            //return null
        val name = myElement.name
        //if (resolvedElement == myElement)
          //  return null
        return PsiTreeUtil
                .collectElementsOfType(myElement.containingFile, CaosScriptCpNamedVarDeclaration::class.java)
                .firstOrNull {
                    isReferenceTo(it)
                }
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return myElement.setName(newElementName)
    }
}