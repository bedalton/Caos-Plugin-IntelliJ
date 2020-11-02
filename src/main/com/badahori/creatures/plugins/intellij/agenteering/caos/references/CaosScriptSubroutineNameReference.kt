package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptSubroutineIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.hasSharedContextOfTypeStrict
import com.badahori.creatures.plugins.intellij.agenteering.utils.hasParentOfType
import com.intellij.psi.util.PsiTreeUtil

class CaosScriptSubroutineNameReference(element: CaosScriptSubroutineName) : PsiReferenceBase<CaosScriptSubroutineName>(element, TextRange.create(0, element.textLength)) {

    private val isDeclaration by lazy {
        myElement.parent?.parent is CaosScriptSubroutine
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (isDeclaration)
            return false
        if (element.text != myElement.text) {
            return false
        }
        if (myElement.hasSharedContextOfTypeStrict(element, CaosScriptScriptBodyElement::class.java)) {
            return element.parent?.parent is CaosScriptSubroutine
        }
        return false
    }

    private fun isValidSubrGsubPair(subroutineName: PsiElement, gsubName: PsiElement): Boolean {
        return subroutineName.parent?.parent is CaosScriptSubroutine && gsubName.hasParentOfType(CaosScriptCGsub::class.java)
    }

    override fun resolve(): PsiElement? {
        //if (myElement.parent is CaosScriptSubroutineHeader)
            //return null
        val name = myElement.name
        //if (resolvedElement == myElement)
          //  return null
        return CaosScriptSubroutineIndex.instance[name, myElement.project].firstOrNull {
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