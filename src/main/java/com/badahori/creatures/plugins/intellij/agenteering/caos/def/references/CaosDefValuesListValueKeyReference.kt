package com.badahori.creatures.plugins.intellij.agenteering.caos.def.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefValuesListElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefValuesListValueKey
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.utils.getSelfOrParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getValuesListId
import com.badahori.creatures.plugins.intellij.agenteering.caos.references.CaosScriptEventNumberReference.Companion.EVENT_NUMBER_VALUES_LIST_NAME
import com.badahori.creatures.plugins.intellij.agenteering.utils.equalsIgnoreCase
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

/**
 * Finds corresponding ValuesListValue from rvalue integer
 */
class CaosDefValuesListValueKeyReference(element:CaosDefValuesListValueKey)
    : PsiReferenceBase<CaosDefValuesListValueKey>(element, TextRange(0, element.textLength)) {

    val text by lazy { myElement.text.trim() }

    /**
     * Checks for references to this values list item
     */
    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element is CaosDefValuesListValueKey)
            return false
        if (element.text != text)
            return false

        // Get value list value parent value list element
        val valuesList = myElement.getSelfOrParentOfType(CaosDefValuesListElement::class.java)
                ?.listName
                ?: return false

        // If element is event number, return if this items list if EventNumbers
        element.getSelfOrParentOfType(CaosScriptEventNumberElement::class.java)?.let {
            return valuesList.equalsIgnoreCase(EVENT_NUMBER_VALUES_LIST_NAME)
        }
        // Check that element is an expression value. If not, bail out
        val expression = element.getSelfOrParentOfType(CaosScriptRvalue::class.java)
                ?: return false
        val variant = expression.variant
                ?: return false

        // If expression is part of equality expression
        // Check for matching values list names
        (expression.parent as? CaosScriptEqualityExpressionPrime)?.let {
            val valuesListId = it.getValuesListId(variant, expression)
                    ?: return false
            val valuesListName = CaosLibs.valuesList[valuesListId]?.name
                    ?: return false
            return valuesListName.equalsIgnoreCase(valuesList)
        }
        return isRvalueReference(variant, valuesList, expression)
    }

    /**
     * Check if values list for parameter matches
     */
    private fun isRvalueReference(variant: CaosVariant, valuesListName:String, rvalue:CaosScriptRvalue) : Boolean {
        val command = rvalue.getParentOfType(CaosScriptCommandElement::class.java)
                ?: return false
        val commandDefinition= command.commandDefinition
                ?: return false
        val index = rvalue.index
        val parameter = commandDefinition.parameters.getOrNull(index)
                ?: return false
        return parameter.valuesList[variant]?.name?.equalsIgnoreCase(valuesListName).orFalse()
    }


    override fun resolve(): PsiElement? = null

}