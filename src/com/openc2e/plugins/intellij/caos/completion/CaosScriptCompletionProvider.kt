package com.openc2e.plugins.intellij.caos.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFile
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCommandCall
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptIncomplete
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptLvalue
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptRvalue
import com.openc2e.plugins.intellij.caos.psi.util.*
import com.openc2e.plugins.intellij.caos.utils.Case
import com.openc2e.plugins.intellij.caos.utils.isNotNullOrBlank
import icons.CaosScriptIcons

object CaosScriptCompletionProvider : CompletionProvider<CompletionParameters>() {
    private val IS_NUMBER = "[0-9]+".toRegex()
    private val WHITESPACE = "\\s+".toRegex()
    private val SKIP_VAR_NAMES = listOf("VARX", "OBVX", "MVXX", "OVXX", "VAXX")
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, resultSet: CompletionResultSet) {
        val element = parameters.position
        val caosFile = element.containingFile.originalFile as? CaosScriptFile
                ?: return
        val variant = caosFile.variant.toUpperCase()

        val text = element.textWithoutCompletionIdString
        if (IS_NUMBER.matches(text)) {
            resultSet.stopHere()
            return
        }

        val previous = element.previous
        if (!WHITESPACE.matches(previous?.text ?: "")) {
            resultSet.stopHere()
            return
        }
        if (previous != null && IS_NUMBER.matches(previous.text)) {
            resultSet.stopHere()
            return
        }
        var parent: PsiElement? = element
        while (parent != null && parent !is CaosScriptRvalue && parent !is CaosScriptLvalue && parent !is CaosScriptCommandCall && parent !is CaosScriptIncomplete) {
            parent = parent.parent
        }
        if (parent == null) {
            LOGGER.info("Parent of ${element.text} is not of a command type")
            resultSet.stopHere()
            return
        }
        val type = parent.getEnclosingCommandType()
        LOGGER.info("Element: ${element.text} of type ${element.elementType} is of caos value type ${type.value.toLowerCase()}")
        val case = element.case
        val singleCommands = CaosDefCommandElementsByNameIndex.Instance.getAll(element.project)
                .filter {
                    if (it.commandName.toUpperCase() !in SKIP_VAR_NAMES)
                        return@filter false
                    if (!(variant.isBlank() || it.isVariant(variant)))
                        return@filter false
                    when (type) {
                        CaosCommandType.COMMAND -> it.isCommand
                        CaosCommandType.RVALUE -> it.isRvalue
                        CaosCommandType.LVALUE -> it.isLvalue
                        CaosCommandType.UNDEFINED -> false
                    }
                }
                .map {
                    if (it.commandWords.size > 1) {
                        val commandWords = it.commandWords
                        if (previous?.text?.toLowerCase() == it.commandWords[0].toLowerCase()) {
                            return@map createCommandTokenLookupElement(element, case, it.commandWords[1], it.isCommand, it.returnTypeString, commandWords[0])
                        }
                    }
                    createCommandTokenLookupElement(element, case, it.commandName, it.isCommand, it.returnTypeString)
                }
        resultSet.addAllElements(singleCommands)
        if (type == CaosCommandType.RVALUE || type == CaosCommandType.LVALUE)
            addVariableCompletions(variant, element, resultSet)
    }

    private fun addVariableCompletions(variant: String, element: PsiElement, resultSet: CompletionResultSet) {
        when (variant) {
            "C1" -> {
                (0..2).forEach {
                    resultSet.addElement(LookupElementBuilder.create("obv$it"))
                }
                (0..9).forEach {
                    resultSet.addElement(LookupElementBuilder.create("var$it"))
                }
            }
            "C2" -> {
                (0..9).forEach {
                    resultSet.addElement(LookupElementBuilder.create("obv$it"))
                }
                (0..9).forEach {
                    resultSet.addElement(LookupElementBuilder.create("var$it"))
                }
            }
        }
        if (variant != "C1" && element.text.substringBefore("zz").matches("[vom].*?".toRegex())) {
            val items = (0..99).map {
                "$it".padStart(2, '0')
            }
            items.map {
                resultSet.addElement(LookupElementBuilder.create("va$it"))
                resultSet.addElement(LookupElementBuilder.create("ov$it"))
            }
            if (variant != "C2") {
                items.map {
                    resultSet.addElement(LookupElementBuilder.create("mv$it"))
                }
            }
        }
    }

    private fun createCommandTokenLookupElement(element: PsiElement, case: Case, commandIn: String, isCommand: Boolean, returnType: String, prefixIn: String? = null): LookupElementBuilder {
        val tailText = if (isCommand)
            "command"
        else
            returnType
        val icon = if (isCommand)
            CaosScriptIcons.COMMAND
        else
            CaosScriptIcons.RVALUE
        val command = if (case == Case.UPPER_CASE)
            commandIn.toUpperCase()
        else
            commandIn.toLowerCase()
        val prefix = when {
            prefixIn == null -> null
            case == Case.UPPER_CASE -> prefixIn.toUpperCase()
            else -> prefixIn.toLowerCase()
        }
        var builder = LookupElementBuilder
                .create(command)
                .withTailText("($tailText)")
                .withIcon(icon)
        if (needsSpaceAfter(element, command))
            builder = builder.withInsertHandler(SpaceAfterInsertHandler)
        if (prefix.isNotNullOrBlank()) {
            builder = builder.withPresentableText("$prefix $command")
        }
        return builder
    }

    fun needsSpaceAfter(element: PsiElement, commandString: String): Boolean {
        var parent: PsiElement? = element.parent
        while (parent != null && parent !is CaosScriptRvalue && parent !is CaosScriptLvalue && parent !is CaosScriptCommandCall) {
            parent = parent.parent
        }
        if (parent == null)
            return false
        val variant = (element.containingFile as? CaosScriptFile)?.variant
        var matches = CaosDefCommandElementsByNameIndex.Instance[commandString, element.project]
                .filter { (variant == null || variant in it.variants) }
        matches = when (parent) {
            is CaosScriptCommandCall -> matches.filter { it.isCommand }
            is CaosScriptRvalue -> matches.filter { it.isRvalue }
            is CaosScriptLvalue -> matches.filter { it.isLvalue }
            else -> return false
        }
        if (matches.isEmpty())
            return false
        return matches.all { it.parameterStructs.size > 0 }
    }

}

private val PsiElement.textWithoutCompletionIdString get() = text.substringBefore(CompletionUtilCore.DUMMY_IDENTIFIER)
