package com.badahori.creatures.plugins.intellij.agenteering.caos.def.references

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefVariableLink

class CaosDefVariableLinkReference(element: CaosDefVariableLink) : PsiReferenceBase<CaosDefVariableLink>(element, element.offsetRange) {

    override fun resolve(): PsiElement? {
        val parent = element.getParentOfType(com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement::class.java)
                ?: return null
        val text = element.variableName.toLowerCase()
        return parent.parameterList.firstOrNull { it.parameterName.toLowerCase() == text }
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return element.setName(newElementName)
    }

}