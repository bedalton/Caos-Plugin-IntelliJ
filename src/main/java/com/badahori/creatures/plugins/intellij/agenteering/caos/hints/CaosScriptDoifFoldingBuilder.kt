@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.caos.hints

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosScriptInferenceUtil.getInferredType
import com.badahori.creatures.plugins.intellij.agenteering.caos.hints.CaosScriptDoifFoldingBuilder.Companion.IS
import com.badahori.creatures.plugins.intellij.agenteering.caos.hints.CaosScriptDoifFoldingBuilder.Companion.IS_NOT
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.EqOp.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType.AGENT
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.textUppercase
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * Folder for DOIF statement equality expressions
 * Takes a command like doif CAGE gt 3 -> "Is Older Than Child
 */
class CaosScriptDoifFoldingBuilder : FoldingBuilderEx(), DumbAware {

    /**
     * Gets command call placeholder text for folding if it should be folded
     */
    override fun getPlaceholderText(node: ASTNode): String? {
        (node.psi as? CaosScriptEqualityExpressionPrime)?.let { expression ->
            expression.getUserData(CACHE_KEY)?.let {
                if (it.first == expression.text)
                    return it.second
            }
            if (shouldFold(expression))
                return getDoifFold(expression)
        }
        return null
    }

    /**
     * Gets the folded text for this command call
     */
    private fun getDoifFold(expression: CaosScriptEqualityExpressionPrime): String? {
        ProgressIndicatorProvider.checkCanceled()
        val variant = expression.variant
            ?: return null

        val eqOp = EqOp.fromValue(expression.eqOp.text)
        if (eqOp == INVALID) {
            expression.putUserData(CACHE_KEY, Pair(expression.text, null))
            return null
        }
        val firstExpression = expression.first
        val secondExpression = expression.second
            ?: return null
        var text = formatComparison(variant, eqOp, firstExpression, secondExpression, false)
            ?: formatComparison(variant, eqOp, secondExpression, firstExpression, true)

        text = if (text like expression.text)
            null
        else
            text?.let { homogenizeFormattedText(it) }
        expression.putUserData(CACHE_KEY, Pair(expression.text, text))
        return text
    }

