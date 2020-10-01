package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefValuesListElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefValuesListValue
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefValuesListElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefValuesListValueKey
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.isVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventNumberElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.hasParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.isOrHasParentOfType
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

class CaosScriptEventNumberReference(element:CaosScriptEventNumberElement) : PsiReferenceBase<CaosScriptEventNumberElement>(element, TextRange(0, element.textLength)) {

    private val text by lazy { element.text.trim() }

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element.text.trim() != text)
            return false
        if (!element.isOrHasParentOfType(CaosDefValuesListValueKey::class.java))
            return false
        val valuesListElement = element.getParentOfType(CaosDefValuesListElement::class.java)
                ?: return false
        if (valuesListElement.typeName != EVENT_NUMBER_VALUES_LIST_NAME) {
            return false
        }
        val variant = myElement.containingCaosFile?.variant
                ?: return false
        if (!valuesListElement.isVariant(variant)) {
            return super.isReferenceTo(element)
        }
        return true
    }

    override fun resolve(): PsiElement? {
        val project = element.project
        val text = element.text
        val variant = element.containingCaosFile?.variant
                ?: return null
        return CaosDefValuesListElementsByNameIndex.Instance["EventNumbers", project]
                .filter { it.isVariant(variant) }
                .mapNotNull { valuesList ->
                    valuesList.valuesListValueList.firstOrNull { it.key == text }
                }
                .firstOrNull()
    }

    companion object {
        const val EVENT_NUMBER_VALUES_LIST_NAME = "EventNumbers"
    }

}