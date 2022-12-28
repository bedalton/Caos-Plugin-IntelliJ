package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptQuoteStringLiteral
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptStringText
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.toTokenOrNull
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.token
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.indices.CatalogueEntryElementIndex
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInsight.completion.CompletionResult
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.search.ProjectScope

object CatalogueCompletionContributor {

    private val needsCatalogueTag = listOf(
        token("read"),
        token("rean"),
        token("reaq")
    )

    fun getCatalogueCompletions(
        result: CompletionResultSet,
        element: PsiElement
    ) {
        val enclosingStringElement = element.getSelfOrParentOfType(CaosScriptQuoteStringLiteral::class.java)
        val previous = (enclosingStringElement ?: element).getPreviousNonEmptySibling(false)?.toTokenOrNull()
        val elementText = element.textWithoutCompletionIdString.lowercase()
        if (previous !in needsCatalogueTag && (elementText.length != 4 || token(elementText) !in needsCatalogueTag)) {
            return
        }
        val project = element.project
        val scope = ProjectScope.getProjectScope(project)
        val tags = CatalogueEntryElementIndex.Instance.getAllKeys(project, scope)
            .distinct()

        val enclosingString = (enclosingStringElement?.text ?: element.text)
        val openQuote = if (enclosingString.isNotEmpty() && enclosingString[0] == '"') "" else "\""
        val closeQuote = if (enclosingString.length > 1 && enclosingString.last() == '"') "" else "\""

        val case = elementText.case
        for (tag in tags) {
            result.addElement(LookupElementBuilder
                .create("$openQuote$tag$closeQuote")
                .withLookupStrings(listOf(tag, tag.matchCase(case)).distinct())
                .withPresentableText(tag)
            )
        }
    }

}