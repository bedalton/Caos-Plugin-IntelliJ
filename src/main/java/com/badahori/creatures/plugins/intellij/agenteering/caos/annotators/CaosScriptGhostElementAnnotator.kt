package com.badahori.creatures.plugins.intellij.agenteering.caos.annotators

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.AppendStatementTerminator
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptInsertBeforeFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.endOffset
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.next
import com.badahori.creatures.plugins.intellij.agenteering.utils.hasParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.matchCase
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

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
        if (element !is CaosScriptCompositeElement || element.hasParentOfType(CaosDefCompositeElement::class.java))
            return
        // Get the expected terminator
        when (element) {
            is CaosScriptDoifStatement -> annotate(element, "ENDI", annotationWrapper) {
                it.cEndi != null
            }
            is CaosScriptSubroutine -> annotate(element, "RETN", annotationWrapper) {
                it.retnKw != null
            }
            is CaosScriptEnumNextStatement -> annotate(element, "NEXT", annotationWrapper) {
                it.cNext != null || it.cNscn != null
            }
            is CaosScriptRepeatStatement -> annotate(element, "REPE", annotationWrapper) {
                it.cRepe != null
            }
            is CaosScriptLoopStatement -> {
                annotate(element, listOf("UNTL", "EVER"), annotationWrapper) { it ->
                    it.loopTerminator?.let { it.cEver != null || it.cUntl != null}.orFalse()
                }
            }
            is CaosScriptEnumSceneryStatement -> annotate(element, "NSCN", annotationWrapper) {
                it.cNext != null || it.cNscn != null
            }
            // Ghost element here is SETV which is optional in grammar for C1 and C2, but not optional in CAOS
            is CaosScriptCAssignment -> {
                if (element.firstChild is CaosScriptLvalue) {
                    annotationWrapper.newErrorAnnotation("${element.text.substring(0,4).toUpperCase()} is an LValue and must be used with an lvalue operator such as SETV ")
                            .range(element.firstChild)
                            .withFix(CaosScriptInsertBeforeFix("Prepend SETV to statement", "SETV".matchCase(element.lastChild.text), element))
                            .create()
                }
            }
        }
    }

    /**
     * Actually annotate element when there is only one ghost element option
     */
    private fun <PsiT:PsiElement> annotate(element:PsiT, expectedToken:String, annotationWrapper: AnnotationHolderWrapper, check:(element:PsiT)->Boolean) {
        // If block ends with expected command, not need to do more
        if (check(element))
            return
        // Get next element after control block
        var next:PsiElement? = element.next
        // Ensure that range is not whitespace
        while (next != null && next.text.isBlank()) {
            next = next.next
        }
        var range = next?.textRange
        // There is no non-whitespace next element, so use last child of control statement
        if (range == null)
            range = element.lastChild.textRange.let {
                var startOffset = it.endOffset - 4
                if (startOffset < 0)
                    startOffset = it.startOffset
                TextRange(startOffset, it.endOffset)
            }

        // Add annotation
        annotationWrapper.newErrorAnnotation("Unterminated ${element.text.substring(0,4).toUpperCase()} statement. Expected ${expectedToken.toUpperCase()}")
                .range(range)
                .withFix(AppendStatementTerminator(element, expectedToken))
                .create()
    }

    /**
     * Annotate multiple options for ghost element.
     * Only used for LOOP.. UNTL/EVER
     */
    private fun <PsiT:PsiElement> annotate(element:PsiT, expectedTokens:List<String>, annotationWrapper: AnnotationHolderWrapper, check:(element:PsiT)->Boolean) {
        if (check(element)) {
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