    /**
     * Base method to build the regions that should be folded
     */
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val group = FoldingGroup.newGroup("CaosScript_STIM_FOLDING")
        // Get a collection of the literal expressions in the document below root
        val children = PsiTreeUtil.findChildrenOfType(root, CaosScriptEqualityExpressionPrime::class.java)
        if (children.size > 400)
            return emptyArray()
        return children
            .filter {
                ProgressIndicatorProvider.checkCanceled()
                shouldFold(it)
            }
            .mapNotNull {
                ProgressIndicatorProvider.checkCanceled()
                getFoldingRegion(it, group)
            }
            .toTypedArray()
    }

    // Helper function to get actual folding regions for command calls
    private fun getFoldingRegion(
        expression: CaosScriptEqualityExpressionPrime,
        group: FoldingGroup
    ): FoldingDescriptor? {
        if (!shouldFold(expression))
            return null
        return FoldingDescriptor(expression.node, expression.textRange, group)
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = true

    /**
     * Determines whether or not to actually fold this command
     */
    private fun shouldFold(expression: CaosScriptEqualityExpressionPrime): Boolean {
        val variant = expression.variant
            ?: return false

        // Checks cached value first to prevent needing to check all arguments
        expression.getUserData(CACHE_KEY)?.let {
            if (it.first == expression.text)
                return it.second != null
        }

        // Get left side argument
        val first = expression.first

        // Get right side argument
        val second = expression.second
        // If second argument is null, there is nothing to compare
            ?: return false

        val firstCommand = first.commandStringUpper
        val secondCommand = second.commandStringUpper
        val isP1P2 = firstCommand in P1P2 || secondCommand in P1P2
        // Get containing script number to find a possible _P1_ or _P2_ named value
        val containingScriptNumber = (if (isP1P2 && variant.isNotOld)
            first.getParentOfType(CaosScriptEventScript::class.java)?.eventNumber
        else null) ?: -1
        // Check if any of the values has a value list
        return listOf(firstCommand, secondCommand).intersect(foldableChemicals).isNotEmpty()
                || AGENT in listOf(getInferredType(first, false), getInferredType(second, false))
                || getValuesList(variant, first) != null
                || getValuesList(variant, second) != null
                || first.text like "null"
                || second.text like "null"
                || (isP1P2 && hasParamName(variant, containingScriptNumber, first))
                || (isP1P2 && hasParamName(variant, containingScriptNumber, second))
                || "KEYD" in listOfNotNull(first.commandStringUpper, second.commandStringUpper)
    }

    private fun hasParamName(variant: CaosVariant, containingScriptNumber: Int, first: CaosScriptRvalue): Boolean {
        return getParamName(variant, containingScriptNumber, first) != null
    }


    companion object {

        val foldableChemicals = listOf("CHEM", "DRIV", "DRV!")

        /**
         * Takes comparison operator properties and formats them
         * @param variant the CAOS variant of this expression
         * @param eqOp the equality operation enum value
         * @param thisValue the base value to build the other value from
         * @param otherValue the value to convert from int to another value
         */
        fun formatComparison(
            variant: CaosVariant,
            eqOp: EqOp,
            thisValue: CaosScriptRvalue,
            otherValue: CaosScriptRvalue,
            reversed: Boolean
        ): String? {

            if (thisValue.textUppercase in P1P2) {
                // If is _P1_ or _P2_ get replacement value if any
                // This may return a less than stellar result
                // if P1 is compared to another command
                return onP1P2(variant, eqOp, thisValue, otherValue)
            }
            // Get command definition
            val command = thisValue.commandDefinition
            // If there is no command definition,
            // then there is no way to get values lists for argument
                ?: return null

            val commandName = command.command
            // If this is not an int value, there is no way to convert it to a list value
            val otherValueInt = otherValue.intValue

            // Check if foldable
            val hasDriveOrChemical = commandName likeAny foldableChemicals
            if (otherValueInt == null && !hasDriveOrChemical && otherValue.commandDefinition?.returnType != AGENT)
                return null


            // Formats the primary value as a drive or chemical name as needed
            val thisValueAsDriveOrChemical: String? = if (hasDriveOrChemical && commandName notLike "DRV!")
                thisValue.rvaluePrime?.let { rvaluePrime ->
                    formatChemicalValue(variant, rvaluePrime)
                }
            else
                null
            // Package parameters for use in formatting
            // Package was build to prevent having to pass so many parameters to each unique formatting function
            var formatInfo = FormatInfo(
                command = command,
                variant = variant,
                thisValue = formatPrimary(thisValueAsDriveOrChemical ?: thisValue.text),
                thisValuesArgs = thisValue.arguments.map { it.text },
                thisValueType = thisValue.inferredType,
                otherValueInt = otherValue.text.toIntSafe(),
                reversed = reversed,
                eqOp = eqOp,
                isBool = false,
                boolOnLessThan = false,
                otherValue = formatPrimary(otherValue.text)
            )
            // Get return value of "thisValue" as a values list
            if (otherValueInt != null && (!hasDriveOrChemical || commandName like "DRV!")) {
                formatInfo = getFormatInfoWithValuesList(formatInfo, otherValueInt)

                // Ensure that info contains either other value text, or a chemical name
                if (formatInfo.otherValue.nullIfEmpty() == null)
                    return DEFAULT_FORMATTER(
                        formatInfo.copy(
                            otherValue = formatPrimary(otherValue.text)
                        )
                    )
            }

            // Ensure that eq operation actually has a format to use
            val pattern = command.doifFormat
                ?: return DEFAULT_FORMATTER(formatInfo)


            // Resolve format to an equation
            val format = resolvePattern(pattern)
            // TODO should text be uniformly formatted
            return format(formatInfo)
        }

        /**
         * Formats the other value using a values list for the command
         */
        private fun getFormatInfoWithValuesList(formatInfo: FormatInfo, otherValueInt: Int): FormatInfo {
            val returnValuesList = formatInfo.command.returnValuesList[formatInfo.variant]
            val eqOp = formatInfo.eqOp
            // Get extension value type
            val extensionType = returnValuesList?.extensionType
            // Check whether value type is bool like
            val isBool = extensionType?.startsWith("bool") ?: false
            // Whether or not to convert "<" to "is". This is based on the extension value
            val boolOnLessThan = isBool && extensionType notLike "bool:gt"
            var doubleNegative = isBool && otherValueInt == 0 && (eqOp == GREATER_THAN || eqOp == NOT_EQUAL)

            // Get other value from its values list value
            val otherValueText = returnValuesList
                // Get value as bitflags if possible
                ?.getWithBitFlags(otherValueInt)
                // If bitflags is empty -> nullify to prevent empty string
                ?.nullIfEmpty()
                // Join bitflags to single string for use in doif format
                ?.joinToString(", ") { it.name }
            // Get the other value as a values list value
                ?: otherValueInt.let {
                    returnValuesList?.get(it)?.name?.let { trueValue ->
                        var out = trueValue
                        if (doubleNegative) {
                            if (trueValue.toLowerCase()
                                    .let { lower ->
                                        lower.startsWith("not")
                                                && !lower.startsWith("noth") // Ensure it != 'Nothing'
                                    }
                            ) {
                                out = returnValuesList[1]?.name ?: out
                            }
                        }
                        // If value is not replaced, then do not invert Eq/NE
                        doubleNegative = out != trueValue
                        out
                    }
                }
            return formatInfo.copy(
                eqOp = if (doubleNegative) EQUAL else eqOp,
                isBool = isBool,
                boolOnLessThan = boolOnLessThan,
                otherValue = formatPrimary(otherValueText ?: "")
            )
        }

        /**
         * Formats the primary value to a Drive or chemical name based on its first parameter
         */
        private fun formatChemicalValue(variant: CaosVariant, rvaluePrime: CaosScriptRvaluePrime?): String? {
            if (rvaluePrime == null)
                return null
            val commandString = rvaluePrime.commandStringUpper
            if (commandString likeNone foldableChemicals)
                return null
            val chemicalIndex = rvaluePrime.arguments.firstOrNull()?.text?.toIntSafe()
                ?: return null
            return when (commandString) {
                "CHEM" -> CaosLibs[variant].valuesList("Chemicals")
                    ?.get(chemicalIndex)?.name
                    ?: "Chemical $chemicalIndex"
                "DRIV" -> CaosLibs[variant].valuesList("Drives")
                    ?.get(chemicalIndex)?.name
                    ?: "Driv $chemicalIndex"
                else -> null
            }
        }

        private fun resolvePattern(format: String): Formatter {
            if (format.trim() == "%%")
                return DEFAULT_FORMATTER
            if (!format.startsWith("%"))
                return createCompoundFormatter(format)
            return when (format.toUpperCase()) {
                // Though %ATTRIBUTES is simple, it is registered here for consistency between the two versions of the command
                "%ATTRIBUTES" -> createCompoundFormatter("Has Attributes: {1}::Attributes Not {1}::%%")
                "%CAGE" -> CAGE
                "%HIST_CAGE" -> HIST_CAGE
                "%IS_SIMPLE" -> SIMPLE_FORMATTER
                "%TIME" -> TIME
                "%KEYD" -> KEYD
                else -> {
                    LOGGER.severe("Failed to understand %pattern: $format")
                    DEFAULT_FORMATTER
                }
            }
        }

        internal const val IS = "is"
        internal const val IS_NOT = "is not"
        private val CACHE_KEY =
            Key<Pair<String, String?>>("com.badahori.creatures.plugins.intellij.agenteering.caos.DOIF_FOLDING_STRING")
    }

}

