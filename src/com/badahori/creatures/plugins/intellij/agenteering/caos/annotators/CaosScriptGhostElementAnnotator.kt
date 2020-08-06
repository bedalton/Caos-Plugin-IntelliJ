package com.badahori.creatures.plugins.intellij.agenteering.caos.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.endOffset
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.AppendStatementTerminator
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptInsertBeforeFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.next
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.matchCase
import com.intellij.openapi.progress.ProgressIndicatorProvider

/**
 * Marks 'Ghost' elements which are required elements in CAOS, that are not required in BNF grammar
 * Grammar allows incomplete control statements to keep from breaking AST tree
 * In grammar, ENUM..NEXT can be parsed without NEXT and DOIF..ENDI without ENDI
 */
class CaosScriptGhostElementAnnotator : Annotator {
    /**
     * Look through all elements in file and annotate as necessary
     */
    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        ProgressIndicatorProvider.checkCanceled()
        val annotationWrapper = AnnotationHolderWrapper(annotationHolder)
        if (element !is CaosScriptCompositeElement)
            return
        // Get the expected terminator
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
            // Ghost element here is SETV which is optional in grammar for C1 and C2, but not optional in CAOS
            is CaosScriptCAssignment -> {
                if (element.firstChild is CaosScriptLvalue) {
                    annotationWrapper.newErrorAnnotation("${element.text.substring(0,4).toUpperCase()} is an LValue and must be used with an lvalue operator such as SETV ")
                            .range(element.firstChild)
                            .withFix(CaosScriptInsertBeforeFix("Prepend SETV to statement", "SETV".matchCase(element.lastChild.text), element))
                            .create()
                }
                null
            }
            // Element does not have possible ghost element
            else -> null
        } ?: return
        // Annotate the statement
        annotate(element, terminator, annotationWrapper)
    }

    /**
     * Actually annotate element when there is only one ghost element option
     */
    private fun <PsiT:PsiElement> annotate(element:PsiT, expectedToken:String, annotationWrapper: AnnotationHolderWrapper) {
        // If block ends with expected command, not need to do more
        if (element.lastChild.text.trim().toUpperCase().endsWith(expectedToken))
            return
        // Get next element after control block
        var range:PsiElement? = element.next
        // Ensure that range is not whitespace
        while (range != null && range.text.isBlank()) {
            range = range.next
        }
        // There is no non-whitespace next element, so use last child of control statement
        if (range == null)
            range = element.lastChild

        // Add annotation
        annotationWrapper.newErrorAnnotation("Unterminated ${element.text.substring(0,4).toUpperCase()} statement. Expected ${expectedToken.toUpperCase()}")
                .range(range!!)
                .withFix(AppendStatementTerminator(element, expectedToken))
                .create()
    }

    /**
     * Annotate multiple options for ghost element.
     * Only used for LOOP.. UNTL/EVER
     */
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