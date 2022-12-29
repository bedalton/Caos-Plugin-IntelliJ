package com.badahori.creatures.plugins.intellij.agenteering.catalogue.support

import com.badahori.creatures.plugins.intellij.agenteering.caos.completion.SpaceAfterInsertHandler
import com.badahori.creatures.plugins.intellij.agenteering.caos.completion.textWithoutCompletionIdString
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.lang.CatalogueFile
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.lexer.CatalogueTypes.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.util.elementType
import com.intellij.util.ProcessingContext


class CatalogueCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), CatalogueCompletionProvider)
    }
}

object CatalogueCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        resultSet: CompletionResultSet,
    ) {
        val element = parameters.position

        if (element.containingFile !is CatalogueFile) {
            return
        }

        val text = element.textWithoutCompletionIdString
        if (text.startsWith('"') || text.startsWith('\'')) {
            return
        }
        val case = text.case
        val thisObject = if (element.elementType == TokenType.WHITE_SPACE) {
            element.getPreviousNonEmptySibling(false)
        } else {
            element
        }

        var previous: PsiElement? = thisObject?.previous

        while (previous != null && previous.text.let { it.isNullOrBlank() && !it.contains("\n") }) {
            previous = previous.previous
        }

        if (previous.elementType == CATALOGUE_ARRAY_KW) {
            addKeywordCompletions(resultSet, case, listOf("OVERRIDE"))
            return
        }
        if (
            previous == null ||
            element.elementType == CATALOGUE_WORD ||
            previous.elementType == CATALOGUE_NEWLINE ||
            previous.elementType == CATALOGUE_NEWLINE_LITERAL ||
            previous.elementType == CATALOGUE_ARRAY ||
            previous.elementType == CATALOGUE_TAG ||
            previous.elementType == CATALOGUE_ITEM
        ) {
            addKeywordCompletions(resultSet, case, listOf("TAG", "ARRAY"))
        }
    }

    private fun addKeywordCompletions(resultSet: CompletionResultSet, case: Case, keywords: List<String>) {
        for (keyword in keywords) {
            resultSet.addElement(
                LookupElementBuilder.create(keyword)
                    .withLookupStrings(
                        listOf(keyword, keyword.upperCaseFirstLetter(), keyword.lowercase())
                    )
                    .withInsertHandler(SpaceAfterInsertHandler)
            )
        }
    }
}