internal val P1P2 = listOf("_P1_", "_P2_")

/**
 * Simple lambda to take formatting parameters and format them as needed
 */
private typealias Formatter = (formatInfo: FormatInfo) -> String

// Helper function to quickly get the values list for this rvalue
private fun getValuesList(variant: CaosVariant, expression: CaosScriptRvalue): CaosValuesList? {
    return expression.commandDefinition?.returnValuesList?.get(variant)
}


// Ensures replacement of possibly two eq value expressions
private val EQ_IS_REGEX = "\\{(is|eq)}".toRegex()

/**
 * Holds information that might be necessary to format a doif comparison expression
 */
data class FormatInfo(
    val command: CaosCommand,
    val variant: CaosVariant,
    val eqOp: EqOp,
    val isBool: Boolean,
    val boolOnLessThan: Boolean,
    val thisValue: String,
    val thisValueType: CaosExpressionValueType,
    val thisValuesArgs: List<String>,
    val otherValue: String,
    val otherValueInt: Int?,
    val reversed: Boolean
)


/**
 * Formats a value with a simple "is" or "is not" prefix.
 * ie "is Frozen" "is not Inactive"
 */
private val SIMPLE_FORMATTER: Formatter = { formatInfo: FormatInfo ->
    val eqOpText = formatEqOp(formatInfo.eqOp, formatInfo.isBool, formatInfo.boolOnLessThan, formatInfo.otherValueInt)
    if (eqOpText == IS || eqOpText == IS_NOT) {
        "$eqOpText ${formatInfo.otherValue}"
    } else {
        DEFAULT_FORMATTER(formatInfo)
    }
}

