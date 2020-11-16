package com.badahori.creatures.plugins.intellij.agenteering.caos.annotators

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosScriptInferenceUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.highlighting.CaosScriptSyntaxHighlighter
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle.message
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant.UNKNOWN
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType.STRING
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommandType.LVALUE
import com.badahori.creatures.plugins.intellij.agenteering.utils.hasParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.matchCase
import com.badahori.creatures.plugins.intellij.agenteering.utils.orElse
import com.badahori.creatures.plugins.intellij.agenteering.utils.SPACES_REGEX
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import kotlin.math.abs
import kotlin.math.floor

class CaosScriptSyntaxErrorAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (DumbService.isDumb(element.project))
            return
        if (element.isOrHasParentOfType(CaosDefCompositeElement::class.java))
            return
        if (!element.isPhysical)
            return
        // Make annotation wrapper to aid in creating annotation
        val annotationWrapper = AnnotationHolderWrapper(holder)

        // Get variant for element's parent file
        val variant = (element.containingFile as? CaosScriptFile)?.variant
        // Cannot validate without a variant. Return early if null
                ?: return
        // Annotate element based on type
        when (element) {
            is CaosScriptSpaceLike -> annotateExtraSpaces(variant, element, annotationWrapper)
            is CaosScriptSymbolComma -> annotateExtraSpaces(variant, element, annotationWrapper)
            is CaosScriptEqOpNew -> annotateNewEqualityOps(variant, element, annotationWrapper)
            is CaosScriptEqualityExpressionPlus -> annotateEqualityExpressionPlus(variant, element, annotationWrapper)
            is CaosScriptElseIfStatement -> annotateElseIfStatement(variant, element, annotationWrapper)
            is CaosScriptCRetn -> annotateRetnCommand(element, annotationWrapper)
            is CaosScriptToken -> annotateToken(element, annotationWrapper)
            is CaosScriptQuoteStringLiteral -> annotateDoubleQuoteString(variant, element, annotationWrapper)
            is CaosScriptC1String -> annotateC1String(variant, element, annotationWrapper)
            is CaosScriptIsCommandToken -> annotateNotAvailable(variant, element, annotationWrapper)
            is CaosScriptVarToken -> annotateVarToken(variant, element, annotationWrapper)
            is CaosScriptNumber -> annotateNumber(variant, element, annotationWrapper)
            is CaosScriptErrorRvalue -> annotationWrapper
                    .newErrorAnnotation("Unrecognized rvalue")
                    .range(element)
                    .create()
            is CaosScriptErrorCommand -> annotateErrorCommand(variant, element, annotationWrapper)
            is CaosScriptCharacter -> {
                if (element.charChar?.textLength.orElse(0) > 1 && element.charChar?.text != "\\\\") {
                    simpleError(element.charChar ?: element, "Char value can be only one character", annotationWrapper)
                }
            }
            is CaosScriptErrorLvalue -> {
                val variableType = if (element.number != null) {
                    "number"
                } else if (element.quoteStringLiteral != null || element.c1String != null) {
                    "string"
                } else if (element.animationString != null) {
                    "animation"
                } else if (element.byteString != null) {
                    "byte-string"
                } else {
                    "literal"
                }
                simpleError(element, "Variable expected. Found $variableType", annotationWrapper)
            }
            is CaosScriptIncomplete -> {
                if (element.hasParentOfType(CaosScriptSubroutineName::class.java)) {
                    return
                }
                simpleError(element, "invalid element", annotationWrapper)
            }
            //is CaosScriptCAssignment -> annotateSetvCompoundLvalue(variant, element, annotationWrapper)
            is CaosScriptSpaceLikeOrNewline -> annotateNewLineLike(variant, element, annotationWrapper)
            is CaosScriptTrailingSpace -> annotationTrailingWhiteSpace(variant, element, annotationWrapper)
            is LeafPsiElement -> {
                if (element.parent is PsiErrorElement)
                    annotateErrorElement(variant, element, annotationWrapper)
            }
        }
    }

    /**
     * Further annotates an error element as it may possibly be an LValue used as a command
     */
    private fun annotateErrorElement(variant: CaosVariant, element: PsiElement, annotationWrapper: AnnotationHolderWrapper) {
        val command = element.text
        val commandUpperCase = element.text.toUpperCase()
        val isLvalue = commandUpperCase == "CLAS" || commandUpperCase == "CLS2" || CaosLibs[variant][LVALUE][command] != null
        if (isLvalue) {
            val setv = "SETV".matchCase(command)
            annotationWrapper
                    .newErrorAnnotation(message("caos.annotator.command-annotator.clas-is-lvalue-and-requires-setv", commandUpperCase))
                    .range(element)
                    .withFix(CaosScriptInsertBeforeFix("Insert '$setv' before $command", setv, element))
                    .create()
        }
    }

    /**
     * Annotates a number if it is a float used outside of C2+
     */
    private fun annotateNumber(variant: CaosVariant, element: CaosScriptNumber, annotationWrapper: AnnotationHolderWrapper) {
        if (variant != CaosVariant.C1 || element.float == null)
            return
        val floatValue = element.text.toFloat()
        var builder = annotationWrapper.newErrorAnnotation(message("caos.annotator.command-annotator.float-value-not-allowed-in-variant"))
                .range(element)
                .withFix(CaosScriptRoundNumberFix(element, floatValue, true))
        if (abs(floatValue - floor(floatValue)) > 0.00001)
            builder = builder.withFix(CaosScriptRoundNumberFix(element, floatValue, false))
        builder.create()
    }

    private fun annotateEqualityExpressionPlus(variant: CaosVariant, element: CaosScriptEqualityExpressionPlus, annotationWrapper: AnnotationHolderWrapper) {
        if (variant.isNotOld)
            return
        annotationWrapper.newErrorAnnotation(message("caos.annotator.command-annotator.compound-equality-operator-not-allowed"))
                .range(element)
                .create()
    }

    /**
     * Annotates a token as unexpected if not found in proper Token parent
     */
    private fun annotateToken(element: CaosScriptToken, annotationWrapper: AnnotationHolderWrapper) {
        if (element.hasParentOfType(CaosScriptTokenRvalue::class.java))
            return
        annotationWrapper.newErrorAnnotation("Unexpected token")
                .range(element)
                .create()
    }

    /**
     * Annotates a string in double quotes as out of variant in C1/C2
     */
    private fun annotateDoubleQuoteString(variant: CaosVariant, quoteStringLiteral: CaosScriptQuoteStringLiteral, wrapper: AnnotationHolderWrapper) {
        if (variant.isNotOld)
            return
        wrapper.newErrorAnnotation(message("caos.annotator.command-annotator.out-of-variant-quote-string"))
                .range(quoteStringLiteral)
                .withFix(CaosScriptFixQuoteType(quoteStringLiteral, '[', ']'))
                .create()
    }

    /**
     * Annotates a C1 string as invalid when assigned or used in CV+
     */
    private fun annotateC1String(variant: CaosVariant, element: CaosScriptC1String, wrapper: AnnotationHolderWrapper) {
        if (variant.isOld) {
            if (element.parent?.parent is CaosScriptEqualityExpression) {
                wrapper.newErrorAnnotation(message("caos.annotator.command-annotator.string-comparisons-not-allowed", variant))
                        .range(element)
                        .create()
            } else if (element.getParentOfType(CaosScriptArgument::class.java)?.parent is CaosScriptCAssignment) {
                wrapper.newErrorAnnotation(message("caos.annotator.command-annotator.variable-string-assignments-not-allowed", variant))
                        .range(element)
                        .create()
            }
            return
        }
        wrapper.newErrorAnnotation(message("caos.annotator.command-annotator.out-of-variant-c1-string"))
                .range(element)
                .withFix(CaosScriptFixQuoteType(element, '"'))
                .create()
    }


    /**
     * Annotates ELIF as invalid in C1/C2
     */
    private fun annotateElseIfStatement(variant: CaosVariant, element: CaosScriptElseIfStatement, annotationWrapper: AnnotationHolderWrapper) {
        if (variant.isNotOld)
            return
        annotationWrapper.newErrorAnnotation(message("caos.annotator.syntax-error-annotator.elif-not-available"))
                .range(element.cElif)
                .create()
    }

    /**
     * Annotates an error message on an element
     */
    @Suppress("SameParameterValue")
    private fun simpleError(element: PsiElement, message: String, annotationWrapper: AnnotationHolderWrapper) {
        annotationWrapper.newErrorAnnotation(message)
                .range(element)
                .create()
    }

    /**
     * Annotates spacing errors, mostly multiple whitespaces within a command
     */
    private fun annotateExtraSpaces(variant: CaosVariant, element: PsiElement, annotationWrapper: AnnotationHolderWrapper) {
        // Spacing does not matter in CV+, so return
        if (variant.isNotOld)
            return

        // Get this elements text
        val text = element.text

        // Get text before and after this space
        val nextText = element.next?.text ?: ""
        val prevText = element.previous?.text ?: ""

        // Test if previous element is a comma or space
        val previousIsCommaOrSpace = IS_COMMA_OR_SPACE.matches(prevText)

        // Is a single space, and not followed by terminating comma or newline
        if (text.length == 1 && !(COMMA_NEW_LINE_REGEX.matches(nextText) || previousIsCommaOrSpace))
            return

        // Psi element is empty, denoting a missing space, possible after quote or byte-string or number
        if (text.isEmpty() && variant.isOld) {
            val next = element.next
                    ?: return

            val toMark = TextRange(element.startOffset - 1, next.startOffset + 1)
            annotationWrapper.newErrorAnnotation(message("caos.annotator.syntax-error-annotator.too-many-spaces"))
                    .range(toMark)
                    .withFix(CaosScriptInsertSpaceFix(next))
                    .create()
            return
        }
        // Did check for trailing comma, but this is assumed to be removed before injection
        // I think BoBCoB does this and CyberLife CAOS tool strips this as well.
        if (nextText.startsWith("\n") || element.node.isDirectlyPrecededByNewline()) {// && variant != CaosVariant.C1) {
            return
        }
        val errorTextRange = element.textRange.let {
            if (element.text.length > 1)
                TextRange(it.startOffset + 1, it.endOffset)
            else
                it
        }
        annotationWrapper.newErrorAnnotation(message("caos.annotator.syntax-error-annotator.too-many-spaces"))
                .range(errorTextRange)
                .withFix(CaosScriptFixTooManySpaces(element))
                .newFix(CaosScriptTrimErrorSpaceBatchFix())
                .range(element.containingFile.textRange)
                .key(CaosScriptTrimErrorSpaceBatchFix.HIGHLIGHT_DISPLAY_KEY)
                .registerFix()
                .create()
        return
    }

    private fun annotateNewEqualityOps(variant: CaosVariant, element: CaosScriptEqOpNew, annotationWrapper: AnnotationHolderWrapper) {
        if (variant.isNotOld || variant == UNKNOWN)
            return
        annotationWrapper.newErrorAnnotation(message("caos.annotator.syntax-error-annotator.invalid_eq_operator"))
                .range(element)
                .withFix(TransposeEqOp(element))
                .create()
    }

    private fun annotateRetnCommand(element: CaosScriptCRetn, annotationWrapper: AnnotationHolderWrapper) {
        if (!element.hasParentOfType(com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSubroutine::class.java)) {
            annotationWrapper.newErrorAnnotation(message("caos.annotator.command-annotator.retn-used-outside-of-subr"))
                    .range(element)
                    .create()
        }
        if (!element.hasParentOfType(com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptNoJump::class.java))
            return
        annotationWrapper.newAnnotation(HighlightSeverity.ERROR, message("caos.annotator.command-annotator.loop-should-not-be-jumped-out-of"))
                .range(element.textRange)
                .create()
    }

    private fun annotateVarToken(variant: CaosVariant, element: CaosScriptVarToken, annotationWrapper: AnnotationHolderWrapper) {
        if (variant == CaosVariant.C1) {
            if (element.parent?.parent is CaosScriptEqualityExpression) {
                val type = CaosScriptInferenceUtil.getInferredType(element)
                if (type == CaosExpressionValueType.C1_STRING || type == CaosExpressionValueType.BYTE_STRING || type == STRING) {
                    annotationWrapper.newErrorAnnotation(message("caos.annotator.command-annotator.string-comparisons-not-allowed", variant))
                            .range(element)
                            .create()
                }
            }
        }
        val variants: String = if (element.varX != null) {
            if (variant.isNotOld)
                "C1,C2"
            else
                return
        } else if (element.vaXx != null) {
            if (variant == CaosVariant.C1)
                "C2+"
            else
                return
        } else if (element.obvX != null) {
            if (variant.isNotOld) {
                if (element.varIndex.orElse(100) < 3)
                    "C1,C2"
                else
                    "C2"
            } else if (variant == CaosVariant.C1 && element.varIndex.orElse(0) > 2)
                "C2"
            else
                return
        } else if (element.ovXx != null) {
            if (variant == CaosVariant.C1)
                "C2+"
            else
                return
        } else if (element.mvXx != null) {
            if (variant.isOld)
                "CV+"
            else
                return
        } else
            return

        val varName = when {
            element.varX != null -> "VARx"
            element.vaXx != null -> "VAxx"
            element.obvX != null -> if (element.varIndex.orElse(0) > 2) "OBVx[3-9]" else "OBVx"
            element.ovXx != null -> "OVxx"
            element.mvXx != null -> "MVxx"
            else -> element.text
        }

        val error = message("caos.annotator.command-annotator.invalid-var-type-for-variant", varName, variants)
        annotationWrapper
                .newErrorAnnotation(error)
                .range(element)
                .create()
    }

    private fun annotateNewLineLike(variant: CaosVariant, element: CaosScriptSpaceLikeOrNewline, annotationWrapper: AnnotationHolderWrapper) {
        if (variant.isNotOld)
            return
        if (element.parent == element.containingFile && element.containingFile.firstChild == element && variant == CaosVariant.C1) {
            simpleError(element, "CAOS files should not begin with leading whitespace", annotationWrapper)
            return
        }
        if (element.spaceLikeList.isNotEmpty() && element.newLineLikeList.isNotEmpty()) {
            val lastNewLine = element.newLineLikeList.last()
            val lastNewLineOffset = lastNewLine.startOffset
            val prevSpaces = element.children.filter {
                it.startOffset < lastNewLineOffset && !it.textContains('\n')
            }.sortedBy {
                it.startOffset
            }
            if (prevSpaces.isNotEmpty()) {
                val range = TextRange.create(prevSpaces.first().startOffset, prevSpaces.last().endOffset)
                val error = message("caos.annotator.syntax-annotator.invalid-trailing-whitespace")
                annotationWrapper
                        .newErrorAnnotation(error)
                        .range(range)
                        .withFix(CaosScriptFixTooManySpaces(prevSpaces.last()))
                        .create()
                return
            }
        }
    }

    private fun annotateErrorCommand(variant: CaosVariant, errorCommand: CaosScriptErrorCommand, annotationWrapper: AnnotationHolderWrapper) {
        // Get error as possible error annotation
        val errorAnnotation = errorCommand
                .text
                .toUpperCase()
                .split(SPACES_REGEX, 3)
                .let { tokens ->
                    if (tokens.size > 1)
                        listOf("${tokens[0]} ${tokens[1]}", tokens[0])
                    else
                        listOf(tokens[0])
                }
                .mapNotNull { commandToken ->
                    getErrorCommandAnnotation(variant, errorCommand, commandToken, annotationWrapper)
                }

        // Check for length less than 2, meaning one matched.
        // If one matches, that means the command is in the definitions file
        // but not in the BNF parser grammar.
        // TODO should we throw error, or simply return without annotating.
        if (errorAnnotation.size != 2) {
            throw Exception("Command found in definitions for element: ${errorCommand.text}, but BNF grammar does not reflect this.")
        }
        errorAnnotation.last().create()
    }

    private fun annotationTrailingWhiteSpace(variant: CaosVariant, element: CaosScriptTrailingSpace, annotationWrapper: AnnotationHolderWrapper) {
        if (element.textContains(',')) {
            annotationWrapper.newErrorAnnotation(message("caos.annotator.syntax-annotator.invalid-trailing-whitespace"))
                    .range(element)
                    .withFix(CaosScriptFixTooManySpaces(element))
                    .create()
        }
        if (variant.isNotOld)
            return
    }

    companion object {
        private val IS_COMMA_OR_SPACE = "[\\s,]+".toRegex()
        private val COMMA_NEW_LINE_REGEX = "([,]|\\s)+".toRegex()

        internal fun annotateNotAvailable(variant: CaosVariant, element: CaosScriptIsCommandToken, annotationWrapper: AnnotationHolderWrapper) {
            // Colorize element as token
            if (element.isOrHasParentOfType(CaosScriptRKwNone::class.java) && element.hasParentOfType(CaosScriptTokenRvalue::class.java)) {
                annotationWrapper.colorize(element, CaosScriptSyntaxHighlighter.TOKEN)
                return
            }
            val commandText = element.commandString.toUpperCase()

            // Get error annotation, run create if needed
            getErrorCommandAnnotation(variant, element, commandText, annotationWrapper)?.create()
        }
    }

}

