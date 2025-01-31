@file:Suppress("UnstableApiUsage")

package com.badahori.creatures.plugins.intellij.agenteering.caos.hints

import com.bedalton.common.util.toListOf
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosValuesList
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.commandStringUpper
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getValuesList
import com.badahori.creatures.plugins.intellij.agenteering.common.InlayHintGenerator
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.Option
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement


enum class CaosScriptInlayTypeHint(description: String, defaultEnabled: Boolean, override val priority: Int = 5) :
    InlayHintGenerator {

    /**
     * Provides hint for return values of expression, mostly for comparisons
     *
     */
    ATTRIBUTE_BITFLAGS_IN_EQUALITY_EXPRESSIONS("Show bit flag for equality expressions", true, 100) {
        override fun isApplicable(element: PsiElement): Boolean {
            return (element as? CaosScriptRvalue)?.intValue.orElse(0) > 0 && element.parent is CaosScriptEqualityExpressionPrime && usesBitFlags(
                element as CaosScriptRvalue
            ) && element.isNotFolded
        }

        private fun usesBitFlags(element: CaosScriptRvalue): Boolean {
            val commandString = getCommandString(element)
                ?: return false
            val variant = element.variant
                ?: return false
            if (element.intValue.orElse(0) < 1)
                return false
            // Check if value is cached for this list
            element.getUserData(BIT_FLAG_IN_EQUALITY_LIST_KEY)?.let {
                if (it.first like commandString) {
                    return it.second
                }
            }
            val commandDefinition = CaosLibs[variant].command[commandString]
            if (commandDefinition == null) {
                // Persist find list fail as command could not be found
                element.putUserData(BIT_FLAG_IN_EQUALITY_LIST_KEY, Pair(commandString, false))
                return false
            }

            // Get return values list if any
            val valuesList = commandDefinition.returnValuesList[variant]
            if (valuesList == null) {
                // Persist find list fail for command
                element.putUserData(BIT_FLAG_IN_EQUALITY_LIST_KEY, Pair(commandString, false))
                return false
            }
            val isBitFlag = valuesList.bitflag
            // Cache isBitFlag value
            element.putUserData(BIT_FLAG_IN_EQUALITY_LIST_KEY, Pair(commandString, isBitFlag))
            // Return value
            return isBitFlag
        }

        override fun provideHints(element: PsiElement): List<InlayInfo> {

            if (element.isFolded) {
                return EMPTY_INLAY_LIST
            }
            // Ensure element is RValue, though that should have been checked in isApplicable()
            val expression = element as? CaosScriptRvalue
                ?: return EMPTY_INLAY_LIST

            // If value is not greater than 0, then it does not matter if it is bitflags or not
            if (element.intValue.orElse(0) < 1)
                return EMPTY_INLAY_LIST

            // Ensure and get a variant for file
            val variant = expression.variant
                ?: return EMPTY_INLAY_LIST

            // Ensure and get bitflag value
            val bitFlagValue = expression.intValue
                ?: return EMPTY_INLAY_LIST

            // Get the command string for this element in the equality expression
            val commandString = getCommandString(element)
                ?: return EMPTY_INLAY_LIST

            // Get command definition for enclosing command
            val commandDefinition = CaosLibs[variant].command[commandString]
                ?: return EMPTY_INLAY_LIST
            // Get the variant type-list if any
            val typeList = commandDefinition.returnValuesList[variant]
                ?: return EMPTY_INLAY_LIST
            // Generate bit-flag inlay hints
            return getBitFlagHintValues(typeList, bitFlagValue, element.endOffset)
        }

        /**
         * Gets the hint info for use in black-listing
         */
        override fun getHintInfo(element: PsiElement): HintInfo? {
            if (element !is CaosScriptRvalue) {
                return null
            }
            // If value is not greater than 0, then it does not matter if it is bitflags or not
            if (element.intValue.orElse(0) < 1)
                return null

            val parent = element.parent as? CaosScriptEqualityExpression ?: element
            return HintInfo.MethodInfo(parent.text, listOf(), CaosScriptLanguage)
        }

        /**
         * Finds the command sting in the given comparison expression
         * This entails getting the opposing argument, and finding its command if any
         */
        private fun getCommandString(element: CaosScriptRvalue): String? {
            // Ensure parent is equality expression
            val parent = element.parent as? CaosScriptComparesEqualityElement
                ?: return null
            // Get command token for equality expression
            val commandToken = getCommandTokenFromEquality(parent, element)
                ?: return null
            return commandToken.commandString
        }
    },

    /**
     * Hint for showing a bit flags values for an argument
     * ie attr 3 (carryable, mousable)
     */
    ATTRIBUTE_BITFLAGS_ARGUMENT_HINT("Show bit flag for argument value", true, 100) {
        override fun isApplicable(element: PsiElement): Boolean {
            return (element as? CaosScriptRvalue)?.intValue.orElse(0) > 0 && element.parent !is CaosScriptEqualityExpressionPrime && usesBitFlags(
                element as CaosScriptRvalue
            ) && element.isNotFolded
        }

        private fun usesBitFlags(element: CaosScriptRvalue): Boolean {
            return getValuesList(element)?.bitflag ?: false
        }

        override fun provideHints(element: PsiElement): List<InlayInfo> {
            val expression = element as? CaosScriptRvalue
                ?: return EMPTY_INLAY_LIST

            // If value is not greater than 0, then it does not matter if it is bitflags or not
            if (element.intValue.orElse(0) < 1)
                return EMPTY_INLAY_LIST

            // Can only get bit-flag hint values from integer rvalue
            val bitFlagValue = expression.intValue
                ?: return EMPTY_INLAY_LIST
            // Get values list
            val valuesList = getValuesList(expression)
                ?: return EMPTY_INLAY_LIST
            return getBitFlagHintValues(valuesList, bitFlagValue, element.endOffset)
        }

        override fun getHintInfo(element: PsiElement): HintInfo? {
            if (element !is CaosScriptRvalue) {
                return null
            }

            // If value is not greater than 0, then it does not matter if it is bitflags or not
            if (element.intValue.orElse(0) < 1)
                return null

            val valuesList = getValuesList(element)
                ?: return null
            return HintInfo.MethodInfo(valuesList.name, listOf(), CaosScriptLanguage)
        }
    },

    // Gets simple name for a family genus combination
    // i.e. scrp 2 8 (vendor) ...
    ASSUMED_GENUS_NAME_HINT("Show genus simple name", true, 102) {
        override fun isApplicable(element: PsiElement): Boolean {
            if (element !is CaosScriptRvalue || !element.isInt)
                return false

            // If parent is actual genus psi element, return true
            if (element.parent is CaosScriptGenus) {
                return true
            }
            // Only need to show genus names for command arguments
            val parentCommand = element.parent as? CaosScriptCommandElement
                ?: return false

            // Get index for argument in command call
            val index = element.index
            if (index < 1) // Cannot have preceding FAMILY value if index is 0
                return false

            // Get command definition for command
            val commandDefinition = parentCommand.commandDefinition
                ?: return false

            // Find parameters in command
            val parameters = commandDefinition.parameters

            // Ensure this parameter is for argument defined as genus
            val thisParameter = parameters.getOrNull(index) ?: return false
            if (thisParameter.name notLike "genus")
                return false

            // Ensure previous parameter is labeled as family
            // Cannot suggest genus without family
            val previousParameter = parameters.getOrNull(index - 1)
                ?: return false
            return previousParameter.name like "family"
        }

        override fun getHintInfo(element: PsiElement): HintInfo {
            return HintInfo.MethodInfo("Genus", listOf("family", "genus"), CaosScriptLanguage)
        }

        override fun provideHints(element: PsiElement): List<InlayInfo> {
            val variant = (element.containingFile as? CaosScriptFile)?.variant
                ?: return EMPTY_INLAY_LIST
            val genus = element.text.toIntSafe()
                ?: return EMPTY_INLAY_LIST
            val family = element.getPreviousNonEmptySibling(true)?.text?.toIntSafe()
                ?: return EMPTY_INLAY_LIST
            return getGenusInlayInfo(variant, element, family, genus)
        }

        /**
         * Gets the actual inlay information for this family genus combination
         */
        private fun getGenusInlayInfo(
            variant: CaosVariant,
            element: PsiElement,
            family: Int,
            genus: Int,
        ): List<InlayInfo> {
            val valuesList: CaosValuesList = CaosLibs.valuesLists[variant]
                // Find list labeled Genus, There should be only one
                .firstOrNull {
                    it.name like "Genus"
                }
            // If not value found, return empty inlay hints array
                ?: return EMPTY_INLAY_LIST
            val valuesListValue = valuesList["$family $genus"]
                ?: return EMPTY_INLAY_LIST
            // Format inlay hint for genus
            return listOf(InlayInfo("(${valuesListValue.name})", element.endOffset))
        }
    },

    /**
     * Gets assumed argument name for an equality expression rvalue
     * i.e. chem 4 (coldness)
     */
    ASSUMED_EQ_VALUE_NAME_HINT("Show assumed value name in Equality expression", true, 102) {
        override fun isApplicable(element: PsiElement): Boolean {
            return element is CaosScriptRvalue
                    // If argument contains a space, it is not a literal
                    // And all but genus values have no spaces, and genus is handled elsewhere
                    && element.isInt
                    && (element.parent is CaosScriptEqualityExpressionPrime)
        }

        /**
         * Provide hints for assumed literal values named value
         */
        override fun provideHints(element: PsiElement): List<InlayInfo> {
            if (element !is CaosScriptRvalue)
                return EMPTY_INLAY_LIST

            val parent = element.parent as? CaosScriptEqualityExpressionPrime
                ?: return EMPTY_INLAY_LIST

            val variant = element.variant
                ?: return EMPTY_INLAY_LIST

            val valuesList = parent.getValuesList(variant, element)
                ?: return EMPTY_INLAY_LIST
            val value = element.text
            if (value == "0" && valuesList.name.uppercase().let { it.startsWith("CHEM") || it.startsWith("DRIVE") })
                return EMPTY_INLAY_LIST

            // Get corresponding value for argument value in list of values
            val valuesListValue = valuesList[value]
                ?: return EMPTY_INLAY_LIST

            // Format hint and return
            return listOf(InlayInfo("(" + valuesListValue.name + ")", element.endOffset))
        }

        /**
         * Gets hint info definition
         * Might be used for blacklisting command hints
         */
        override fun getHintInfo(element: PsiElement): HintInfo? {
            val hint = provideHints(element).firstOrNull()
                ?: return null
            return HintInfo.MethodInfo("EQ Type Hint: " + hint.text, listOf(), CaosScriptLanguage)
        }
    },

    /**
     * Gets assumed argument name for a given value
     * i.e. chem 4 (coldness)
     */
    ASSUMED_VALUE_NAME_HINT("Show assumed value name", true) {
        override fun isApplicable(element: PsiElement): Boolean {
            return element is CaosScriptRvalue
                    // If argument contains a space, it is not a literal
                    // And all but genus values have no spaces, and genus is handled elsewhere
                    && element.isInt
        }

        /**
         * Provide hints for assumed literal values named value
         */
        override fun provideHints(element: PsiElement): List<InlayInfo> {
            if (element !is CaosScriptRvalue)
                return EMPTY_INLAY_LIST
            if (!element.isValid || element.project.isDisposed) {
                return EMPTY_INLAY_LIST
            }
            val value = element.text
            if (value.contains(' '))
                return EMPTY_INLAY_LIST
            val project = element.project
            if (DumbService.isDumb(project))
                return EMPTY_INLAY_LIST

            // Get list for argument
            val valuesList = getValuesList(element)
                ?: return EMPTY_INLAY_LIST
            if (value == "0" && valuesList.name.uppercase().let { it.startsWith("CHEM") || it.startsWith("DRIVE") })
                return EMPTY_INLAY_LIST

            // Get corresponding value for argument value in list of values
            val valuesListValue = valuesList[value]
                ?: return EMPTY_INLAY_LIST

            // Format hint and return
            return listOf(InlayInfo("(" + valuesListValue.name + ")", element.endOffset))
        }

        /**
         * Gets hint info definition
         * Might be used for blacklisting command hints
         */
        override fun getHintInfo(element: PsiElement): HintInfo? {
            if (element !is CaosScriptRvalue) {
                return null
            }
            val parentCommand = (element.parent as? CaosScriptCommandLike)?.commandStringUpper
                ?: return null
            return HintInfo.MethodInfo(parentCommand, listOf(), CaosScriptLanguage)
        }
    },

    /**
     * Get Hints about event script event names
     */
    ASSUMED_EVENT_SCRIPT_NAME_HINT("Show assumed event script name", true) {

        override fun isApplicable(element: PsiElement): Boolean {
            return element is CaosScriptEventNumberElement && element.isNotFolded
        }

        /**
         * Get actual inlay hints for event numbers
         */
        override fun provideHints(element: PsiElement): List<InlayInfo> {
            val eventElement = element as? CaosScriptEventNumberElement
                ?: return EMPTY_INLAY_LIST
            // Generate the 'event' prefix inlay value
            val items = listOf(
                InlayInfo("event", eventElement.startOffset)
            )
            // Ensure and get variant
            val variant = element.containingCaosFile?.variant
                ?: return items

            val typeList = CaosLibs[variant].valuesList("EventNumbers")
                ?: return items
            val value = typeList[eventElement.text]
                ?: return items
            return items + InlayInfo("(${value.name})", eventElement.endOffset)
        }

        /**
         * Gets hint information for this event number
         * Possibly used in black-lists
         */
        override fun getHintInfo(element: PsiElement): HintInfo? {
            if (element !is CaosScriptEventNumberElement) {
                return null
            }
            val variant = element.variant
                ?: return null
            return HintInfo.MethodInfo("${variant.code} EventNumber - ${element.text}", listOf(), CaosScriptLanguage)
        }
    },

    /**
     * Gets inlay hints for a DDE: PICT dimensions
     */
    DDE_PIC_DIMENSIONS("Show DDE: PICT dimensions", true) {

        /**
         * Determine whether this element should show a DDE: PICT inlay hint
         */
        override fun isApplicable(element: PsiElement): Boolean {
            return element is CaosScriptPictDimensionLiteral
        }

        /**
         * Provide the actual inlay hint for the DDE: PICT argument
         */
        override fun provideHints(element: PsiElement): List<InlayInfo> {
            val dimensions = element as? CaosScriptPictDimensionLiteral
                ?: return EMPTY_INLAY_LIST
            val text = dimensions.dimensions.let {
                "${it.first}x${it.second}"
            }
            return listOf(InlayInfo(text, element.endOffset))
        }

        override fun getHintInfo(element: PsiElement): HintInfo? {
            if (element !is CaosScriptPictDimensionLiteral) {
                return null
            }
            return HintInfo.MethodInfo("DDE: PICT " + element.text, listOf(), CaosScriptLanguage)
        }
    },

    /**
     * Shows family+genus+species breakdown for SETV CLAS statements
     * ie drv! (int) or targ (agent)
     */
    C1_CLAS_VALUE("Show family+genus+species for CLAS assignment value", true, 1000) {

        private val setvClasRegexTest = "[Ss][Ee][Tt][Vv]\\s+[Cc][Ll][Aa][Ss]\\s+\\d+".toRegex()

        /**
         * Determines whether to show this parameter hint of not
         */
        override fun isApplicable(element: PsiElement): Boolean {
            if (element !is CaosScriptRvalue)
                return false
            val parent = element.parent as? CaosScriptCAssignment
                ?: return false
            return setvClasRegexTest.matches(parent.text)
        }

        /**
         * Provide actual hints for this CLAS rvalue
         */
        override fun provideHints(element: PsiElement): List<InlayInfo> {
            val formattedClas = getC1ClasText(element)
                ?: return EMPTY_INLAY_LIST
            return listOf(InlayInfo(formattedClas, element.endOffset))
        }

        override fun getHintInfo(element: PsiElement): HintInfo? {
            if (!isApplicable(element)) {
                return null
            }
            return HintInfo.MethodInfo(element.parent.text, listOf(), CaosScriptLanguage)
        }
    },

    /**
     * Shows return type for command calls
     * ie drv! (int) or targ (agent)
     */
    COMMAND_RETURN_TYPE("Show rvalue return type", true) {
        /**
         * Determines whether to show this parameter hint of not
         */
        override fun isApplicable(element: PsiElement): Boolean {
            return element is CaosScriptRvaluePrime
        }

        /**
         * Provide hints for this command's return type
         */
        override fun provideHints(element: PsiElement): List<InlayInfo> {
            if (element !is CaosScriptRvaluePrime)
                return EMPTY_INLAY_LIST
            if (!element.isValid || element.project.isDisposed) {
                return EMPTY_INLAY_LIST
            }
            val commandToken = element.commandTokenElement
                ?: return EMPTY_INLAY_LIST
            val commandString = element.commandStringUpper
                ?: return EMPTY_INLAY_LIST
            element.getUserData(RETURN_VALUES_TYPE_KEY)?.let {
                if (commandString like it.first && it.second != null && it.second?.isNotEmpty() == true)
                    return listOf(InlayInfo("(${it.second!!.first()})", commandToken.endOffset))
            }
            val types: List<CaosExpressionValueType> = if (DumbService.isDumb(element.project))
                return EMPTY_INLAY_LIST
            else
                element.inferredType.let { types ->
                    if (types.all { it == UNKNOWN || it == ANY })
                        null
                    else
                        types
                } ?: element.commandDefinition?.returnType?.toListOf()
                ?: return EMPTY_INLAY_LIST
            var typeNames = types.map { it.simpleName }
            var typeName: String? = null
            // element.parent.parent = RvaluePrime -> Rvalue -> CommandElement
            (element.parent?.parent as? CaosScriptCommandElement)?.let { parent ->
                val index = (element.parent as CaosScriptRvalue).index
                val parameter = parent.commandDefinition?.parameters?.getOrNull(index)
                    ?: return emptyList()
                if (parameter.name in typeNames)
                    return EMPTY_INLAY_LIST


                // In C1/C2 agent return values are integers
                // and can be used anywhere integers are
                val isIntToAgentCoercion = element.variant?.isOld.orFalse()
                        && types.all { it == AGENT }
                        && parameter.type == INT
                // When coercing an agent to an int
                // change the return value hint to depict that
                typeName = if (isIntToAgentCoercion) {
                    "Agent->ID".apply {
                        typeNames = this.toListOf()
                    }
                } else when (types.size) {
                    0 -> return EMPTY_INLAY_LIST
                    1 -> types.first().simpleName
                    else -> {
                        if (parameter.type in types)
                            parameter.type.simpleName
                        else
                            return EMPTY_INLAY_LIST
                    }
                }
            }
            element.putUserData(RETURN_VALUES_TYPE_KEY, Pair(commandString, typeNames))
            if (typeName == null)
                typeName = if (typeNames.size == 1)
                    typeNames[0]
                else
                    return EMPTY_INLAY_LIST
            return listOf(InlayInfo("(${typeName})", commandToken.endOffset))


        }

        /**
         * Generate hint info
         * Possibly for blacklisting
         */
        override fun getHintInfo(element: PsiElement): HintInfo? {
            if (element !is CaosScriptCommandLike) {
                return null
            }
            val commandString = element.commandStringUpper
                ?: return null
            return HintInfo.MethodInfo(commandString, listOf(), CaosScriptLanguage)
        }
    };

    override val enabled: Boolean
        get() = option.isEnabled()

    override val option: Option = Option("SHOW_${this.name}", { description }, defaultEnabled)
}


