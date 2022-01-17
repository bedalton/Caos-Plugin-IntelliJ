package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosValuesList
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPreviousNonEmptyNode
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPreviousNonEmptySibling
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getValuesList
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInsight.completion.AddSpaceInsertHandler
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import icons.CaosScriptIcons

/**
 * Adds completions for known value lists
 */
object CaosScriptValuesListValuesCompletionProvider {

    private val startsWithNumber = "^[0-9].*".toRegex()

    /**
     * Adds completions for a known value list, based on argument position and parent command
     */
    fun addParameterTypeDefValueCompletions(
        resultSet: CompletionResultSet,
        variant: CaosVariant,
        argument: CaosScriptArgument,
        isExtendedCompletion:Boolean
    ) {
        val containingCommand = (argument.parent as? CaosScriptCommandElement)
            ?: return

        // Get argument index
        val index = argument.index

        val case = argument.text.case

        if (containingCommand is CaosScriptCAssignment) {
            containingCommand.lvalue
                ?.commandDefinition
                ?.returnValuesList
                ?.get(variant)
                ?.let { valuesList ->
                    // Finally, add basic completion for list values
                    addListValues(resultSet, valuesList, case, false, argument !is CaosScriptRvalue)
                    return
                }
        }
        // Get parent command entry in CaosDef
        val reference = containingCommand
            .commandDefinition
            ?: return
        // Resolve arguments corresponding parameter definition
        val parameters = reference
            .parameters

        // Get number of parameters
        val numParameters = parameters.size

        // Get arguments parameter definition
        val parameterStruct = parameters.getOrNull(index)
            ?: return

        // If argument represents GENUS, allow for named genus completions
        if (argument.parent is CaosScriptGenus || (index != 0 && parameterStruct.name.equalsIgnoreCase("genus"))) {
            if (startsWithNumber.matches(argument.text))
                return
            val family = argument.getPreviousNonEmptySibling(false)?.text?.toIntSafe()
                ?: return
            addGenusCompletions(resultSet, variant, case, family, numParameters > index + 1)
            return

            // If argument is FAMILY, Allow for name based family completions
        } else if (argument.parent is CaosScriptFamily || (argument.parent !is CaosScriptFamily && parameterStruct.name.equalsIgnoreCase(
                "family"
            ))
        ) {
            val next = parameters.getOrNull(index + 1)
                ?: return
            if (next.name.equalsIgnoreCase("genus"))
                addGenusCompletions(resultSet, variant, case, null, numParameters > index + 1)
            return
        }

        // Get completion values list getter.
        // Getter stores list of ids by variant
        val valuesListGetter =
            // If is assignment, get expected completion values
            (if (containingCommand is CaosScriptCAssignment) {
                containingCommand.lvalue
                    ?.commandDefinition
                    ?.returnValuesList
                // Completion is for parameter
            } else {
                parameterStruct.valuesList
            })


        // Does a space need to be added insertion
        val addSpace = numParameters - 1 > index

        if (valuesListGetter == null) {
            if (isExtendedCompletion) {
                addAllListValues(resultSet, variant, case, addSpace)
            }
            return
        }

        // Retrieve values list by variant
        val valuesList = valuesListGetter[variant]
            ?: return


        // If type definition contains values list annotation, add list values
        // If supertype == Files, should complete with known file names for type
        if (valuesList.extensionType?.equalsIgnoreCase("File").orFalse()) {
            addFileNameCompletions(
                resultSet,
                argument.project,
                argument.containingFile?.module,
                variant.isOld,
                parameterStruct.type,
                valuesList.name
            )
            return
        }

        // Finally, add basic completion for list values
        addListValues(resultSet, valuesList, case, addSpace, argument !is CaosScriptRvalue)
    }

