package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefTypeDefinitionElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefTypeDefinition
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefTypeDefinitionElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.isVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.orDefault
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventNumberElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

class CaosScriptEventNumberReference(element:CaosScriptEventNumberElement) : PsiReferenceBase<CaosScriptEventNumberElement>(element, TextRange(0, element.textLength)) {

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element !is CaosDefTypeDefinition)
            return false
        val text = myElement.text
        if (element.text != text)
            return false
        val typeDefElement = element.getParentOfType(CaosDefTypeDefinitionElement::class.java)
                ?: return false
        return typeDefElement.typeName == EVENT_NUMBER_TYPE_DEF_NAME && typeDefElement.isVariant(myElement.containingCaosFile?.variant.orDefault())
    }

    override fun resolve(): PsiElement? {
        val project = element.project
        val text = element.text
        val variant = element.containingCaosFile?.variant.orDefault()
        return CaosDefTypeDefinitionElementsByNameIndex.Instance["EventNumbers", project]
                .filter { it.isVariant(variant) }
                .mapNotNull {
                    it.typeDefinitionList.firstOrNull { it.key == text }
                }
                .firstOrNull()
    }

    companion object {
        const val EVENT_NUMBER_TYPE_DEF_NAME = "EventNumbers"
    }

}