package com.badahori.creatures.plugins.intellij.agenteering.caos.annotators

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.AppendStatementTerminator
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptInsertAfterFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptInsertBeforeFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

/**
 * Marks 'Ghost' elements which are required elements in CAOS, that are not required in BNF grammar.
 * Grammar allows incomplete control statements to keep from breaking AST tree
 * In grammar, ENUM...NEXT can be parsed without NEXT and DOIF...ENDI without ENDI
 */
class CaosScriptGhostElementAnnotator : Annotator {
    /**
     * Look through all elements in file and annotate as necessary
     */
    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        ProgressIndicatorProvider.checkCanceled()

        if (element.elementType == CaosScriptTypes.CaosScript_GHOST_QUOTE) {
            val parent = element.parent
            val range = try {
                parent.textRange.cutOut(element.textRange)
            }catch (e: Exception) {
                if (parent is CaosScriptQuoteStringLiteral) {
                    parent.stringText?.textRange ?: parent.textRange
                } else {
                    parent.textRange
                }
            }
            if (parent is CaosScriptQuoteStringLiteral) {
                annotationHolder.newErrorAnnotation("Unclosed string literal")
                    .range(range)
                    .withFix(CaosScriptInsertBeforeFix("Add closing quote", "\"", element))
                    .create()
            } else if (parent is CaosScriptCharacter) {
                annotationHolder.newWarningAnnotation("Unclosed char literal")
                    .range(range)
                    .withFix(CaosScriptInsertBeforeFix("Add closing quote", "\'", element))
                    .create()
            } else {
                annotationHolder.newErrorAnnotation("Unclosed string")
                    .range(range)
                    .create()
            }
        }
        if (element !is CaosScriptCompositeElement || element.hasParentOfType(CaosDefCompositeElement::class.java))
            return
        // Get the expected terminator
        when (element) {

            is CaosScriptDoifStatement -> annotate(element, "ENDI", annotationHolder) {
                it.cEndi != null
            }
            is CaosScriptSubroutine -> annotate(element, "RETN", annotationHolder) {
                it.retnKw != null
            }
            is CaosScriptEnumNextStatement -> annotate(element, "NEXT", annotationHolder) {
                it.cNext != null || it.cNscn != null
            }
            is CaosScriptRepeatStatement -> annotate(element, "REPE", annotationHolder) {
                it.cRepe != null
            }
            is CaosScriptLoopStatement -> {
                annotate(element, listOf("UNTL", "EVER"), annotationHolder) { it ->
                    it.loopTerminator?.let { it.cEver != null || it.cUntl != null}.orFalse()
                }
            }
            is CaosScriptEnumSceneryStatement -> annotate(element, "NSCN", annotationHolder) {
                it.cNext != null || it.cNscn != null
            }
            // Ghost element here is SETV which is optional in grammar for C1 and C2, but not optional in CAOS
            is CaosScriptCAssignment -> {
                if (element.firstChild is CaosScriptLvalue) {
                    annotationHolder.newErrorAnnotation("${element.text.substring(0,4).uppercase()} is an LValue and must be used with an lvalue operator such as SETV ")
                            .range(element.firstChild)
                            .withFix(CaosScriptInsertBeforeFix("Prepend SETV to statement", "SETV".matchCase(element.lastChild.text, element.variant ?: CaosVariant.C1), element))
                            .create()
                }
            }
        }
    }

    /**
     * Actually annotate element when there is only one ghost element option
     */
    private fun <PsiT:PsiElement> annotate(element:PsiT, expectedToken:String, annotationHolder: AnnotationHolder, check:(element:PsiT)->Boolean) {
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
                var startOffset = it.endOffset - 2
                if (startOffset < 0)
                    startOffset = it.startOffset
                TextRange(startOffset, it.endOffset)
            }

        // Add annotation
        annotationHolder.newErrorAnnotation("Unterminated ${element.text.substring(0,4).uppercase()} statement. Expected ${expectedToken.uppercase()}")
                .range(range)
                .withFix(AppendStatementTerminator(element, expectedToken))
                .create()
    }

    /**
     * Annotate multiple options for ghost element.
     * Only used for LOOP...UNTL/EVER
     */
    private fun <PsiT:PsiElement> annotate(element:PsiT, expectedTokens:List<String>, annotationHolder: AnnotationHolder, check:(element:PsiT)->Boolean) {
        if (check(element)) {
            return
        }
        for(expectedToken in expectedTokens) {
            ProgressIndicatorProvider.checkCanceled()
            annotationHolder.newErrorAnnotation("Unterminated ${element.text.substring(0,4).uppercase()} statement. Expected ${expectedToken.uppercase()}")
                    .range(TextRange(element.lastChild.endOffset - 1, element.lastChild.endOffset))
                    .withFix(AppendStatementTerminator(element, expectedToken))
                    .create()
        }
    }
}