    /**
     * Fill in completions for GENUS int value
     */
    private fun addGenusCompletions(
        resultSet: CompletionResultSet,
        variant: CaosVariant,
        case: Case,
        family: Int?,
        addSpace: Boolean
    ) {

        // Locate genus list for variant
        val genusValuesList = CaosLibs.valuesLists[variant].firstOrNull { it.name.equalsIgnoreCase("Genus") }
            ?: return
        // If family is zero, it is a wild card matching anything
        // Genus completions do not apply
        if (family == 0) {
            return
        }

        // Get values filtering by family as needed
        val hasFamily = family != null
        val values = if (hasFamily)
            genusValuesList.values.filter { it.value.startsWith("$family") }
        else
            genusValuesList.values

        // Loop through all filtered values for completion items
        for (value in values) {
            ProgressIndicatorProvider.checkCanceled()
            val key = if (hasFamily) {
                value.value.split(" ").lastOrNull() ?: continue
            } else
                value.value
            var lookupElement = LookupElementBuilder
                .create(key)
                .withLookupString(value.name.matchCase(case))
                .withPresentableText(value.value + ": " + value.name)
                .withTailText(value.description)
            if (addSpace) {
                lookupElement = lookupElement
                    .withInsertHandler(AddSpaceInsertHandler(true))
            }
            resultSet.addElement(PrioritizedLookupElement.withPriority(lookupElement.withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE), 900.0))
        }
    }

    /**
     * Add completion for expressions, based on opposing values
     */
    fun addEqualityExpressionCompletions(
        variant: CaosVariant,
        resultSet: CompletionResultSet,
        case: Case,
        equalityExpression: CaosScriptEqualityExpressionPrime,
        expression: CaosScriptRvalue,
        isExtendedCompletion: Boolean
    ) {
        val valuesList = equalityExpression.getValuesList(variant, expression)
        if (valuesList == null) {
            if (isExtendedCompletion) {
                addAllListValues(resultSet, variant, case, addSpace = expression.getPreviousNonEmptyNode(false) !is CaosScriptEqOp)
            }
            return
        }
        addListValues(resultSet, valuesList, case, expression.getPreviousNonEmptyNode(false) !is CaosScriptEqOp, false)
    }

    /**
     * Actually add list values from previously determined lists
     */
    private fun addListValues(resultSet: CompletionResultSet, list: CaosValuesList, case: Case, addSpace: Boolean, prefixValueWithSpace: Boolean) {
        val values = list.values.let { values ->
            if (values.all { it.intValue != null })
                values.sortedBy { it.intValue }
            else
                values.sortedBy { it.name }
        }
        // If values list is bitflags, create bit-flags builder dialog
        if (list.extensionType?.equalsIgnoreCase("BitFlags").orFalse()) {
            val builderLabel = "${list.name} bit-flag builder"
            val lookupElement = PrioritizedLookupElement.withPriority(
                LookupElementBuilder
                    .create("")
                    .withIcon(CaosScriptIcons.VALUE_LIST_VALUE)
                    .withLookupString(builderLabel)
                    .withPresentableText(builderLabel)
                    .withInsertHandler(GenerateBitFlagInsertHandler(list.name, values, prefixValueWithSpace))
                    .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE), 1000.0
            )
            resultSet.addElement(lookupElement)
        }
        // Actually add lookup element completion values
        for (value in values) {
            val name = value.name.matchCase(case)
            var lookupElement = LookupElementBuilder
                .create(value.value)
                .withLookupString(name)
                .withIcon(CaosScriptIcons.VALUE_LIST_VALUE)
                .withPresentableText(value.value + "-" + name)

            if (value.description.nullIfEmpty() != null)
                lookupElement = lookupElement
                    .withTailText(" (${value.description})")

            // If value is disabled, strike it through
            if (value.name.contains("(Disabled)")) {
                lookupElement = lookupElement.withStrikeoutness(true)
            }
            // Add space insert handler if needed
            if (addSpace) {
                lookupElement = lookupElement
                    .withInsertHandler(SpaceAfterInsertHandler)
            }
            resultSet.addElement(PrioritizedLookupElement.withPriority(lookupElement.withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE), 900.0))
        }
    }

    /**
     * Actually add list values from previously determined lists
     */
    private fun addAllListValues(resultSet: CompletionResultSet, variant:CaosVariant, case: Case, addSpace: Boolean) {
        for (list in CaosLibs[variant].valuesLists) {
            val listName = list.name
            val values = list.values.let { values ->
                if (values.all { it.intValue != null })
                    values.sortedBy { it.intValue }
                else
                    values.sortedBy { it.name }
            }
            // Actually add lookup element completion values
            for (value in values) {
                val name = value.name.matchCase(case)
                var lookupElement = LookupElementBuilder
                    .create(value.value)
                    .withLookupString(name)
                    .withIcon(CaosScriptIcons.VALUE_LIST_VALUE)
                    .withPresentableText(value.value + "-" + name)
                if (value.description.nullIfEmpty() != null)
                    lookupElement = lookupElement
                        .withTailText(" @$listName")

                // If value is disabled, strike it through
                if (value.name.contains("(Disabled)")) {
                    lookupElement = lookupElement.withStrikeoutness(true)
                }
                // Add space insert handler if needed
                if (addSpace) {
                    lookupElement = lookupElement
                        .withInsertHandler(SpaceAfterInsertHandler)
                }
                resultSet.addElement(PrioritizedLookupElement.withPriority(lookupElement.withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE), -100.0))
            }
        }
    }
}

/**
 * Add completions for file names based on the lists name. (i.e. 'File.SPR', 'File.S16/C16')
 */
private fun addFileNameCompletions(
    resultSet: CompletionResultSet,
    project: Project,
    module: Module?,
    isOldVariant: Boolean,
    parameterType: CaosExpressionValueType,
    valuesListName: String
) {
    // RValue requires a file type, so fill in filenames with matching types
    // ValuesList name is in format 'File.{extension}' or 'File.{extension/extension}'
    val types = valuesListName.substring(5).split("/")
    // Get all files of for filetype list
    // Flat map is necessary for CV-SM, as they can take C16 or S16
    val allFiles = types
        .flatMap { fileExtensionTemp ->
            val fileExtension = fileExtensionTemp.lowercase()
            val searchScope =
                module?.let { GlobalSearchScope.moduleScope(it) }
                    ?: GlobalSearchScope.projectScope(project)
            FilenameIndex.getAllFilesByExt(project, fileExtension, searchScope).toListOf()
        }
        .flatMap { files -> files.map { it.nameWithoutExtension } }
    // Loop through all files and format them as needed.
    for (file in allFiles) {
        val isToken = parameterType == CaosExpressionValueType.TOKEN
        val text = when {
            isToken -> file
            isOldVariant -> "[$file"
            else -> "\"$file"
        }
        val openQuote = when {
            isToken -> null
            isOldVariant -> "["
            else -> "\""
        }
        val closeQuote = when {
            isToken -> null
            isOldVariant -> "]"
            else -> "\""
        }
        // Create lookup element
        var lookupElement = LookupElementBuilder
            .create(text)
            .withStrikeoutness(isToken && file.length != 4)
            .withLookupString("$openQuote$file$closeQuote")
            .withPresentableText(file)
        if (closeQuote != null) {
            lookupElement = lookupElement
                .withInsertHandler(CloseQuoteInsertHandler(closeQuote))
        }
        resultSet.addElement(
            lookupElement
            .withAutoCompletionPolicy(AutoCompletionPolicy.NEVER_AUTOCOMPLETE)
        )
    }
}