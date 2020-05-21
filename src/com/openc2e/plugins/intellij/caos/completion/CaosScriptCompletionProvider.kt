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
import icons.CaosScriptIcons
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCommandCall
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptLvalue
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptRvalue
import com.openc2e.plugins.intellij.caos.psi.util.CaosCommandType
import com.openc2e.plugins.intellij.caos.psi.util.getEnclosingCommandType
import com.openc2e.plugins.intellij.caos.psi.util.getPreviousNonEmptySibling
import com.openc2e.plugins.intellij.caos.utils.isNotNullOrBlank

object CaosScriptCompletionProvider : CompletionProvider<CompletionParameters>() {

    private val UPPERCASE_REGEX = "[A-Z]".toRegex()
    private val IS_ID_CHAR = "[^_A-Za-z]".toRegex()
    private val IS_NUMBER = "[0-9]+".toRegex()
    private val WHITESPACE = "\\s+".toRegex()

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
        val text = element.textWithoutCompletionIdString
        if (IS_NUMBER.matches(text)) {
            resultSet.stopHere()
            return
        }
        if (!WHITESPACE.matches(element.prevSibling?.text?:"")) {
            resultSet.stopHere()
            return
        }
        val previous = element.getPreviousNonEmptySibling(false)
        if (previous != null && IS_NUMBER.matches(previous.text)) {
            resultSet.stopHere()
            return
        }
        var parent:PsiElement? = element.parent
        while(parent != null && parent !is CaosScriptRvalue && parent !is CaosScriptLvalue && parent !is CaosScriptCommandCall) {
            parent = parent.parent
        }
        if (parent == null) {
            resultSet.stopHere()
            return
        }
        val type = parent.getEnclosingCommandType()

        val singleCommands = CaosDefCommandElementsByNameIndex.Instance.getAll(element.project).filter {
            if (!(variant.isBlank() || it.isVariant(variant)))
                return@filter false
            when(type) {
                CaosCommandType.COMMAND -> it.isCommand
                CaosCommandType.RVALUE -> it.isRvalue
                CaosCommandType.LVALUE -> it.isLvalue
                CaosCommandType.UNDEFINED -> false
            }
        }.map {
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

    private fun addVariableCompletions(variant:String, element:PsiElement, resultSet: CompletionResultSet) {
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

    private fun createCommandTokenLookupElement(element:PsiElement, case: Case, commandIn: String, isCommand: Boolean, returnType: String, prefixIn: String? = null): LookupElementBuilder {
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

    fun needsSpaceAfter(element:PsiElement, commandString:String) : Boolean {
        var parent:PsiElement? = element.parent
        while(parent != null && parent !is CaosScriptRvalue && parent !is CaosScriptLvalue && parent !is CaosScriptCommandCall) {
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

private enum class Case {
    UPPER_CASE,
    LOWER_CASE
}