/**
 * Gets the command token from the opposing side of an equality comparison
 */
private fun getCommandTokenFromEquality(
    parent: CaosScriptComparesEqualityElement,
    expression: CaosScriptRvalue,
): CaosScriptIsCommandToken? {
    val other: CaosScriptRvalue? = if (parent.first == expression) parent.second else parent.first
    return other?.rvaluePrime?.commandTokenElement //?: other?.varToken?.lastAssignment
}

/**
 * Generates the inlay hints for a bitflag and bitflag list
 */
private fun getBitFlagHintValues(typeList: CaosValuesList, bitFlagValue: Int, offset: Int): List<InlayInfo> {
    val text = getBitFlagText(typeList, bitFlagValue, ",")
        ?: return EMPTY_INLAY_LIST
    return listOf(InlayInfo("($text)", offset))
}

internal fun getBitFlagText(typeList: CaosValuesList, bitFlagValue: Int, delimiter: String): String? {
    // This hint is only worried about bitflags
    // Return if not bit-flags, even if list exists, but is not Bit-Flags
    if (!typeList.bitflag)
        return null

    // Loop through bitflags values list, and collect matches to rvalue int value
    val values = mutableListOf<String>()
    for (typeListValue in typeList.values) {
        try {
            val typeListValueValue = typeListValue.intValue
                ?: continue
            if (bitFlagValue and typeListValueValue > 0)
                values.add(typeListValue.name)
        } catch (e: Exception) {
            e.rethrowAnyCancellationException()
        }
    }
    return values.joinToString(delimiter)
}

