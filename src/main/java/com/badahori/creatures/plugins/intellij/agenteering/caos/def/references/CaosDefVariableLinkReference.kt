package com.badahori.creatures.plugins.intellij.agenteering.caos.def.references

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefVariableLink
import com.badahori.creatures.plugins.intellij.agenteering.utils.getParentOfType

/**
 * Finds corresponding variable from link
 * ie. {variable1} -> variable1 (integer), {speed} -> speed (float)
 */
class CaosDefVariableLinkReference(element: CaosDefVariableLink) : PsiReferenceBase<CaosDefVariableLink>(element, element.offsetRange) {

    override fun resolve(): PsiElement? {
        val parent = element.getParentOfType(CaosDefCommandDefElement::class.java)
                ?: return null
        val text = element.variableName.lowercase()
        return parent.parameterList.firstOrNull { it.parameterName.lowercase() == text }
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return element.setName(newElementName)
    }

}