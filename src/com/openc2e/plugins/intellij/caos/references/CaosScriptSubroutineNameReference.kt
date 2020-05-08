package com.openc2e.plugins.intellij.caos.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.openc2e.plugins.intellij.caos.indices.CaosScriptSubroutineIndex
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCGsub
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptScriptBodyElement
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptSubroutine
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptSubroutineName
import com.openc2e.plugins.intellij.caos.psi.util.hasSharedContextOfTypeStrict

class CaosScriptSubroutineNameReference(element:CaosScriptSubroutineName) : PsiReferenceBase<CaosScriptSubroutineName>(element, TextRange.create(0, element.textLength)) {

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element.text != myElement.text)
            return false
        if (!myElement.hasSharedContextOfTypeStrict(element, CaosScriptScriptBodyElement::class.java))
            return false
        return (myElement.parent is CaosScriptSubroutine && element.parent is CaosScriptCGsub) ||
                (myElement.parent is CaosScriptCGsub && element.parent is CaosScriptSubroutine)
    }

    override fun resolve(): PsiElement? {
        if (myElement.parent !is CaosScriptSubroutine)
            return null;
        return CaosScriptSubroutineIndex.instance[myElement.name, myElement.project].firstOrNull {
            myElement.hasSharedContextOfTypeStrict(element, CaosScriptScriptBodyElement::class.java)
        }
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return myElement.setName(newElementName);
    }
}