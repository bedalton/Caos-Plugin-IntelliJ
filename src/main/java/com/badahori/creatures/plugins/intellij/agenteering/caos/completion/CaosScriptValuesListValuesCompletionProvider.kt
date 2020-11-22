package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosValuesList
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInsight.completion.AddSpaceInsertHandler
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

/**
 * Adds completions for known value lists
 */
object CaosScriptValuesListValuesCompletionProvider {

    private val startsWithNumber = "^[0-9].*".toRegex()

    /**
     * Adds completions for a known value list, based on argument position and parent command
     */
    fun addParameterTypeDefValueCompletions(resultSet: CompletionResultSet, valueOfType: CaosScriptArgument) {
        val containingCommand = (valueOfType.parent as? CaosScriptCommandElement)
                ?: return
        // Get variant as it is required to get the right list of values
        val variant = valueOfType.containingCaosFile?.variant
                ?: return

        // Get argument index
        val index = valueOfType.index

        val case = valueOfType.text.case

        if (containingCommand is CaosScriptCAssignment) {
            containingCommand.lvalue
                    ?.commandDefinition
                    ?.returnValuesList
                    ?.get(variant)
                    ?.let { valuesList ->
                        LOGGER.info("Added values list for LValue ${containingCommand.lvalue?.commandStringUpper} in ${containingCommand.commandStringUpper}")
                        // Finally, add basic completion for list values
                        addListValues(resultSet, valuesList, case, false)
                        return
                    }
            LOGGER.info("Failed to find completions for lvalue: ${containingCommand.lvalue?.text} in ${containingCommand.commandStringUpper}")
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
        if (valueOfType.parent is CaosScriptGenus || (index != 0 && parameterStruct.name.equalsIgnoreCase("genus"))) {
            if (startsWithNumber.matches(valueOfType.text))
                return
            val family = valueOfType.getPreviousNonEmptySibling(false)?.text?.toIntSafe()
                    ?: return
            addGenusCompletions(resultSet, variant, case, family, numParameters > index + 1)
            return

        // If argument is FAMILY, Allow for name based family completions
        } else if (valueOfType.parent is CaosScriptFamily || (valueOfType.parent !is CaosScriptFamily && parameterStruct.name.equalsIgnoreCase("family"))) {
            val next = parameters.getOrNull(index + 1)
                    ?: return
            if (next.name.equalsIgnoreCase("genus"))
                addGenusCompletions(resultSet, variant, case,null, numParameters > index + 1)
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
                        ?: return

        // Retrieve values list by variant
        val valuesList = valuesListGetter[variant]
                ?: return

        // Check if need to add space after insertion
        val addSpace = numParameters - 1 > index

        // If type definition contains values list annotation, add list values
        // If supertype == Files, should complete with known file names for type
        if (valuesList.extensionType?.equalsIgnoreCase("File").orFalse()) {
            addFileNameCompletions(resultSet, valueOfType.project, valueOfType.containingFile?.module, variant.isOld, parameterStruct.type, valuesList.name)
            return
        }

        // Finally, add basic completion for list values
        addListValues(resultSet, valuesList, case, addSpace)
    }

    /**
     * Fill in completions for GENUS int value
     */
    private fun addGenusCompletions(resultSet: CompletionResultSet, variant: CaosVariant, case:Case, family: Int?, addSpace: Boolean) {

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
            resultSet.addElement(PrioritizedLookupElement.withPriority(lookupElement, 900.0))
        }
    }

    /**
     * Add completion for expressions, based on opposing values
     */
    fun addEqualityExpressionCompletions(variant: CaosVariant, resultSet: CompletionResultSet, case:Case, equalityExpression: CaosScriptEqualityExpressionPrime, expression: CaosScriptRvalue) {
        val valuesList = equalityExpression.getValuesList(variant, expression)
                ?: return
        LOGGER.info("ValuesListForCompletions is ${valuesList.name}")
        addListValues(resultSet, valuesList, case, expression.getPreviousNonEmptyNode(false) !is CaosScriptEqOp)
    }

    /**
     * Actually add list values from previously determined lists
     */
    private fun addListValues(resultSet: CompletionResultSet, list: CaosValuesList, case:Case, addSpace: Boolean) {
        val values = list.values
        // If values list is bitflags, create bit-flags builder dialog
        if (list.extensionType?.equalsIgnoreCase("BitFlags").orFalse()) {
            val builderLabel = "${list.name} bit-flag builder"
            val lookupElement = PrioritizedLookupElement.withPriority(LookupElementBuilder
                    .create("")
                    .withLookupString(builderLabel)
                    .withPresentableText(builderLabel)
                    .withInsertHandler(GenerateBitFlagIntegerAction(list.name, values)), 1000.0)
            resultSet.addElement(lookupElement)
        }

        // Actually add lookup element completion values
        for (value in values) {
            val name = value.name.matchCase(case)
            var lookupElement = LookupElementBuilder
                    .create(value.value)
                    .withLookupString(name)
                    .withPresentableText(name + " - " + value.value)
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
            resultSet.addElement(PrioritizedLookupElement.withPriority(lookupElement, 900.0))
        }
    }

    /**
     * Add completions for file names based on the lists name. (ie. 'File.SPR', 'File.S16/C16')
     */
    private fun addFileNameCompletions(resultSet: CompletionResultSet, project: Project, module:Module?, isOldVariant:Boolean, parameterType:CaosExpressionValueType, valuesListName:String) {
        // RValue requires a file type, so fill in filenames with matching types
        // ValuesList name is in format 'File.{extension}' or 'File.{extension/extension}'
        val types = valuesListName.substring(5).split("/")
        // Get all files of for filetype list
        // Flat map is necessary for CV-SM, as they can take C16 or S16
        val allFiles = types
                .flatMap { fileExtensionTemp ->
                    val fileExtension = fileExtensionTemp.toLowerCase()
                    val searchScope =
                            module?.let { GlobalSearchScope.moduleScope(it) }
                                    ?: GlobalSearchScope.projectScope(project)
                    FilenameIndex.getAllFilesByExt(project, fileExtension, searchScope).toList()
                }
                .map { it.nameWithoutExtension }
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
            resultSet.addElement(lookupElement)
        }
    }

}