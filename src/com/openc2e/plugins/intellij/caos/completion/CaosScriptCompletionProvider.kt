package com.openc2e.plugins.intellij.caos.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFile
import com.openc2e.plugins.intellij.caos.lang.CaosScriptIcons
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCommandToken
import com.openc2e.plugins.intellij.caos.psi.util.LOGGER
import com.openc2e.plugins.intellij.caos.psi.util.getPreviousNonEmptySibling

object CaosScriptCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, resultSet: CompletionResultSet) {
        LOGGER.info("addCompletions")
        val element = parameters.position
        val caosFile = element.containingFile.originalFile as? CaosScriptFile
                ?: return
        LOGGER.info("Completion is caos script element")
        val variant = caosFile.variant.toUpperCase()
        LOGGER.info("Variant is <$variant>")
        val previous = element.getPreviousNonEmptySibling(false)
        if (previous is CaosScriptCommandToken) {
            val previousText = previous.text
            val subcommands = previous.reference.multiResolve(true)
                    .mapNotNull item@{
                        val commandElement = (it.element as? CaosDefCommandDefElement)
                                ?: return@item null
                        val commandWords = commandElement.commandWords
                        if (commandWords.size == 1)
                            return@item null
                        val index = commandWords.indexOf(previousText)
                        if (index < 0)
                            return@item null

                        val word = commandWords.getOrNull(index + 1)
                                ?: return@item null
                        createCommandTokenLookupElement(word, commandElement.isCommand, commandElement.returnTypeString)
                    }
            resultSet.addAllElements(subcommands)
        }
        val singleCommands = CaosDefCommandElementsByNameIndex.Instance.getAll(element.project).filter {
            (variant.isBlank() || it.isVariant(variant))
        }.map {
            createCommandTokenLookupElement(it.commandName, it.isCommand, it.returnTypeString)
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

    private fun createCommandTokenLookupElement(command: String, isCommand: Boolean, returnType: String): LookupElementBuilder {
        val tailText = if (isCommand)
            "command"
        else
            returnType
        val icon = if (isCommand)
            CaosScriptIcons.COMMAND
        else
            CaosScriptIcons.VARIABLE
        return LookupElementBuilder
                .create(command)
                .withTailText("($tailText)")
                .withIcon(icon)
                .withInsertHandler(SpaceAfterInsertHandler)
    }

}

private enum class Case {
    UPPER_CASE,
    LOWER_CASE
}
