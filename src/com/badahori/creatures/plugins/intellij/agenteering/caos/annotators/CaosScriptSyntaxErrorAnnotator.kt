package com.badahori.creatures.plugins.intellij.agenteering.caos.annotators

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosScriptInferenceUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.highlighting.CaosScriptSyntaxHighlighter
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.hasParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.matchCase
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.orElse
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
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
            return;
        val annotationWrapper = AnnotationHolderWrapper(holder)
        val variant = (element.containingFile as? CaosScriptFile)?.variant
                ?: return
        when (element) {
            //is CaosScriptTrailingSpace -> annotateExtraSpaces(element, annotationWrapper)
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
            is PsiComment, is CaosScriptComment -> annotateComment(variant, element, annotationWrapper)
            is CaosScriptCharacter -> {
                if (element.charChar?.textLength.orElse(0) > 1 && element.charChar?.text != "\\\\") {
                    simpleError(element.charChar?:element, "Char value can be only one character", annotationWrapper)
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
                    return;
                }
                simpleError(element, "invalid element", annotationWrapper)
            }
            is CaosScriptCAssignment -> annotateSetvCompoundLvalue(variant, element, annotationWrapper)
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
    private fun annotateErrorElement(variant:CaosVariant, element: PsiElement, annotationWrapper: AnnotationHolderWrapper) {
        val command = element.text
        val commandUpperCase = element.text.toUpperCase()
        val isLvalue = commandUpperCase == "CLAS" || commandUpperCase == "CLS2" || CaosDefCommandElementsByNameIndex.Instance[commandUpperCase, element.project]
                .any {
                    it.isVariant(variant) && it.isLvalue
                }
        if (isLvalue) {
            val setv = "SETV".matchCase(command)
            annotationWrapper
                    .newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.clas-is-lvalue-and-requires-setv", commandUpperCase))
                    .range(element)
                    .withFix(CaosScriptInsertBeforeFix("Insert '$setv' before $command", setv, element))
                    .create()
        }
    }

    /**
     * Annotates a number if it is a float used outside of C2+
     */
    private fun annotateNumber(variant: CaosVariant, element: CaosScriptNumber, annotationWrapper: AnnotationHolderWrapper) {
        if (variant != CaosVariant.C1 || element.decimal == null)
            return
        val floatValue = element.text.toFloat()
        var builder = annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.float-value-not-allowed-in-variant"))
                .range(element)
                .withFix(CaosScriptRoundNumberFix(element, floatValue, true))
        if (abs(floatValue - floor(floatValue)) > 0.00001)
            builder = builder.withFix(CaosScriptRoundNumberFix(element, floatValue, false))
        builder.create()
    }

    private fun annotateEqualityExpressionPlus(variant: CaosVariant, element: CaosScriptEqualityExpressionPlus, annotationWrapper: AnnotationHolderWrapper) {
        if (variant.isNotOld)
            return
        annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.compound-equality-operator-not-allowed"))
                .range(element)
                .create()
    }

    private fun annotateToken(element: CaosScriptToken, annotationWrapper: AnnotationHolderWrapper) {
        val parentExpectation = element.getParentOfType(CaosScriptExpectsValueOfType::class.java)
        if (parentExpectation != null && parentExpectation is CaosScriptExpectsToken)
            return
        annotationWrapper.newErrorAnnotation("Unexpected token")
                .range(element)
                .create()
    }

    private fun annotateDoubleQuoteString(variant: CaosVariant, quoteStringLiteral: CaosScriptQuoteStringLiteral, wrapper: AnnotationHolderWrapper) {
        if (variant.isNotOld)
            return
        wrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.out-of-variant-quote-string"))
                .range(quoteStringLiteral)
                .withFix(CaosScriptFixQuoteType(quoteStringLiteral, '[', ']'))
                .create()
    }

    private fun annotateC1String(variant: CaosVariant, element: CaosScriptC1String, wrapper: AnnotationHolderWrapper) {
        if (variant.isOld) {
            if (element.parent?.parent is CaosScriptEqualityExpression) {
                wrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.string-comparisons-not-allowed", variant))
                        .range(element)
                        .create()
            } else if (element.getParentOfType(CaosScriptExpectsValueOfType::class.java)?.parent is CaosScriptCAssignment) {
                wrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.variable-string-assignments-not-allowed", variant))
                        .range(element)
                        .create()
            }
            return
        }
        wrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.out-of-variant-c1-string"))
                .range(element)
                .withFix(CaosScriptFixQuoteType(element, '"'))
                .create()
    }


    private fun annotateElseIfStatement(variant: CaosVariant, element: CaosScriptElseIfStatement, annotationWrapper: AnnotationHolderWrapper) {
        if (variant.isNotOld)
            return
        annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.syntax-error-annotator.elif-not-available"))
                .range(element.cElif)
                .create()
    }

    private fun annotateSetvCompoundLvalue(variant: CaosVariant, element: CaosScriptCAssignment, annotationWrapper: AnnotationHolderWrapper) {
        if (variant.isNotOld)
            return
        val args = element.arguments
        if (args.size == 2) {
            return
        }
        if (element.cKwNegv != null && args.size == 1)
            return
        val lvalue = args.getOrNull(0) as? CaosScriptLvalue
        if (lvalue?.argumentsLength.orElse(0) > 0)
            return
        annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.syntax-error-annotator.expects-a-value"))
                .range(TextRange(element.endOffset-1, element.endOffset))
                .create()
    }

    @Suppress("SameParameterValue")
    private fun simpleError(element: PsiElement, message: String, annotationWrapper: AnnotationHolderWrapper) {
        annotationWrapper.newErrorAnnotation(message)
                .range(element)
                .create()
    }

    private fun annotateExtraSpaces(variant: CaosVariant, element: PsiElement, annotationWrapper: AnnotationHolderWrapper) {
        val nextText = element.next?.text ?: ""
        val prevText = element.previous?.text ?: ""
        if (variant.isNotOld)
            return
        if (prevText.contains("\n")) {
            if (false) { //(element.containingFile as? CaosScriptFile)?.variant == CaosVariant.C1) {
                annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.syntax-error-annotator.c1-leading-spaces"))
                        .range(element)
                        .withFix(CaosScriptFixTooManySpaces(element))
                        .newFix(CaosScriptTrimErrorSpaceBatchFix())
                        .range(element.containingFile.textRange)
                        .key(CaosScriptTrimErrorSpaceBatchFix.HIGHLIGHT_DISPLAY_KEY)
                        .registerFix()
                        .create()
            }
            return
        }
        //val nextIsCommaOrSpace = IS_COMMA_OR_SPACE.matches(nextText)
        val previousIsCommaOrSpace = IS_COMMA_OR_SPACE.matches(prevText)
        val text = element.text
        // Is a single space, and not followed by terminating comma or newline
        if (text.length == 1 && !(COMMA_NEW_LINE_REGEX.matches(nextText) || previousIsCommaOrSpace))
            return
        // Psi element is empty, denoting a missing space, possible after quote or bytestring or number
        if (text.isEmpty() && variant.isOld) {
            val next = element.next
                    ?: return
            val toMark = TextRange(element.startOffset - 1, next.startOffset+1)
            annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.syntax-error-annotator.too-many-spaces"))
                    .range(toMark)
                    .withFix(CaosScriptInsertSpaceFix(next))
                    .create()
            return
        }
        // Did check for trailing comma, but this is assumed to be removed before injection
        // I think bobcob does this and Cyberlife CAOS tool strips this as well.
        if (nextText.startsWith("\n") || element.node.isDirectlyPrecededByNewline()) {// && variant != CaosVariant.C1) {
            return
        }
        val errorTextRange = element.textRange.let {
            if (element.text.length > 1)
                TextRange(it.startOffset + 1, it.endOffset)
            else
                it
        }
        annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.syntax-error-annotator.too-many-spaces"))
                .range(errorTextRange)
                .withFix(CaosScriptFixTooManySpaces(element))
                .newFix(CaosScriptTrimErrorSpaceBatchFix())
                .range(element.containingFile.textRange)
                .key(CaosScriptTrimErrorSpaceBatchFix.HIGHLIGHT_DISPLAY_KEY)
                .registerFix()
                .create()
        return
    }

    private fun annotateComment(variant: CaosVariant, element: PsiElement, annotationWrapper: AnnotationHolderWrapper) {
        // Skip comment error for now, as C2 CAOS tool strips comments anyways, so we should too?
        if (true || variant != CaosVariant.C1)
            return

        annotationWrapper.newErrorAnnotation("Comments are not allowed in [C1] variant")
                .range(element)
                .create()
    }

    private fun annotateNewEqualityOps(variant: CaosVariant, element: CaosScriptEqOpNew, annotationWrapper: AnnotationHolderWrapper) {
        if (variant.isNotOld)
            return
        annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.syntax-error-annotator.invalid_eq_operator"))
                .range(element)
                .withFix(TransposeEqOp(element))
                .create()
    }

    private fun annotateRetnCommand(element: CaosScriptCRetn, annotationWrapper: AnnotationHolderWrapper) {
        if (!element.hasParentOfType(com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSubroutine::class.java)) {
            annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.retn-used-outside-of-subr"))
                    .range(element)
                    .create()
        }
        if (!element.hasParentOfType(com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptNoJump::class.java))
            return
        annotationWrapper.newAnnotation(HighlightSeverity.ERROR, CaosBundle.message("caos.annotator.command-annotator.loop-should-not-be-jumped-out-of"))
                .range(element.textRange)
                .create()
    }

    private fun annotateVarToken(variant: CaosVariant, element: CaosScriptVarToken, annotationWrapper: AnnotationHolderWrapper) {
        if (variant == CaosVariant.C1) {
            if (element.parent?.parent is CaosScriptEqualityExpression) {
                val type = CaosScriptInferenceUtil.getInferredType(element)
                if (type == CaosExpressionValueType.C1_STRING || type == CaosExpressionValueType.BYTE_STRING || type == CaosExpressionValueType.STRING) {
                    annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.string-comparisons-not-allowed", variant))
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

        val error = CaosBundle.message("caos.annotator.command-annotator.invalid-var-type-for-variant", varName, variants)
        annotationWrapper
                .newErrorAnnotation(error)
                .range(element)
                .create()
    }

    private fun annotateNewLineLike(variant:CaosVariant, element:CaosScriptSpaceLikeOrNewline, annotationWrapper: AnnotationHolderWrapper) {
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
            if(prevSpaces.isNotEmpty()) {
                val range = TextRange.create(prevSpaces.first().startOffset, prevSpaces.last().endOffset)
                val error = CaosBundle.message("caos.annotator.syntax-annotator.invalid-trailing-whitespace")
                annotationWrapper
                        .newErrorAnnotation(error)
                        .range(range)
                        .withFix(CaosScriptFixTooManySpaces(prevSpaces.last()))
                        .create()
                return
            }
        }
        /*
        val next = element.next
                ?: return
        if (element.text.contains(",")) {
            if (next.textContains('\n')) {
                val error = CaosBundle.message("caos.annotator.syntax-annotator.invalid-trailing-comma")
                annotationWrapper
                        .newErrorAnnotation(error)
                        .range(element)
                        .create()
            }
            return
        }
        if (next.text.contains(",")) {
            val error = CaosBundle.message("caos.annotator.command-annotator.invalid-var-type-for-variant", varName, variants)
            annotationWrapper
                    .newErrorAnnotation(error)
                    .range(element)
                    .create()
        }
        if (element.text.contains(",") && element.next?.text?.contains(",")) {
            if (element.)
        }*/
    }

    private fun annotationTrailingWhiteSpace(variant: CaosVariant, element: CaosScriptTrailingSpace, annotationWrapper: AnnotationHolderWrapper) {
        if (element.textContains(',')) {
            annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.syntax-annotator.invalid-trailing-whitespace"))
                    .range(element)
                    .withFix(CaosScriptFixTooManySpaces(element))
                    .create()
        }
        if (variant.isNotOld)
            return
    }

    companion object {
        private val IS_COMMA_OR_SPACE = "[\\s,]+".toRegex()

        private val COMMA_SPACED = "(\\s+,\\s*)||(\\s*,\\s+)".toRegex()
        private val TRAILING_SPACE_REGEX = "[ ]+[,]?\n".toRegex()
        private val WHITE_SPACE = "[ ]+".toRegex()
        private val COMMA_NEW_LINE_REGEX = "([,]|\\s)+".toRegex()

        internal fun annotateNotAvailable(variant: CaosVariant, element: CaosScriptIsCommandToken, annotationWrapper: AnnotationHolderWrapper) {
            if (element.isOrHasParentOfType(CaosScriptRKwNone::class.java) && element.hasParentOfType(CaosScriptExpectsToken::class.java)) {
                annotationWrapper.colorize(element, CaosScriptSyntaxHighlighter.TOKEN)
                return
            }
            val commandToUpperCase = element.commandString.toUpperCase()
            val commandType = element.getEnclosingCommandType()
            val commands = CaosDefCommandElementsByNameIndex
                    .Instance[commandToUpperCase, element.project]
            var variants = commands
                    .flatMap { it.variants.filterNotNull() }
                    .toSet()
                    .toList()
            if (variants.isEmpty()) {
                annotationWrapper
                        .newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.invalid-command", commandToUpperCase))
                        .range(element)
                        .create()
                return
            }

            if (variant !in variants) {
                val variantString = getVariantString(variants)
                val message = CaosBundle.message("caos.annotator.command-annotator.invalid-variant", commandToUpperCase, variantString)
                annotationWrapper
                        .newErrorAnnotation(message)
                        .range(element)
                        .create()
                return
            }
            // If command type cannot be determined, exit This means it was used as out of command expression
            if (commandType == CaosCommandType.UNDEFINED)
                return

            // Filter commands by matching type (ie. Command, RValue, LValue)
            val commandsOfType = commands
                    .filter {
                        when (commandType) {
                            CaosCommandType.COMMAND -> it.isCommand
                            CaosCommandType.RVALUE -> it.isRvalue
                            CaosCommandType.LVALUE -> it.isLvalue
                            CaosCommandType.CONTROL_STATEMENT -> it.isCommand
                            CaosCommandType.UNDEFINED -> false
                        }
                    }
            // Command variant of type exists, so exit
            if (commandsOfType.any { it.isVariant(variant) })
                return

            variants = commandsOfType
                    .flatMap { it.variants }
                    .toSet()
                    .filterNotNull()
            // Command variant of type does not exist, show error
            val error = CaosBundle.message(
                    "caos.annotator.command-annotator.invalid-command-type-for-variant",
                    commandToUpperCase,
                    commandType.value.toLowerCase(),
                    getVariantString(variants)
            )
            var builder = annotationWrapper
                    .newErrorAnnotation(error)
                    .range(element)
            if (variant.isOld && commands.any { it.isLvalue && it.isVariant(variant) }) {
                builder = builder
                        .withFix(CaosScriptInsertBeforeFix("Insert SETV before ${commandToUpperCase}", "SETV".matchCase(element.commandString), element))
            }
            builder.create()
        }

        private fun getVariantString(variantsIn: List<CaosVariant>): String {
            val variants = variantsIn.sortedBy { it.index }
            return when {
                4 == variants.intersect(listOf(CaosVariant.C2, CaosVariant.CV, CaosVariant.C3, CaosVariant.DS)).size -> "C2+"
                3 == variants.intersect(listOf(CaosVariant.CV, CaosVariant.C3, CaosVariant.DS)).size -> "CV+"
                2 == variants.intersect(listOf(CaosVariant.C3, CaosVariant.DS)).size -> "C3+"
                else -> variants.joinToString(",") { it.code }
            }
        }
    }

}