/**
 * Default format is  "{0} {is} {1}"
 */
private val DEFAULT_FORMATTER: Formatter = formatter@{ formatInfo: FormatInfo ->
    val thisValue = formatInfo.thisValue
    val otherValueInt = formatInfo.otherValueInt
    if (otherValueInt == 0 && formatInfo.thisValueType == AGENT) {
        val eqOpText = formatEqOp(
            formatInfo.eqOp,
            isBool = true,
            boolOnLessThan = false,
            otherValueInt = otherValueInt
        )
        if (eqOpText == IS || eqOpText == IS_NOT)
            return@formatter "$thisValue $eqOpText ${formatPrimary("NULL")}"
    }
    // Get Eq op text, converting "=" -> "is", "!=" -> "is not" and if bool and 0  ">" -> "is not"
    val eqOpText = formatEqOp(formatInfo.eqOp, formatInfo.isBool, formatInfo.boolOnLessThan, formatInfo.otherValueInt)

    // Get other value to simplify code reading
    val otherValue = formatInfo.otherValue
    // Return first and second to original order if needed
    val first = if (formatInfo.reversed) otherValue else thisValue
    val second = if (formatInfo.reversed) thisValue else otherValue
    "$first $eqOpText $second"
}

/**
 * Formats a string using both left and right side values along with arguments and chemicals/drives
 */
