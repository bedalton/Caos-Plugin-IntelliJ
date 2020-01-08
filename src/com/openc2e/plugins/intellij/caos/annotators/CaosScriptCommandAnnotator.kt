package com.openc2e.plugins.intellij.caos.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandWord
import com.openc2e.plugins.intellij.caos.def.stubs.api.variants
import com.openc2e.plugins.intellij.caos.highlighting.CaosScriptSyntaxHighlighter
import com.openc2e.plugins.intellij.caos.highlighting.colorize
import com.openc2e.plugins.intellij.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.caos.lexer.CaosScriptTypes
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCommandToken
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptEnumSceneryStatement
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptExpression
import com.openc2e.plugins.intellij.caos.psi.util.LOGGER
import com.openc2e.plugins.intellij.caos.psi.util.getPreviousNonEmptySibling

class CaosScriptCommandAnnotator : Annotator {


    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        if(DumbService.isDumb(element.project))
            return
        when (element) {
            is CaosScriptCommandToken -> annotateCommand(element, annotationHolder)
        }
    }

    private fun annotateCommand(word:CaosScriptCommandToken, annotationHolder: AnnotationHolder) {
        if (isFileToken(word)) {
            annotationHolder.colorize(word, CaosScriptSyntaxHighlighter.STRING)
            return
        }
        if (annotateInvalidCommandLength(word, annotationHolder))
            return
        if (annotateInvalidCommand(word, annotationHolder))
            return
        if (annotateCommandVsValue(word, annotationHolder))
            return
    }

    private fun isFileToken(word: CaosScriptCommandToken): Boolean {
        val previous = (word.getPreviousNonEmptySibling(false) as? CaosScriptExpression)?.commandToken
                ?: return false
        val wordString = word.text.toLowerCase()
        val previousText= previous.text.toLowerCase()
        val prevAndSelfAreSame = previousText == wordString
        return previous.reference.multiResolve(false)
                .filter{
                    it.element?.text?.toLowerCase() == previousText
                }.mapNotNull {
            (it.element as? CaosDefCommandWord)?.getParentOfType(CaosDefCommandDefElement::class.java) }
                .any {
                    val commandWords = it.command.commandWordList
                    val first = commandWords.first().text.toLowerCase()
                    if (commandWords.size > 1 && first == previousText)
                        return@any false
                    LOGGER.info("${it.command.commandWordList.first().text.toLowerCase()} == $wordString; $previousText == $wordString")
                    (prevAndSelfAreSame ||  first != wordString) &&
                    it.parameterStructs.getOrNull(0)?.type?.type?.toLowerCase() == "token"
                }
    }

    private fun annotateInvalidCommandLength(word:CaosScriptCommandToken, annotationHolder: AnnotationHolder) : Boolean {
        if (word.textLength == 4)
            return false
        annotationHolder.createErrorAnnotation(word, CaosBundle.message("caos.annotator.command-annotator.invalid-token-length"))
        return true
    }

    private fun annotateInvalidCommand(word:CaosScriptCommandToken, annotationHolder: AnnotationHolder) : Boolean {
        val exists = word.reference
                .multiResolve(false)
                .mapNotNull { it.element as? CaosDefCommandWord }
        val forVariant = exists.filter {
            word.isVariant(it.containingCaosDefFile.variants, false)
        }
        if (forVariant.isNotEmpty())
            return false
        val availableOn = exists.flatMap { it.containingCaosDefFile.variants }.toSet()
        if (exists.isNotEmpty())
            annotationHolder.createWarningAnnotation(word, CaosBundle.message("caos.annotator.command-annotator.invalid-variant", word.name, availableOn.joinToString(",")))
        else
            annotationHolder.createWarningAnnotation(word, CaosBundle.message("caos.annotator.command-annotator.invalid-command", word.name, availableOn.joinToString(",")))
        return true
    }

    private fun findForward() {

    }

    private fun annotateCommandVsValue(word: CaosScriptCommandToken, annotationHolder: AnnotationHolder): Boolean {
        return false
    }

    companion object {
        private val HAS_TOKN_COMMANDS = listOf(
                "TOKN",
                "SIMP",
                "COMP",
                "BKBD",
                "SCEN",
                "SIMP"
        )
    }
}