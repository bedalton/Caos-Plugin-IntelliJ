package com.openc2e.plugins.intellij.caos.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.openc2e.plugins.intellij.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.caos.psi.api.*

class CaosScriptCommandAnnotator : Annotator {


    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        when (element) {
            is CaosScriptCommandToken -> annotateCommand(element, annotationHolder)
            is CaosScriptEnumSceneryStatement -> annotateSceneryEnum(element, annotationHolder)
        }
    }

    fun annotateCommand(word:CaosScriptCommandToken, annotationHolder: AnnotationHolder) {
        if (annotateInvalidCommandLength(word, annotationHolder))
            return
        if (annotateInvalidCommand(word, annotationHolder))
            return
        if (annotateCommandVsValue(word, annotationHolder))
            return
    }

    private fun annotateInvalidCommandLength(word:CaosScriptCommandToken, annotationHolder: AnnotationHolder) : Boolean {
        if (word.textLength == 4)
            return false
        annotationHolder.createErrorAnnotation(word, CaosBundle.message("caos.annnotator.command-annotator.invalid-token-length"))
        return true
    }

    private fun annotateInvalidCommand(word:CaosScriptCommandToken, annotationHolder: AnnotationHolder) : Boolean {
        val command = (word.parent as? CaosScriptCommand)
                ?: return true
        val commandString = command.commandString
        val exists = CaosDefCommandElementsByNameIndex.Instance[commandString, word.project]
        if (exists.isEmpty()) {
            annotationHolder.createWarningAnnotation(word, CaosBundle.message("caos.annnotator.command-annotator.invalid-command", commandString))
            return true
        }
        val forVariant = exists.filter {
            word.isVariant(it.variants, false)
        }
        if (forVariant.isNotEmpty())
            return false
        val availableOn = exists.flatMap { it.variants }.toSet()
        annotationHolder.createWarningAnnotation(word, CaosBundle.message("caos.annnotator.command-annotator.invalid-variant", commandString, availableOn.joinToString(",")))
        return true
    }

    private fun annotateCommandVsValue(word: CaosScriptCommandToken, annotationHolder: AnnotationHolder): Boolean {
        return false;
    }

    private fun annotateSceneryEnum(element:CaosScriptEnumSceneryStatement, annotationHolder: AnnotationHolder) {
        if (element.containingCaosFile.variant == "C2")
            return
        annotationHolder.createErrorAnnotation(element.escn, CaosBundle.message("caos.annotator.command-annotator.escn-only-on-c2-error-message"))
    }
}