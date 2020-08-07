package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefValuesListDefinitionElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefValuesListValue
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefValuesListElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.isVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventNumberElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

class CaosScriptEventNumberReference(element:CaosScriptEventNumberElement) : PsiReferenceBase<CaosScriptEventNumberElement>(element, TextRange(0, element.textLength)) {

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element !is CaosDefValuesListValue)
            return false
        val text = myElement.text
        if (element.text != text)
            return false
        val valuesListElement = element.getParentOfType(CaosDefValuesListElement::class.java)
                ?: return false
        val variant = myElement.containingCaosFile?.variant
                ?: return false
        return valuesListElement.typeName == EVENT_NUMBER_VALUES_LIST_NAME && valuesListElement.isVariant(variant)
    }

    override fun resolve(): PsiElement? {
        val project = element.project
        val text = element.text
        val variant = element.containingCaosFile?.variant
                ?: return null
        return CaosDefValuesListDefinitionElementsByNameIndex.Instance["EventNumbers", project]
                .filter { it.isVariant(variant) }
                .mapNotNull {
                    it.valuesListValueList.firstOrNull { it.key == text }
                }
                .firstOrNull()
    }

    companion object {
        const val EVENT_NUMBER_VALUES_LIST_NAME = "EventNumbers"
    }

}