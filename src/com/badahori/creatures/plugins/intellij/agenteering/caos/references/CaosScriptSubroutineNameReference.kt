package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptSubroutineIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.hasSharedContextOfTypeStrict
import com.intellij.psi.util.PsiTreeUtil

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
        if (myElement.parent is CaosScriptSubroutineHeader)
            return null
        val name = myElement.name
        LOGGER.info("Resolving...subr <$name>")
        val resolvedElement = CaosScriptSubroutineIndex.instance[name, myElement.project].firstOrNull {
            myElement.hasSharedContextOfTypeStrict(element, CaosScriptScriptBodyElement::class.java)
        } ?: myElement.getParentOfType(CaosScriptScriptElement::class.java)?.let { scriptElement ->
            PsiTreeUtil.collectElementsOfType(scriptElement, CaosScriptSubroutine::class.java)
                    .mapNotNull { it.subroutineHeader.subroutineName }
                    .firstOrNull {
                        LOGGER.info("Check subr $name == ${it.text}")
                        it.text == name
                    }}
        if (resolvedElement == myElement)
            return null
        return resolvedElement
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return myElement.setName(newElementName)
    }
}