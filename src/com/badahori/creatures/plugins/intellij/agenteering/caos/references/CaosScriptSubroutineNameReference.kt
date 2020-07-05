package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptSubroutineIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCGsub
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptScriptBodyElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSubroutine
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSubroutineName
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.hasSharedContextOfTypeStrict

class CaosScriptSubroutineNameReference(element: CaosScriptSubroutineName) : PsiReferenceBase<CaosScriptSubroutineName>(element, TextRange.create(0, element.textLength)) {

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element.text != myElement.text)
            return false
        if (!myElement.hasSharedContextOfTypeStrict(element, CaosScriptScriptBodyElement::class.java))
            return false
        return (myElement.parent?.parent is CaosScriptSubroutine && element.parent is CaosScriptCGsub) ||
                (myElement.parent is CaosScriptCGsub && element.parent?.parent is CaosScriptSubroutine)
    }

    override fun resolve(): PsiElement? {
        if (myElement.parent !is CaosScriptSubroutine)
            return null
        return CaosScriptSubroutineIndex.instance[myElement.name, myElement.project].firstOrNull {
            myElement.hasSharedContextOfTypeStrict(element, CaosScriptScriptBodyElement::class.java)
        }
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return myElement.setName(newElementName)
    }
}