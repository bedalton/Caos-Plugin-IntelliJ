package com.badahori.creatures.plugins.intellij.agenteering.caos.def.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefValuesListElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefValuesListValueKey
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.elementType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getSelfOrParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getValuesList
import com.badahori.creatures.plugins.intellij.agenteering.caos.references.CaosScriptEventNumberReference.Companion.EVENT_NUMBER_VALUES_LIST_NAME
import com.badahori.creatures.plugins.intellij.agenteering.utils.equalsIgnoreCase
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

class CaosDefValuesListValueKeyReference(element:CaosDefValuesListValueKey)
    : PsiReferenceBase<CaosDefValuesListValueKey>(element, TextRange(0, element.textLength)) {

    val text by lazy { myElement.text.trim() }

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element is CaosDefValuesListValueKey)
            return false
        LOGGER.info("Checking is reference to this($text) equals ${element.elementType}(${element.text})")
        if (element.text != text)
            return false
        val valuesList = myElement.getSelfOrParentOfType(CaosDefValuesListElement::class.java)
                ?.typeName
                ?: return false
        // If element is event number, return if this items list if EventNumbers
        element.getSelfOrParentOfType(CaosScriptEventNumberElement::class.java)?.let {
            LOGGER.info("Checking if list ($valuesList) equals value list of $EVENT_NUMBER_VALUES_LIST_NAME")
            return valuesList.equalsIgnoreCase(EVENT_NUMBER_VALUES_LIST_NAME)
        }
        LOGGER.info("Checking if element is expression")
        // Check that element is an expression value. If not, bail out
        val expression = element.getSelfOrParentOfType(CaosScriptExpression::class.java)
                ?: return false
        LOGGER.info("Element is expression")
        // If expression is part of equality express
        // Check for matching values list names
        (expression.parent as? CaosScriptEqualityExpressionPrime)?.let {
            LOGGER.info("Element in expression prime")
            return it.getValuesList(expression)?.equalsIgnoreCase(valuesList).orFalse()
        }
        (expression.getParentOfType(CaosScriptExpectsValueOfType::class.java))?.let {
            LOGGER.info("Element is expects value of type")
            return isRvalueReference(valuesList, it)
        }
        return false
    }

    private fun isRvalueReference(valuesList:String, rvalue:CaosScriptExpectsValueOfType) : Boolean {
        val command = rvalue.getParentOfType(CaosScriptCommandElement::class.java)
                ?: return false
        val commandToken = command.commandToken
                ?: return false
        val index = rvalue.index
        return commandToken.reference
                .multiResolve(true)
                .any {
                    val thisValuesList = it.element
                        ?.getSelfOrParentOfType(CaosDefCommandDefElement::class.java)
                            ?.parameterStructs
                            ?.getOrNull(index)
                            ?.type
                            ?.valuesList
                            ?: return@any false
                    return valuesList == thisValuesList
                }
    }


    override fun resolve(): PsiElement? = null

}