/**
 * Gets the values list given an element
 */
internal fun getValuesList(element: CaosScriptRvalue): CaosValuesList? {
    val parent = element.parent as? CaosScriptCommandElement
        ?: return null
    val variant = element.variant
        ?: return null
    val index = element.index
    val key = "${parent.commandString}:$index"
    // If enclosing command and parameter index are the same
    // Return the cached value list or null if value list id is also null
    element.getUserData(ARGUMENT_VALUES_LIST_KEY)?.let { cachedValuesListData ->
        if (cachedValuesListData.first like key) {
            return cachedValuesListData.second?.let { valuesListId -> CaosLibs.valuesList[valuesListId] }
        }
    }

    // Get values list for expression
    // If is SETV expression get values list for lvalue
    val valuesList = if (parent is CaosScriptCAssignment) {
        parent.lvalue
            ?.commandDefinition
            ?.returnValuesList
            ?.get(variant)
    } else {
        // Load up command definition
        val definition = parent.commandDefinition
            ?: return null
        // Get corresponding parameter information for argument
        val parameter = definition.parameters.getOrNull(index)
            ?: return null
        // Get parameters value list if any
        parameter.valuesList[variant]
    }
    // Cache values list id for easier retrieval without parameters check
    // Stores null id if no list is found
    // Stored to prevent an additional query if cached, and it is already known that there is no list
    element.putUserData(ARGUMENT_VALUES_LIST_KEY, Pair(key, valuesList?.id))
    // Return nullable values list
    return valuesList
}

/**
 * Used to cache bit flag list validity in equality
 */
private val BIT_FLAG_IN_EQUALITY_LIST_KEY: Key<Pair<String, Boolean>> =
    Key("com.badahori.creatures.BitFlagsListIsValidInEqualityExpression")

private val ARGUMENT_VALUES_LIST_KEY: Key<Pair<String, Int?>> = Key("com.badahori.creatures.ArgumentValueList")

private val RETURN_VALUES_TYPE_KEY: Key<Pair<String, List<String>?>> = Key("com.badahori.creatures.ReturnValueType")

internal val EMPTY_INLAY_LIST: List<InlayInfo> = emptyList()