private fun createCompoundFormatter(formatIn: String): Formatter = func@{ formatInfo: FormatInfo ->
    // Get "is" or "is not" text
    val eqOpText = formatEqOp(formatInfo.eqOp, formatInfo.isBool, formatInfo.boolOnLessThan, formatInfo.otherValueInt)
    // Test for "is" or "is not" and return appropriate sub format
    var out = formatIn.split("::").let { patterns ->
        when {
            eqOpText == IS -> patterns.first()
            eqOpText != IS_NOT -> patterns.last()
            patterns.size > 2 -> patterns[1]
            else -> patterns.first()
        }.trim()
    }

    if (out == "%%")
        return@func DEFAULT_FORMATTER(formatInfo)

    // Replace eq/is in format
    out = out.replace(EQ_IS_REGEX, eqOpText)

    // Get left side rvalues arguments
    val thisValuesArgs = formatInfo.thisValuesArgs

    // Check if argument replacement is necessary
    if (out.contains("{0:")) {
        // Insert all requested arguments. In format they are {0:i}
        for (i in thisValuesArgs.indices) {
            out = out.replace("{0:$i}", thisValuesArgs[i])
        }
    }

    // Replace left and right side values and return
    out.replace("{0}", formatInfo.thisValue).replace("{1}", formatInfo.otherValue)
}

/**
 * Format CAGE command to a more readable format
 * Take CAGE > 0 to Is Older Than an Infant
 */
private val CAGE: Formatter = { formatInfo: FormatInfo ->
    val otherValue = formatInfo.otherValue
    when (formatInfo.eqOp) {
        EQUAL -> "Is $otherValue"
        NOT_EQUAL -> "Is not $otherValue"
        GREATER_THAN -> "Is older than a $otherValue"
        LESS_THAN -> "Is younger than a $otherValue"
        GREATER_THAN_EQUAL -> "Is $otherValue or Older"
        LESS_THAN_EQUAL -> "Is $otherValue or Younger"
        else -> DEFAULT_FORMATTER(formatInfo)
    }
}

/**
 * Formats a HIST CAGE eq operation
 */
private val HIST_CAGE = formatter@{ formatInfo: FormatInfo ->
    val moniker = formatInfo.thisValuesArgs.firstOrNull()?.nullIfEmpty()
        ?: return@formatter DEFAULT_FORMATTER(formatInfo)
    val root = CAGE(formatInfo)
    (formatInfo.thisValuesArgs.getOrNull(1))?.let {
        "'$moniker' $root at Event#$it"
    } ?: return@formatter DEFAULT_FORMATTER(formatInfo)
}

/**
 * Formats a time based eq statement
 */
private val TIME: Formatter = formatter@{ formatInfo: FormatInfo ->
    val otherValue = formatInfo.otherValue
    when (formatInfo.eqOp) {
        EQUAL -> "Is $otherValue"
        NOT_EQUAL -> "Is not $otherValue"
        GREATER_THAN -> "Is later than $otherValue"
        LESS_THAN -> "Is earlier than $otherValue"
        GREATER_THAN_EQUAL -> "Is $otherValue or Later"
        LESS_THAN_EQUAL -> "Is $otherValue or Earlier"
        else -> DEFAULT_FORMATTER(formatInfo)
    }
}

/**
 * Gets the DOIF fold when _P1_ or _P2_
 */
private fun onP1P2(
    variant: CaosVariant,
    eqOp: EqOp,
    thisValue: CaosScriptRvalue,
    otherValue: CaosScriptRvalue
): String? {
    // Gets event number safely,
    // as an infinite loop is created if calling before index is build
    val eventScriptNumber = (if (DumbService.isDumb(thisValue.project))
        thisValue.getParentOfType(CaosScriptEventScript::class.java)?.eventNumberElement?.text?.toIntOrNull()
    else
        thisValue.getParentOfType(CaosScriptEventScript::class.java)?.eventNumber)
        ?: return null

    // Get the parameter name value as parts.
    // Parameters can be in format paramName@valuesListName
    // ie keyCode@KeyCodes
    val paramNameParts = getParamName(variant, eventScriptNumber, thisValue)?.split("@")
        ?: return null
    // Get param name without trailing values list name
    val paramName = paramNameParts[0]

    // Get the other value as literal text value, or values list value if available
    val otherValueName = paramNameParts.getOrNull(1)?.let otherValue@{ valuesListName ->
        val key = otherValue.intValue
        // Other value is not int. Bail out as there will be no value found
            ?: return@otherValue null
        // Get the associated values list
        val valuesList = CaosLibs[variant].valuesList(valuesListName)
            ?: return@otherValue null
        // Get the associated value
        valuesList[key]?.name
    } ?: otherValue.text
    // Get formatted eq op value
    val eqOpString = formatEqOp(eqOp, isBool = true, boolOnLessThan = false, otherValueInt = otherValue.intValue)
    // Get formatted string
    return "$paramName $eqOpString $otherValueName"
}

