package com.openc2e.plugins.intellij.caos.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandWord
import com.openc2e.plugins.intellij.caos.def.stubs.api.variants
import com.openc2e.plugins.intellij.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.caos.lexer.CaosScriptTypes
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCommandToken
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptEnumSceneryStatement
import com.openc2e.plugins.intellij.caos.psi.util.getPreviousNonEmptySibling

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
        val exists = word.reference.multiResolve(false).mapNotNull { it.element as? CaosDefCommandWord }
        val forVariant = exists.filter {
            word.isVariant(it.containingCaosDefFile.variants, false)
        }
        if (forVariant.isNotEmpty())
            return false
        val availableOn = exists.flatMap { it.containingCaosDefFile.variants }.toSet()
        annotationHolder.createWarningAnnotation(word, CaosBundle.message("caos.annnotator.command-annotator.invalid-variant", word.name, availableOn.joinToString(",")))
        return true
    }

    private fun findForward() {

    }

    private fun annotateCommandVsValue(word: CaosScriptCommandToken, annotationHolder: AnnotationHolder): Boolean {
        return false;
    }

    private fun annotateSceneryEnum(element:CaosScriptEnumSceneryStatement, annotationHolder: AnnotationHolder) {
        if (element.containingCaosFile?.variant == "C2")
            return
        annotationHolder.createErrorAnnotation(element.escn, CaosBundle.message("caos.annotator.command-annotator.escn-only-on-c2-error-message"))
    }
}