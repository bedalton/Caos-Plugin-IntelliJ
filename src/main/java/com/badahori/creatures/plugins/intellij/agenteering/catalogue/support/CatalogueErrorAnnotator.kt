package com.badahori.creatures.plugins.intellij.agenteering.catalogue.support

import com.badahori.creatures.plugins.intellij.agenteering.caos.annotators.newErrorAnnotation
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.DeleteElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.lexer.CatalogueTypes
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.levenshteinDistance
import com.badahori.creatures.plugins.intellij.agenteering.utils.tokenType
import com.bedalton.common.util.stripSurroundingQuotes
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement

class CatalogueErrorAnnotator : Annotator, DumbAware {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is CatalogueErrorItem -> annotateErrorElement(element, holder)
            is CatalogueOverride -> annotateOverride(element, holder)
            is CatalogueCount -> annotateNotAllowedInTag(element, holder)
            else -> {
                when (element.tokenType) {
                    CatalogueTypes.CATALOGUE_TAG_KW -> annotateNotUpperCase(element, holder)
                    CatalogueTypes.CATALOGUE_ARRAY_KW -> annotateNotUpperCase(element, holder)
                    CatalogueTypes.CATALOGUE_WORD -> annotateBadKeyword(element, holder, annotateErrorItem = false)
                }
            }
        }
    }

    private fun annotateOverride(element: CatalogueOverride, holder: AnnotationHolder) {
        annotateNotUpperCase(element, holder)
        annotateNotAllowedInTag(element, holder)
    }

    private fun annotateNotUpperCase(element: PsiElement, holder: AnnotationHolder) {
        val text = element.text
        if (text == text.uppercase()) {
            return
        }
        holder.newErrorAnnotation(CaosBundle.message("catalogue.errors.not-upper-case", text.uppercase()))
            .range(element)
            .withFix(
                CaosScriptReplaceElementFix(
                    element,
                    text.uppercase(),
                    CaosBundle.message("catalogue.errors.not-upper-case.fix", text),
                    false
                )
            )
            .create()
    }

    private fun annotateNotAllowedInTag(element: PsiElement, holder: AnnotationHolder) {
        if (element.parent is CatalogueArray) {
            return
        }
        val error = when (element) {
            is CatalogueOverride -> CaosBundle.message("catalogue.errors.override-not-allowed-in-tag")
            is CatalogueCount -> CaosBundle.message("catalogue.errors.count-not-allowed-in-tag")
            else -> return
        }
        holder.newErrorAnnotation(error)
            .range(element)
            .withFix(DeleteElementFix(
                CaosBundle.message("catalogue.errors.remove-invalid-element"),
                element
            ))
            .create()
    }


    private fun annotateErrorElement(element: CatalogueErrorItem, holder: AnnotationHolder) {
        val text = element.text
        if (text.startsWith('"') || text.startsWith('\'')) {
            annotateErrorString(element, holder)
            return
        }

        if (text.toFloatOrNull() != null) {
            annotateErrorInt(element, holder)
            return
        }

        if (annotateBadKeyword(element, holder, true)) {
            return
        }

        val parent = if (element.parent?.parent is CatalogueTag) {
            "TAG"
        } else {
            "ARRAY"
        }
        val value = if (element.parent is CatalogueItemName) {
            CaosBundle.message("catalogue.component-names.item-name", parent)
        } else {
            CaosBundle.message("catalogue.component-names.item", parent)
        }
        val error = CaosBundle.message("catalogue.errors.must-be-quoted", value)
        val fixMessage = CaosBundle.message("catalogue.errors.must-be-quoted.fix")
        val replacementText = "\"$text\""
        holder.newErrorAnnotation(error)
            .range(element)
            .withFix(
                CaosScriptReplaceElementFix(
                    element,
                    replacementText,
                    fixMessage,
                    false
                )
            )
            .create()

    }

    private fun annotateErrorString(element: PsiElement, holder: AnnotationHolder) {
        val text = element.text
        var unquotedText = text.stripSurroundingQuotes(1)
        if (unquotedText.startsWith('"')) {
            unquotedText = unquotedText.substring(1)
        }
        val errorText: String
        val fixText: String

        if (text.startsWith('\'')) {
            errorText = CaosBundle.message("catalogue.errors.incorrect-quote")
            fixText = CaosBundle.message("catalogue.errors.incorrect-quote.fix")
        } else {
            errorText = CaosBundle.message("catalogue.errors.unclosed-string")
            fixText = CaosBundle.message("catalogue.errors.unclosed-string.fix")
        }
        holder.newErrorAnnotation(errorText)
            .range(element)
            .afterEndOfLine()
            .withFix(
                CaosScriptReplaceElementFix(
                    element,
                    "\"$unquotedText\"",
                    fixText,
                    false
                )
            )
            .create()
    }

    private fun annotateErrorInt(element: PsiElement, holder: AnnotationHolder) {
        val text = element.text
        val errorText: String = CaosBundle.message("catalogue.errors.unquoted-number")
        val fixMessage: String = CaosBundle.message("catalogue.errors.unquoted-number.fix")
        holder.newErrorAnnotation(errorText)
            .range(element)
            .afterEndOfLine()
            .withFix(
                CaosScriptReplaceElementFix(
                    element,
                    "\"$text\"",
                    fixMessage,
                    false
                )
            )
            .create()
    }

    private fun annotateBadKeyword(element: PsiElement, holder: AnnotationHolder, annotateErrorItem: Boolean): Boolean {
        if (element.parent is CatalogueErrorItem && !annotateErrorItem) {
            return false
        }
        val text = element.text
        val errorText: String = CaosBundle.message("catalogue.errors.unexpected-word", text)
        val possibleIntention = getSimilar(text)
        var annotation = holder.newErrorAnnotation(errorText)
            .range(element)
        if (possibleIntention != null) {
            val fixMessage = CaosBundle.message("fix.replace-with", possibleIntention)
            annotation = annotation.withFix(CaosScriptReplaceElementFix(
                element,
                possibleIntention,
                fixMessage,
                false
            ))
        }
        annotation.create()
        return true
    }

    private fun getSimilar(text: String): String? {
        val textUpperCase = text.uppercase()
        return keywords.associateWith { textUpperCase.levenshteinDistance(it) }
            .entries
            .minByOrNull {
                "Distance[${it.key}]: " + it.value
                it.value
            }
            ?.let {
                "Match = ${it.key}; Distance: " + it.value
                if (it.value < 4) {
                    it.key
                } else {
                    null
                }
            }
    }
}

val keywords = listOf("ARRAY", "OVERRIDE", "TAG")