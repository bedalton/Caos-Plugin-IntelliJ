package com.badahori.creatures.plugins.intellij.agenteering.caos.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.endOffset
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.AppendStatementTerminator
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptInsertBeforeFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.next
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.matchCase
import com.intellij.openapi.progress.ProgressIndicatorProvider

class CaosScriptGhostElementAnnotator : Annotator {
    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        ProgressIndicatorProvider.checkCanceled()
        val annotationWrapper = AnnotationHolderWrapper(annotationHolder)
        if (element !is CaosScriptCompositeElement)
            return
        val terminator = when (element) {
            is CaosScriptDoifStatement -> "ENDI"
            is CaosScriptSubroutine -> "RETN"
            is CaosScriptEnumNextStatement -> "NEXT"
            is CaosScriptRepeatStatement -> "REPE"
            is CaosScriptLoopStatement -> {
                annotate(element, listOf("UNTL","EVER"), annotationWrapper)
                null
            }
            is CaosScriptEnumSceneryStatement -> "NSCN"
            is CaosScriptCAssignment -> {
                if (element.firstChild is CaosScriptLvalue) {
                    annotationWrapper.newErrorAnnotation("${element.text.substring(0,4).toUpperCase()} is an LValue and must be used with an lvalue operator such as SETV ")
                            .range(element.firstChild)
                            .withFix(CaosScriptInsertBeforeFix("Prepend SETV to statement", "SETV".matchCase(element.lastChild.text), element))
                            .create()
                }
                null
            }
            else -> null
        } ?: return
        annotate(element, terminator, annotationWrapper)
    }

    private fun <PsiT:PsiElement> annotate(element:PsiT, expectedToken:String, annotationWrapper: AnnotationHolderWrapper) {
        if (element.lastChild.text.toUpperCase().endsWith(expectedToken))
            return
        var range:PsiElement? = element.next
        while (range != null && range.text.isBlank()) {
            range = range.next
        }
        if (range == null)
            range = element.lastChild
        annotationWrapper.newErrorAnnotation("Unterminated ${element.text.substring(0,4).toUpperCase()} statement. Expected ${expectedToken.toUpperCase()}")
                .range(range!!)
                .withFix(AppendStatementTerminator(element, expectedToken))
                .create()
    }

    private fun <PsiT:PsiElement> annotate(element:PsiT, expectedTokens:List<String>, annotationWrapper: AnnotationHolderWrapper) {
        for(expectedToken in expectedTokens) {
            ProgressIndicatorProvider.checkCanceled()
            if (element.lastChild.text.toUpperCase().contains(expectedToken))
                return
        }
        for(expectedToken in expectedTokens) {
            ProgressIndicatorProvider.checkCanceled()
            annotationWrapper.newErrorAnnotation("Unterminated ${element.text.substring(0,4).toUpperCase()} statement. Expected ${expectedToken.toUpperCase()}")
                    .range(TextRange(element.lastChild.endOffset - 1, element.lastChild.endOffset))
                    .withFix(AppendStatementTerminator(element, expectedToken))
                    .create()
        }
    }
}