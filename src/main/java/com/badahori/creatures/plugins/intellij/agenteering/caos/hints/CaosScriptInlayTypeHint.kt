@file:Suppress("UnstableApiUsage")

package com.badahori.creatures.plugins.intellij.agenteering.caos.hints

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosValuesList
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.commandStringUpper
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.endOffset
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getPreviousNonEmptySibling
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.startOffset
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.CaosAgentClassUtils
import com.badahori.creatures.plugins.intellij.agenteering.utils.like
import com.badahori.creatures.plugins.intellij.agenteering.utils.notLike
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.utils.toIntSafe
import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.Option
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement


enum class CaosScriptInlayTypeHint(description: String, override val enabled: Boolean, override val priority: Int = 0) : CaosScriptHintsProvider {


    /**
     * Provides hint for return values of expression, mostly for comparisons
     *
     */
    ATTRIBUTE_BITFLAGS_IN_EQUALITY_EXPRESSIONS("Show bit flag for equality expressions", true, 100) {
        override fun isApplicable(element: PsiElement): Boolean {
            return option.isEnabled() && (element as? CaosScriptRvalue)?.isInt.orFalse() && usesBitFlags(element as CaosScriptRvalue)
        }

        private fun usesBitFlags(element: CaosScriptRvalue): Boolean {
            val commandString = getCommandString(element)
                    ?: return false
            val variant = element.variant
                    ?: return false
            // Check if value is cached for this list
            element.getUserData(BIT_FLAG_IN_EQUALITY_LIST_KEY)?.let {
                if (it.first like commandString) {
                    return it.second
                }
            }
            val commandDefinition = CaosLibs[variant].command[commandString]
            if (commandDefinition == null) {
                // Persist find list fail as command could not be found
                element.putUserData(BIT_FLAG_IN_EQUALITY_LIST_KEY, Pair(commandString,false))
                return false
            }

            // Get return values list if any
            val valuesList = commandDefinition.returnValuesList[variant]
            if (valuesList == null) {
                // Persist find list fail for command
                element.putUserData(BIT_FLAG_IN_EQUALITY_LIST_KEY, Pair(commandString,false))
                return false
            }
            val isBitFlag = valuesList.bitflag
            // Cache isBitFlag value
            element.putUserData(BIT_FLAG_IN_EQUALITY_LIST_KEY, Pair(commandString, isBitFlag))
            // Return value
            return isBitFlag
        }

        override fun provideHints(element: PsiElement): List<InlayInfo> {
            // Ensure element is RValue, though that should have been checked in isApplicable()
            val expression = element as? CaosScriptRvalue
                    ?: return EMPTY_INLAY_LIST

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
            val parent = element.parent as? CaosScriptEqualityExpression ?: element
            return HintInfo.MethodInfo(parent.text, listOf(), CaosScriptLanguage)
        }

        /**
         * Finds the command sting in the given comparison expression
         * This entails getting the opposing argument, and finding its command if any
         */
        private fun getCommandString(element:CaosScriptRvalue) : String? {
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
     * Hint for showing an bit flags values for an argument
     * ie attr 3 (carryable, mousable)
     */
    ATTRIBUTE_BITFLAGS_ARGUMENT_HINT("Show bit flag for argument value", true, 100) {
        override fun isApplicable(element: PsiElement): Boolean {
            return option.isEnabled() && (element as? CaosScriptRvalue)?.isInt.orFalse() && usesBitFlags(element as CaosScriptRvalue)
        }

        private fun usesBitFlags(element: CaosScriptRvalue): Boolean {
            return getValuesList(element)?.bitflag ?: false
        }

        override fun provideHints(element: PsiElement): List<InlayInfo> {
            val expression = element as? CaosScriptRvalue
                    ?: return EMPTY_INLAY_LIST
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
            val valuesList = getValuesList(element)
                    ?: return null
            return HintInfo.MethodInfo(valuesList.name, listOf(), CaosScriptLanguage)
        }
    },

    // Gets simple name for a family genus combination
    // ie. scrp 2 8 (vendor) ...
    ASSUMED_GENUS_NAME_HINT("Show genus simple name", true, 102) {
        override fun isApplicable(element: PsiElement): Boolean {
            if (!option.isEnabled())
                return false
            if (element !is CaosScriptRvalue || !element.isInt)
                return false

            // If parent is actual genus psi element, return true
            if (element.parent is CaosScriptGenus) {
                return true
            }
            // Only need to show genus names for command arguments
            val parentCommand = element.parent as? CaosScriptCommandLike
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
            return previousParameter.name like  "family"
        }

        override fun getHintInfo(element: PsiElement): HintInfo? {
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
        private fun getGenusInlayInfo(variant: CaosVariant, element: PsiElement, family: Int, genus: Int): List<InlayInfo> {
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
     * Gets assumed argument name for a given value
     * ie. chem 4 (coldness)
     */
    ASSUMED_VALUE_NAME_HINT("Show assumed value name", true) {
        override fun isApplicable(element: PsiElement): Boolean {
            return option.isEnabled()
                    && element is CaosScriptRvalue
                    // If argument contains a space, it is not a literal
                    // And all but genus values have no spaces, and genus is handled elsewhere
                    && !element.text.contains(' ')
        }

        /**
         * Provide hints for assumed literal values named value
         */
        override fun provideHints(element: PsiElement): List<InlayInfo> {
            if (element !is CaosScriptRvalue)
                return emptyList()
            val value = element.text
            if (value.contains(' '))
                return EMPTY_INLAY_LIST
            val project = element.project
            if (DumbService.isDumb(project))
                return EMPTY_INLAY_LIST

            // Get list for argument
            val valuesList = getValuesList(element)
                    ?: return EMPTY_INLAY_LIST

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
            val parent = element.parent as? CaosScriptCommandLike
                    ?: return null
            return HintInfo.MethodInfo(parent.commandString, listOf(), CaosScriptLanguage)
        }
    },

    /**
     * Get Hints about event script event names
     */
    ASSUMED_EVENT_SCRIPT_NAME_HINT("Show assumed event script name", true) {

        override fun isApplicable(element: PsiElement): Boolean {
            return option.isEnabled() && element is CaosScriptEventNumberElement
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
            return items + InlayInfo("(${value.value})", eventElement.endOffset)
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
            return option.isEnabled() && element is CaosScriptPictDimensionLiteral
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
    C1_CLAS_VALUE("Show family+genus+species for CLAS assignment value", true) {

        private val setvClasRegexTest = "[Ss][Ee][Tt][Vv]\\s+[Cc][Ll][Aa][Ss].*".toRegex()
        /**
         * Determines whether to show this parameter hint of not
         */
        override fun isApplicable(element: PsiElement): Boolean {
            if (!option.isEnabled() || element !is CaosScriptRvalue)
                return false
            val parent = element.parent as? CaosScriptCAssignment
                    ?: return false
            return  setvClasRegexTest.matches(parent.text)
        }

        /**
         * Provide actual hints for this CLAS rvalue
         */
        override fun provideHints(element: PsiElement): List<InlayInfo> {
            val rvalue = element as? CaosScriptRvalue
                    ?: return EMPTY_INLAY_LIST
            val clasValue = rvalue.intValue
                    ?: return EMPTY_INLAY_LIST
            val agentClass = CaosAgentClassUtils.parseClas(clasValue)
                    ?: return EMPTY_INLAY_LIST
            val formattedClas = "family:${agentClass.family} genus:${agentClass.genus} species:${agentClass.species}"
            return listOf(InlayInfo(formattedClas, rvalue.endOffset))
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
            return option.isEnabled() && (element is CaosScriptRvaluePrime || element is CaosScriptLvalue)
        }

        /**
         * Provide hints for this command's return type
         */
        override fun provideHints(element: PsiElement): List<InlayInfo> {
            if (element !is CaosScriptCommandLike)
                return EMPTY_INLAY_LIST
            val commandToken = element.commandToken
                    ?: return EMPTY_INLAY_LIST
            val commandString = element.commandStringUpper
            element.getUserData(RETURN_VALUES_TYPE_KEY)?.let {
                if (commandString like it.first)
                    return listOf(InlayInfo("(${it.second})", commandToken.endOffset))
            }
            val type: CaosExpressionValueType = if (element is CaosScriptLvalue) {
                CaosExpressionValueType.VARIABLE
            } else {
                element.commandDefinition?.returnType
            }
                    ?: return EMPTY_INLAY_LIST
            val typeName = type.simpleName
            element.putUserData(RETURN_VALUES_TYPE_KEY, Pair(commandString, typeName))
            return listOf(InlayInfo("($typeName)", commandToken.endOffset))
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
            return HintInfo.MethodInfo(commandString, listOf(), CaosScriptLanguage)
        }
    },

    /**
     * Shows parameter name for argument
     * ie stm# shou {stimulus}:10
     */
    ARGUMENT_PARAMETER_NAME("Show parameter name for argument", true) {
        /**
         * Determines whether to show this parameter hint of not
         */
        override fun isApplicable(element: PsiElement): Boolean {
            return option.isEnabled() && element is CaosScriptRvalue && element.parent is CaosScriptCommandLike
        }

        /**
         * Provide hints for this command's return type
         */
        override fun provideHints(element: PsiElement): List<InlayInfo> {
            if (element !is CaosScriptRvalue)
                return EMPTY_INLAY_LIST
            // Ensure and get parent command
            val parent = element.parent as? CaosScriptCommandLike
                    ?: return EMPTY_INLAY_LIST
            // Get command definition from static lib
            val commandDefinition = parent.commandDefinition
                    ?: return EMPTY_INLAY_LIST
            // Get argument index in command call
            val index = element.index
            val parameter = commandDefinition.parameters.getOrNull(index)
                    ?: return EMPTY_INLAY_LIST
            val parameterName = parameter.name
            return listOf(InlayInfo("$parameterName:", element.startOffset))
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
            return HintInfo.MethodInfo(commandString, listOf(), CaosScriptLanguage)
        }
    };
    override val option: Option = Option("SHOW_${this.name}", description, enabled)
}


/**
 * Gets the command token from the opposing side of a equality comparison
 */
private fun getCommandTokenFromEquality(parent: CaosScriptComparesEqualityElement, expression: CaosScriptRvalue): CaosScriptIsCommandToken? {
    val other: CaosScriptRvalue? = if (parent.first == expression) parent.second else parent.first
    return other?.varToken?.lastAssignment ?: other?.rvaluePrime?.commandToken
}

/**
 * Generates the inlay hints for a bitflag and bitflag list
 */
private fun getBitFlagHintValues(typeList:CaosValuesList, bitFlagValue:Int, offset:Int) : List<InlayInfo> {
    // This hint is only worried about bitflags
    // Return if not bit-flags, even if list exists, but is not Bit-Flags
    if (!typeList.bitflag)
        return emptyList()

    // Loop through bitflags values list, and collect matches to rvalue int value
    val values = mutableListOf<String>()
    for (typeListValue in typeList.values) {
        try {
            val typeListValueValue = typeListValue.intValue
                    ?: continue
            if (bitFlagValue and typeListValueValue > 0)
                values.add(typeListValue.name)
        } catch (e: Exception) {
        }
    }
    return listOf(InlayInfo("(${values.joinToString()})", offset))
}

/**
 * Gets the values list given an element
 */
private fun getValuesList(element: CaosScriptRvalue): CaosValuesList? {
    val parent = element.parent as? CaosScriptCommandLike
            ?: return null
    val variant = element.variant
            ?: return null
    val index = element.index
    val key = "${parent.commandString}:$index"
    // If enclosing command and parameter index are the same
    // Return cached value list or null if value list id is also null
    element.getUserData(ARGUMENT_VALUES_LIST_KEY)?.let { cachedValuesListData ->
        if (cachedValuesListData.first like key) {
            return cachedValuesListData.second?.let { valuesListId -> CaosLibs.valuesList[valuesListId] }
        }
    }
    // Load up command definition
    val definition = parent.commandDefinition
            ?: return null
    // Get corresponding parameter information for argument
    val parameter = definition.parameters.getOrNull(index)
            ?: return null
    // Get parameters value list if any
    val valuesList = parameter.valuesList[variant]
    // Cache values list id for easier retrieval without parameters check
    // Stores null id if no list is found
    // Stored to prevent an additional query if cached and it is already known there is no list
    element.putUserData(ARGUMENT_VALUES_LIST_KEY, Pair(key, valuesList?.id))
    // Return nullable values list
    return valuesList
}

/**
 * Used to cache bit flag list validity in equality
 */
private val BIT_FLAG_IN_EQUALITY_LIST_KEY:Key<Pair<String, Boolean>> = Key("com.badahori.creatures.BitFlagsListIsValidInEqualityExpression")

private val ARGUMENT_VALUES_LIST_KEY:Key<Pair<String, Int?>> = Key("com.badahori.creatures.ArgumentValueList")

private val RETURN_VALUES_TYPE_KEY:Key<Pair<String, String?>> = Key("com.badahori.creatures.ReturnValueType")

private val EMPTY_INLAY_LIST:List<InlayInfo> = emptyList()

