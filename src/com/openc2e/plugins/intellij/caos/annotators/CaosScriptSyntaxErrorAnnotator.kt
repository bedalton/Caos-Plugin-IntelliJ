package com.openc2e.plugins.intellij.caos.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.openc2e.plugins.intellij.caos.deducer.CaosScriptInferenceUtil
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.openc2e.plugins.intellij.caos.fixes.*
import com.openc2e.plugins.intellij.caos.indices.CaosScriptSubroutineIndex
import com.openc2e.plugins.intellij.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFile
import com.openc2e.plugins.intellij.caos.lang.variant
import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.psi.util.*
import com.openc2e.plugins.intellij.caos.utils.hasParentOfType
import com.openc2e.plugins.intellij.caos.utils.matchCase
import com.openc2e.plugins.intellij.caos.utils.orElse
import kotlin.math.abs
import kotlin.math.floor

class CaosScriptSyntaxErrorAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (DumbService.isDumb(element.project))
            return
        val annotationWrapper = AnnotationHolderWrapper(holder)
        val variant = (element.containingFile as? CaosScriptFile).variant.toUpperCase()
        when (element) {
            //is CaosScriptTrailingSpace -> annotateExtraSpaces(element, annotationWrapper)
            is CaosScriptSpaceLike -> annotateExtraSpaces(element, annotationWrapper)
            is CaosScriptSymbolComma -> annotateExtraSpaces(element, annotationWrapper)
            is CaosScriptEqOpNew -> annotateNewEqualityOps(variant, element, annotationWrapper)
            is CaosScriptEqualityExpressionPlus -> annotateEqualityExpressionPlus(variant, element, annotationWrapper)
            is CaosScriptElseIfStatement -> annotateElseIfStatement(variant, element, annotationWrapper)
            is CaosScriptCRetn -> annotateRetnCommand(variant, element, annotationWrapper)
            is CaosScriptExpressionList -> annotateExpressionList(element, annotationWrapper)
            is CaosScriptToken -> annotateToken(element, annotationWrapper)
            is CaosScriptQuoteStringLiteral -> annotateDoubleQuoteString(variant, element, annotationWrapper)
            is CaosScriptC1String -> annotateC1String(variant, element, annotationWrapper)
            is CaosScriptIsCommandToken -> annotateNotAvailable(variant, element, annotationWrapper)
            is CaosScriptVarToken -> annotateVarToken(variant, element, annotationWrapper)
            is CaosScriptNumber -> annotateNumber(variant, element, annotationWrapper)
            is CaosScriptSubroutineName -> annotateSubroutineName(variant, element, annotationWrapper)
            is CaosScriptCRndv -> annotateRndv(variant, element, annotationWrapper)
            is PsiComment -> annotateComment(variant, element, annotationWrapper)
            is CaosScriptIncomplete -> simpleError(element, "invalid element", annotationWrapper)
            is LeafPsiElement -> {
                if (element.parent is PsiErrorElement)
                    annotateErrorElement(variant, element, annotationWrapper)
            }
        }
    }

    private fun annotateRndv(variant: String, element: CaosScriptCRndv, annotationWrapper: AnnotationHolderWrapper) {
        if (variant !in VARIANT_OLD)
            return
        val minMax = element.rndvIntRange
        if (minMax.first < minMax.second)
            return
        if (minMax.first == minMax.second) {
            annotationWrapper.newWeakWarningAnnotation(CaosBundle.message("caos.annotator.command-annotator.rndv-result-is-the-same"))
                    .range(element.commandToken)
                    .create()
            return
        }
        listOfNotNull(element.minElement, element.maxElement).forEach {
            // Todo Infer min/max values from each variable value
            annotationWrapper.newWarningAnnotation(CaosBundle.message("caos.annotator.command-annotator.rndv-out-of-order", variant))
                    .range(it)
                    .withFix(CaosScriptReorderRndvParameters(element))
                    .create()
        }
    }

    private fun annotateErrorElement(variant: String, element: PsiElement, annotationWrapper: AnnotationHolderWrapper) {
        val command = element.text
        val commandUpperCase = element.text.toUpperCase()
        if (commandUpperCase == "CLAS" || commandUpperCase == "CLS2") {
            val setv = "SETV".matchCase(command)
            annotationWrapper
                    .newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.clas-is-lvalue-and-requires-setv", commandUpperCase))
                    .range(element)
                    .withFix(CaosScriptInsertBeforeFix("Insert '$setv' before $command", setv, element))
                    .create()
        }
    }

    private fun annotateSubroutineName(variant: String, element: CaosScriptSubroutineName, annotationWrapper: AnnotationHolderWrapper) {
        val name = element.name
        if ((variant == "C1" || variant == "C2") && name.length != 4) {
            annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.subroutine-name-invalid-length", variant))
                    .range(element)
                    .create()
        }
        val searchScope = GlobalSearchScope.fileScope(element.containingFile)
        val hasMatch = CaosScriptSubroutineIndex.instance[name, element.project, searchScope]
                .isNotEmpty()
        if (hasMatch)
            return
        annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.subroutine-not-found", name))
                .range(element)
                .create()
    }

    private fun annotateNumber(variant: String, element: CaosScriptNumber, annotationWrapper: AnnotationHolderWrapper) {
        if (variant != "C1" || element.decimal == null)
            return
        val floatValue = element.text.toFloat()
        var builder = annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.float-value-not-allowed-in-variant"))
                .range(element)
                .withFix(CaosScriptRoundNumberFix(element, floatValue, true))
        if (abs(floatValue - floor(floatValue)) > 0.00001)
            builder = builder.withFix(CaosScriptRoundNumberFix(element, floatValue, false))
        builder.create()
    }

    private fun annotateEqualityExpressionPlus(variant: String, element: CaosScriptEqualityExpressionPlus, annotationWrapper: AnnotationHolderWrapper) {
        if (variant !in VARIANT_OLD)
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

    private fun annotateDoubleQuoteString(variant: String, quoteStringLiteral: CaosScriptQuoteStringLiteral, wrapper: AnnotationHolderWrapper) {
        if (variant !in VARIANT_OLD)
            return
        wrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.out-of-variant-quote-string"))
                .range(quoteStringLiteral)
                .withFix(CaosScriptFixQuoteType(quoteStringLiteral, '[', ']'))
                .create()
    }

    private fun annotateC1String(variant: String, element: CaosScriptC1String, wrapper: AnnotationHolderWrapper) {
        if (variant == "C1") {
            if (element.parent?.parent is CaosScriptEqualityExpression) {
                wrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.string-comparisons-not-allowed"))
                        .range(element)
                        .create()
            } else if (element.getParentOfType(CaosScriptExpectsValueOfType::class.java)?.parent is CaosScriptCAssignment) {
                wrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.variable-string-assignments-not-allowed"))
                        .range(element)
                        .create()
            }
        }
        if (variant in VARIANT_OLD)
            return
        wrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.out-of-variant-c1-string"))
                .range(element)
                .withFix(CaosScriptFixQuoteType(element, '"'))
                .create()
    }


    private fun annotateElseIfStatement(variant: String, element: CaosScriptElseIfStatement, annotationWrapper: AnnotationHolderWrapper) {
        if (variant !in VARIANT_OLD)
            return
        annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.syntax-error-annotator.elif-not-available"))
                .range(element.cElif)
                .create()
    }

    @Suppress("SameParameterValue")
    private fun simpleError(element: PsiElement, message: String, annotationWrapper: AnnotationHolderWrapper) {
        annotationWrapper.newErrorAnnotation(message)
                .range(element)
                .create()
    }

    private fun annotateExtraSpaces(element: PsiElement, annotationWrapper: AnnotationHolderWrapper) {
        val nextText = element.next?.text ?: ""
        val prevText = element.previous?.text ?: ""
        val nextIsCommaOrSpace = IS_COMMA_OR_SPACE.matches(nextText)
        val previousIsCommaOrSpace = IS_COMMA_OR_SPACE.matches(prevText)
        if (element.text.length == 1 && !nextIsCommaOrSpace)
            return
        val errorTextRange = if (element.text.contains("\n") || previousIsCommaOrSpace)
            element.textRange
        else
            TextRange.create(element.textRange.startOffset, element.textRange.endOffset)
        annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.syntax-error-annotator.too-many-spaces"))
                .range(errorTextRange)
                .withFix(CaosScriptFixTooManySpaces(element))
                .newFix(CaosScriptTrimErrorSpaceBatchFix())
                .range(element.containingFile.textRange)
                .key(CaosScriptTrimErrorSpaceBatchFix.HIGHLIGHT_DISPLAY_KEY)
                .registerFix()
                .create()
    }

    private fun annotateComment(variant: String, element: PsiComment, annotationWrapper: AnnotationHolderWrapper) {
        if (variant !in VARIANT_OLD)
            return
        annotationWrapper.newErrorAnnotation("Comments are not allowed in version less than CV")
                .range(element)
                .create()
    }

    private fun annotateNewEqualityOps(variant: String, element: CaosScriptEqOpNew, annotationWrapper: AnnotationHolderWrapper) {
        if (variant !in VARIANT_OLD)
            return
        annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.syntax-error-annotator.invalid_eq_operator"))
                .range(element)
                .withFix(TransposeEqOp(element))
                .create()
    }

    private fun annotateRetnCommand(variant: String, element: CaosScriptCRetn, annotationWrapper: AnnotationHolderWrapper) {
        if (!element.hasParentOfType(CaosScriptSubroutine::class.java)) {
            annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.retn-used-outside-of-subr"))
                    .range(element)
                    .create()
        }
        if (!element.hasParentOfType(CaosScriptNoJump::class.java))
            return
        annotationWrapper.newAnnotation(HighlightSeverity.ERROR, CaosBundle.message("caos.annotator.command-annotator.loop-should-not-be-jumped-out-of"))
                .range(element.textRange)
                .create()
    }


    private fun annotateExpressionList(element: CaosScriptExpressionList, annotationWrapper: AnnotationHolderWrapper) {
        annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.stray-expressions-are-not-allowed"))
                .range(element)
                .create()
    }

    private fun annotateVarToken(variantIn: String, element: CaosScriptVarToken, annotationWrapper: AnnotationHolderWrapper) {
        val variant = variantIn.toUpperCase()
        if (variant == "C1") {
            if (element.parent?.parent is CaosScriptEqualityExpression) {
                val type = CaosScriptInferenceUtil.getInferredType(element)
                if (type == CaosExpressionValueType.C1_STRING || type == CaosExpressionValueType.BYTE_STRING || type == CaosExpressionValueType.STRING) {
                    annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.string-comparisons-not-allowed"))
                            .range(element)
                            .create()
                }
            }
        }
        val variants: String = if (element.varX != null) {
            if (variant !in VARIANT_OLD)
                "C1,C2"
            else
                return
        } else if (element.vaXx != null) {
            if (variant == "C1")
                "C2+"
            else
                return
        } else if (element.obvX != null) {
            if (variant !in VARIANT_OLD) {
                if (element.varIndex.orElse(100) < 3)
                    "C1,C2"
                else
                    "C2"
            } else if (variant == "C1" && element.varIndex.orElse(0) > 2)
                "C2"
            else
                return
        } else if (element.ovXx != null) {
            if (variant == "C1")
                "C2+"
            else
                return
        } else if (element.mvXx != null) {
            if (variant in VARIANT_OLD)
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

    companion object {
        internal val VARIANT_OLD = listOf("C1", "C2")
        private val IS_COMMA_OR_SPACE = "[\\s,]+".toRegex()

        internal fun annotateNotAvailable(variant: String, element: CaosScriptIsCommandToken, annotationWrapper: AnnotationHolderWrapper) {
            val command = element.commandString
            val commandType = element.getEnclosingCommandType()
            val commands = CaosDefCommandElementsByNameIndex
                    .Instance[command, element.project]
            var variants = commands
                    .flatMap { it.variants.filterNotNull() }
                    .toSet()
                    .toList()
            if (variants.isEmpty()) {
                annotationWrapper
                        .newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.invalid-command", command))
                        .range(element)
                        .create()
                return
            }

            if (variant !in variants) {
                val variantString = getVariantString(variants)
                val message = CaosBundle.message("caos.annotator.command-annotator.invalid-variant", command, variantString)
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
                    command.toUpperCase(),
                    commandType.value.toLowerCase(),
                    getVariantString(variants)
            )
            var builder = annotationWrapper
                    .newErrorAnnotation(error)
                    .range(element)
            if (variant in VARIANT_OLD) {
                builder = builder
                        .withFix(CaosScriptInsertBeforeFix("Insert SETV before NORN", "SETV".matchCase(command), element))
            }
            builder.create()
        }

        private fun getVariantString(variantsIn: List<String>): String {
            val variants = sortVariants(variantsIn)
            return when {
                4 == variants.intersect(listOf("C2", "CV", "C3", "DS")).size -> "C2+"
                3 == variants.intersect(listOf("CV", "C3", "DS")).size -> "CV+"
                2 == variants.intersect(listOf("C3", "DS")).size -> "C3+"
                else -> variants.joinToString(",")
            }
        }


        private fun sortVariants(variants: List<String>): List<String> {
            return variants.sortedBy {
                when (it.toLowerCase()) {
                    "c1" -> 1
                    "c2" -> 2
                    "cv" -> 3
                    "c3" -> 4
                    "ds" -> 5
                    "sm" -> 6
                    else -> 7
                }
            }
        }
    }

}