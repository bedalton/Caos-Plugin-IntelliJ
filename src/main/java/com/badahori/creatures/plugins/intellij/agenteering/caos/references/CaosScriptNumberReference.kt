package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefValuesListElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefValuesListElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefValuesListValue
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefValuesListValueKey
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.isVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptEventScriptIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosValuesList
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.notLike
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getValuesList
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

class CaosScriptNumberReference(element: CaosScriptNumber) :
    PsiPolyVariantReferenceBase<CaosScriptNumber>(element, TextRange.create(0, element.textLength)) {

    private val text by lazy { myElement.text }

    private val possibleValuesListName: String? by lazy lazy@{
        getValuesListDefinition()?.let { valuesList ->
            // Only cache list name, if it actually contains a valid entry for this value
            if (valuesList[text] != null)
                valuesList.name
            else
                null
        }
    }

    private val intValue by lazy { myElement.int?.text?.toIntSafe() }

    private val variant by lazy {
        myElement.variant
    }

    private val enclosingCommand by lazy {
        // Get argument parent command element. Required
        val parentRaw = myElement.parent?.parent as? CaosScriptCommandElement
            ?: return@lazy null
        parentRaw.commandDefinition
    }

    private val parameter by lazy {
        val index = (myElement.parent as? CaosScriptRvalue)
            ?.index
            ?: return@lazy null

        if (index < 0)
            return@lazy null

        enclosingCommand?.parameters?.getOrNull(index)
    }

    /**
     * Get possible values list name from an assignment such as SETV
     * Uses assignment LValue for the scope
     */
    private fun possibleValuesListNameForAssignment(assignment: CaosScriptCAssignment): CaosValuesList? {
        val commandString = assignment.lvalue?.commandString
            ?: return null
        val variant = assignment.variant
            ?: return null
        val variantCode = variant.code
        return CaosLibs.commands(commandString)
            .firstOrNull {
                variantCode in it.variants
            }
            ?.returnValuesList
            ?.get(variant)
    }

    /**
     * Gets values list name from
     */
    private fun possibleValuesListNameForParameter(
        argument: CaosScriptArgument,
        parentRaw: CaosScriptCommandElement
    ): CaosValuesList? {
        val commandDefinition = parentRaw.commandDefinition
            ?: return null

        // Values list check requires variant
        val variant = myElement.variant
            ?: return null
        // Get values list for argument position
        val argumentNumber = argument.index

        if (argumentNumber < 0) {
            return null
        }

        // Get parameter non-null
        val parameter = commandDefinition.parameters.getOrNull(argumentNumber)
        if (parameter == null) {
            LOGGER.severe("Incorrect command returned for ${commandDefinition.command}. Expected a parameter at $argumentNumber, but returned null. BNF grammar may be out of sync")
            return null
        }

        // get values list from parameter
        return parameter.valuesList[variant]
    }

    private fun possibleValuesListNamesForEquality(): CaosValuesList? {
        val expression = myElement.parent as? CaosScriptRvalue
            ?: return null
        val equalityExpression = expression.parent as? CaosScriptEqualityExpressionPrime
            ?: return null
        val variant = expression.variant
            ?: return null
        return equalityExpression.getValuesList(variant, expression)
    }

    private val project: Project by lazy { myElement.project }

    override fun isReferenceTo(anElement: PsiElement): Boolean {

        if (anElement is CaosScriptEventNumberElement) {
            return isMatchingEventScriptNumber(anElement)
        }

        if (anElement !is CaosDefValuesListValueKey) {
            return false
        }
        if (anElement.text != text)
            return false
        val parentDef = anElement.getParentOfType(CaosDefValuesListElement::class.java)
            ?: return false
        return parentDef.listName == possibleValuesListName
    }

    override fun multiResolve(partialMatch: Boolean): Array<ResolveResult> {

        // Make sure this is actually an int value for completion
        getScriptsIfAny()?.let {
            return PsiElementResolveResult.createResults(it)
        }

        val variant = myElement.containingCaosFile?.variant
            ?: return ResolveResult.EMPTY_ARRAY
        val keyAsInt = text.toIntSafe()
        val valuesListName = possibleValuesListName
            ?: return PsiElementResolveResult.EMPTY_ARRAY

        val keys = getKeys(variant, valuesListName, keyAsInt)
            ?: return PsiElementResolveResult.EMPTY_ARRAY
        return PsiElementResolveResult.createResults(keys)
    }

    private fun getValuesListDefinition(): CaosValuesList? {
        // If expression is in equality expression
        if (myElement.parent is CaosScriptEqualityExpression) {
            return possibleValuesListNamesForEquality()
        }
        // Get enclosing argument. Element may be nested 2 or 3 levels down
        val argument = myElement.getSelfOrParentOfType(CaosScriptArgument::class.java)
            ?: return null

        // Get argument parent command element. Required
        val parentRaw = argument.parent as? CaosScriptCommandElement
            ?: return null

        // If parent is assignment, get other side values list
        return if (parentRaw is CaosScriptCAssignment) {
            possibleValuesListNameForAssignment(parentRaw)
        } else {
            possibleValuesListNameForParameter(argument, parentRaw)
        }
    }

    private fun getValuesListDefinitions(project: Project, listName: String): List<CaosDefValuesListElement> {
        /*return CaosDefElementsSearchExecutor.getCaosDefFiles(project)
                .flatMap { file ->
                    PsiTreeUtil.collectElementsOfType(file, CaosDefValuesListElement::class.java)
                            .filter { valuesList ->
                                valuesList.typeName like listName
                            }
                } + */ return CaosDefValuesListElementsByNameIndex.Instance[listName, project]
    }

    private fun getKeys(variant: CaosVariant, valuesListName: String, value: Int?): List<CaosDefValuesListValue>? {
        val valuesLists: List<CaosDefValuesListElement> = getValuesListDefinitions(project, valuesListName)
            .nullIfEmpty()
            ?: return null
        return valuesLists
            .filter {
                it.isVariant(variant)
            }
            .flatMap matchKeys@{ valuesListElement: CaosDefValuesListElement ->
                if (valuesListElement.isBitflags && value != null) {
                    valuesListElement.valuesListValueList.filter { valuesListValueStruct ->
                        valuesListValueStruct.key.toIntSafe()?.let { key -> value and key > 0 } ?: false
                    }
                } else if (valuesListName == "Genus") {
                    val family = myElement.getPreviousNonEmptySibling(false)?.text?.toIntSafe()
                        ?: return@matchKeys emptyList<CaosDefValuesListValue>()
                    valuesListElement.valuesListValueList.filter { it.key == "$family $value" }
                } else {
                    valuesListElement.valuesListValueList.filter { it.key == text }
                }
            }
            .nullIfEmpty()
    }


    private fun getScriptsIfAny(): List<PsiElement>? {

        val number = intValue
            ?: return null

        val command = enclosingCommand
            ?: return null

        val parameter = parameter
            ?: return null

        // Simplified and hard coded check to see if event type
        var isCall = (command.command == "CALL" && parameter.index == 0)
        val isEvent = !isCall && parameter.name.equals("event", true) && parameter.index == 3
        val isMessage = !isCall && parameter.name like "messageId"
        if (!isCall && !isEvent && !isMessage) {
            return null
        }

        if (isMessage) {
            val variant = variant
                ?: return null
            getKeys(variant, "MessageNumbers", intValue)?.let {
                return it
            }
            if (variant == CaosVariant.C1)
                return null
            val previousText = myElement.getPreviousNonEmptySibling(false)?.text
            isCall = previousText.equals("ownr", ignoreCase = true)
        }

        return getScripts(isCall, number)
    }

    private fun getScripts(isCall: Boolean, number: Int?): List<PsiElement>? {
        val variant = variant
            ?: return null

        val scripts: Collection<CaosScriptEventScript> = if (isCall) {
            val script = element.getParentOfType(CaosScriptEventScript::class.java)
                ?: return null
            CaosScriptEventScriptIndex.instance[
                    variant,
                    script.family,
                    script.genus,
                    script.species,
                    number ?: 0,
                    project
            ]
        } else if (number != null) {
            CaosScriptEventScriptIndex.instance[variant, 0, 0, 0, number, project]
        } else {
            return null
        }
        return scripts.mapNotNull { it.eventNumberElement ?: it.firstChild }
    }

    private fun isMatchingEventScriptNumber(anElement: CaosScriptEventNumberElement): Boolean {

        // Make sure this element is actually an int
        val int = intValue
            ?: return false
        // Event script numbers cannot equal zero
        if (int == 0)
            return false

        // Make sure event numbers parent is an event script
        // Other elements use event number elements
        val otherEventScript = anElement.parent as? CaosScriptEventScript
            ?: return false


        // Make sure the event numbers match
        if (int != anElement.text?.toIntOrNull())
            return false


        if (variant notLike anElement.variant)
            return false

        val command = enclosingCommand
            ?: return false

        val parameter = parameter
            ?: return false

        // Hard coded to the event script commands. Probably shouldn't be though
        val isCallParameter = command.command == "CALL" && parameter.index == 0
        val isEventScriptReferencing = !isCallParameter && parameter.name.equals("event", true) && parameter.index == 3

        // If is neither call parameter nor an event script reference, return
        if (!isCallParameter && !isEventScriptReferencing)
            return false

        // Check that scripts are in same module
        val module = element.containingCaosFile?.module
        val otherModule = anElement.containingFile?.module
        if (module != null && otherModule != null && module != otherModule)
            return false


        val otherFamily = otherEventScript.family
        val otherGenus = otherEventScript.genus
        val otherSpecies = otherEventScript.species

        val thisFamily: Int
        val thisGenus: Int
        val thisSpecies: Int

        // If in null or matching module, and is event script find as match
        if (isEventScriptReferencing) {
            val siblings = (myElement.parent as? CaosScriptCommandElement)
                ?.arguments
                ?: return false
            if (siblings.size < 4)
                return false
            thisFamily = (siblings[0] as? CaosScriptRvalue)?.intValue ?: 0
            thisGenus = (siblings[1] as? CaosScriptRvalue)?.intValue ?: 0
            thisSpecies = (siblings[2] as? CaosScriptRvalue)?.intValue ?: 0
        } else if (command.command == "CALL" && parameter.index == 0) {
            val parentEventScript = myElement.getParentOfType(CaosScriptEventScript::class.java)
                ?: return false
            thisFamily = parentEventScript.family
            thisGenus = parentEventScript.genus
            thisSpecies = parentEventScript.species
        } else {
            return false
        }
        if (thisFamily != 0 && otherFamily != 0 && thisFamily != otherFamily)
            return false

        if (thisGenus != 0 && otherGenus != 0 && thisGenus != otherGenus)
            return false

        if (thisSpecies != 0 && otherSpecies != 0 && thisSpecies != otherSpecies)
            return false

        return true
    }
}
