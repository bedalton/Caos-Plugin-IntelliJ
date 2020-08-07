package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefValuesListDefinitionElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.impl.containingCaosDefFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.ValuesListEq
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.isVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getPreviousNonEmptyNode
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getSelfOrParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.matchCase
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.toIntSafe
import com.intellij.codeInsight.completion.AddSpaceInsertHandler
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

/**
 * Adds completions for known value lists
 */
object CaosScriptValuesListValuesCompletionProvider {

    /**
     * Adds completions for a known value list, based on argument position and parent command
     */
    fun addParameterTypeDefValueCompletions(resultSet: CompletionResultSet, valueOfType: CaosScriptExpectsValueOfType) {
        val containingCommand = valueOfType.getSelfOrParentOfType(CaosScriptCommandElement::class.java)
                ?: return
        val index = valueOfType.index
        // Get parent command entry in CaosDef
        val reference = containingCommand
                .commandToken
                ?.reference
                ?.multiResolve(true)
                ?.firstOrNull()
                ?.element
                ?.getSelfOrParentOfType(CaosDefCommandDefElement::class.java)
                ?: return

        // Get arguments parameter definition
        val parameterStruct = reference
                .docComment
                ?.parameterStructs
                ?.getOrNull(index)
                ?: return
        // Get the expected type of this argument
        val type =
                if (containingCommand is CaosScriptCAssignment) {
                    containingCommand.lvalue
                            ?.commandToken
                            ?.reference
                            ?.multiResolve(true)
                            ?.firstOrNull()
                            ?.element
                            ?.getSelfOrParentOfType(CaosDefCommandDefElement::class.java)
                            ?.returnTypeStruct
                            ?.type
                } else {
                    parameterStruct.type
                }
                        ?: return

        val variant = valueOfType.containingCaosFile?.variant
                ?: return
        val addSpace = reference.parameterStructs.size - 1 > index
        // If type definition contains values list annotation, add list values
        if (type.valuesList != null) {
            addListValues(resultSet, variant, type.valuesList, valueOfType.project, valueOfType.text, addSpace)
        } else if (!type.fileTypes.isNullOrEmpty()) {
            // RValue requires a file type, so fill in filenames with matching types
            val project = valueOfType.project
            // Get all files of for filetype list
            // List is necessary for CV-DS, as they can take C16 or S16
            val allFiles = type.fileTypes
                    .flatMap { fileExtensionTemp ->
                        val fileExtension = fileExtensionTemp.toLowerCase()
                        val searchScope =
                                valueOfType.containingFile?.module?.let { GlobalSearchScope.moduleScope(it) }
                                        ?: GlobalSearchScope.projectScope(project)
                        FilenameIndex.getAllFilesByExt(project, fileExtension, searchScope).toList()
                    }
                    .map { it.nameWithoutExtension }
            // Loop through all files and format them as needed.
            for (file in allFiles) {
                val fileName = file
                val isToken = parameterStruct.type.type.toLowerCase() == "token"
                val text = when {
                    isToken -> fileName
                    variant.isOld -> "[$fileName"
                    else -> "\"$fileName"
                }
                val openQuote = when {
                    isToken -> null
                    variant.isOld -> "["
                    else -> "\""
                }
                val closeQuote = when {
                    isToken -> null
                    variant.isOld -> "]"
                    else -> "\""
                }
                // Create lookup element
                var lookupElement = LookupElementBuilder
                        .create(text)
                        .withStrikeoutness(isToken && fileName.length != 4)
                        .withLookupString("$openQuote$fileName$closeQuote")
                        .withPresentableText(fileName)
                if (closeQuote != null) {
                    lookupElement = lookupElement
                            .withInsertHandler(CloseQuoteInsertHandler(closeQuote))
                }
                resultSet.addElement(lookupElement)
            }
        }
    }

    fun addEqualityExpressionCompletions(resultSet: CompletionResultSet, equalityExpression: com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEqualityExpression, expression: com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptExpression) {
        val other = equalityExpression.expressionList.let {
            when (it.size) {
                0, 1 -> return
                2 -> if (it[0].isEquivalentTo(expression)) it[1] else it[0]
                else -> {
                    LOGGER.severe("Equality operator expects exactly TWO expressions")
                    return
                }
            }
        } ?: return
        val token = other.rvaluePrime?.getChildOfType(CaosScriptIsCommandToken::class.java)
                ?: return
        val reference = token
                .reference
                .multiResolve(true)
                .firstOrNull()
                ?.element
                ?.getSelfOrParentOfType(CaosDefCommandDefElement::class.java)
                ?: return
        val typeDef = reference
                .docComment
                ?.returnTypeStruct
                ?.type
                ?.valuesList
                ?: return
        val variant = expression.containingCaosFile?.variant
                ?: return
        addListValues(resultSet, variant, typeDef, expression.project, expression.text, expression.getPreviousNonEmptyNode(false) !is CaosScriptEqOp)
    }

    private fun addListValues(resultSet: CompletionResultSet, variant: CaosVariant, listName: String, project: Project, string: String, addSpace: Boolean) {
        val def = CaosDefValuesListDefinitionElementsByNameIndex
                .Instance[listName, project]
                .firstOrNull { it.containingCaosDefFile.isVariant(variant, true) }
                ?: return
        val values = def.keys
                .filter { it.equality == ValuesListEq.EQUAL }
                .ifEmpty { null }
                ?.sortedBy { it.key.toIntSafe() ?: 1000 }
                ?: return
        if (def.typeNoteString != null) {
            val lookupElement = PrioritizedLookupElement.withPriority(LookupElementBuilder
                    .create("")
                    .withLookupString("$listName bit-flag builder")
                    .withPresentableText("$listName bit-flag builder")
                    .withInsertHandler(GenerateBitFlagIntegerAction(listName, values)), 1000.0)
            resultSet.addElement(lookupElement)
        }
        for (value in values) {
            var lookupElement = LookupElementBuilder
                    .create(value.key)
                    .withLookupString(value.value.matchCase(string))
                    .withPresentableText(value.key + ": " + value.value)
                    .withTailText(value.description)
            if (addSpace) {
                lookupElement = lookupElement
                        .withInsertHandler(AddSpaceInsertHandler(true))
            }
            resultSet.addElement(PrioritizedLookupElement.withPriority(lookupElement, 900.0))
        }
    }

}