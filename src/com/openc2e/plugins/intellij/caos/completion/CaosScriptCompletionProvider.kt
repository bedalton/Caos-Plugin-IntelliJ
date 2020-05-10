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
import com.openc2e.plugins.intellij.caos.lang.CaosScriptIcons
import com.openc2e.plugins.intellij.caos.psi.util.getPreviousNonEmptySibling
import com.openc2e.plugins.intellij.caos.utils.isNotNullOrBlank

object CaosScriptCompletionProvider : CompletionProvider<CompletionParameters>() {

    private val UPPERCASE_REGEX = "[A-Z]".toRegex()
    private val IS_ID_CHAR = "[^_A-Za-z]".toRegex()
    private val IS_NUMBER = "[0-9]+".toRegex()

    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, resultSet: CompletionResultSet) {
        val element = parameters.position
        val caosFile = element.containingFile.originalFile as? CaosScriptFile
                ?: return
        val variant = caosFile.variant.toUpperCase()
        val firstChar = element.textWithoutCompletionIdString.toCharArray().getOrNull(0)
        if (firstChar != null && IS_ID_CHAR.matches(firstChar + ""))
            return
        val case = if (firstChar != null && UPPERCASE_REGEX.matches(firstChar + ""))
            Case.UPPER_CASE
        else
            Case.LOWER_CASE

        val previous = element.getPreviousNonEmptySibling(false)
        if (previous != null && IS_NUMBER.matches(previous.text)) {
            resultSet.stopHere()
            return
        }

        val singleCommands = CaosDefCommandElementsByNameIndex.Instance.getAll(element.project).filter {
            (variant.isBlank() || it.isVariant(variant))
        }.map {
            if (it.commandWords.size > 1) {
                val commandWords = it.commandWords
                if (previous?.text?.toLowerCase() == it.commandWords[0].toLowerCase()) {
                    return@map createCommandTokenLookupElement(case, it.commandWords[1], it.isCommand, it.returnTypeString, commandWords[0])
                }
            }
            createCommandTokenLookupElement(case, it.commandName, it.isCommand, it.returnTypeString)
        }
        resultSet.addAllElements(singleCommands)
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

    private fun createCommandTokenLookupElement(case: Case, commandIn: String, isCommand: Boolean, returnType: String, prefixIn: String? = null): LookupElementBuilder {
        val tailText = if (isCommand)
            "command"
        else
            returnType
        val icon = if (isCommand)
            CaosScriptIcons.COMMAND
        else
            CaosScriptIcons.VARIABLE
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
                .withInsertHandler(SpaceAfterInsertHandler)
        if (prefix.isNotNullOrBlank()) {
            builder = builder.withPresentableText("$prefix $command")
        }
        return builder
    }

}

private val PsiElement.textWithoutCompletionIdString get() = text.substringBefore(CompletionUtilCore.DUMMY_IDENTIFIER)

private enum class Case {
    UPPER_CASE,
    LOWER_CASE
}