/**
 * Formats a KEYD eq operation
 */
private val KEYD = formatter@{ formatInfo: FormatInfo ->
    val keycodeAsString = formatInfo.command
        .parameters
        .getOrNull(0)
        ?.valuesList
        ?.get(formatInfo.variant)
        ?.get(formatInfo.thisValuesArgs.getOrNull(0) ?: "")
        ?.name
    val key = keycodeAsString?.let { "'$it' Key" } ?: ("Key #" + (formatInfo.thisValuesArgs.getOrNull(0) ?: "??"))
    val equals = formatEqOp(formatInfo.eqOp, true, boolOnLessThan = false, otherValueInt = formatInfo.otherValueInt)
    val other = formatInfo.otherValue
    "$key $equals $other"
}

/**
 * Gets a proper "is", "is not" or raw eq value
 */
private fun formatEqOp(eqOp: EqOp, isBool: Boolean, boolOnLessThan: Boolean, otherValueInt: Int?): String {
    return when (eqOp) {
        EQUAL -> if (isBool) IS else null
        NOT_EQUAL -> if (isBool) IS_NOT else null
        GREATER_THAN -> if (isBool && otherValueInt == 0) IS_NOT else null
        LESS_THAN -> if (isBool && otherValueInt != null && otherValueInt != 0 && boolOnLessThan) IS_NOT else null
        else -> null
    } ?: eqOp.values.getOrNull(1) ?: eqOp.values.first()
}

private typealias FormatString = (text: String) -> String

/**
 * Formats text so only first letter of string is upper cased
 */
private val formatUpperCaseFirst: FormatString = { text: String ->
    text.toLowerCase().upperCaseFirstLetter()
}

/**
 * Formats text so all text is lower cased
 */
private val allLowerCase: FormatString = { text: String ->
    text.toLowerCase()
}

/**
 * Formats text so all text is UPPER case
 */
@Suppress("unused")
private val allUpperCase: FormatString = { text: String ->
    text.toUpperCase()
}

/**
 * Formats text so each new word is upper cased
 */
@Suppress("unused")
private val upperCaseFirstOnAllWords: FormatString = { text: String ->
    text.split(" ").joinToString(" ") {
        if (it like "an" || it like "a" || it like "or")
            it
        else
            it.upperCaseFirstLetter()
    }
}

private val noChange: FormatString = { string: String -> string }

/**
 * Formats text to unify case of words.
 * @TODO should the words all be upper-cased or only first letter
 * ie Is Not Dead <> Is not dead <> is not dead <> is not Dead
 */
private val homogenizeFormattedText: FormatString get() = noChange


/**
 * Formatter for comparison's left and right replacement values
 */
private val formatPrimary: FormatString = noChange

/**
 * Gets a _P1_ or _P2_ name given an event script number
 */
private fun getParamName(variant: CaosVariant, containingScriptNumber: Int, thisValue: CaosScriptRvalue): String? {

    // No named parameters exist or are known for C1 or C2
    if (variant.isOld)
        return null

    // Get list for _P1_ or _P2_ token
    val list = when (thisValue.text.toUpperCase()) {
        "_P1_" -> CaosLibs[variant].valuesList("_P1_")
        "_P2_" -> CaosLibs[variant].valuesList("_P2_")
        else -> null
    } ?: return null
    // Get name for containing script number
    return list[containingScriptNumber]?.name
}