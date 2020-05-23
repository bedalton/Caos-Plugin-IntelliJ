package com.openc2e.plugins.intellij.caos.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.openc2e.plugins.intellij.caos.fixes.CaosScriptFixTooManySpaces
import com.openc2e.plugins.intellij.caos.fixes.CaosScriptTrimErrorSpaceBatchFix
import com.openc2e.plugins.intellij.caos.fixes.TransposeEqOp
import com.openc2e.plugins.intellij.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.psi.impl.containingCaosFile
import com.openc2e.plugins.intellij.caos.psi.util.*
import com.openc2e.plugins.intellij.caos.utils.hasParentOfType
import com.openc2e.plugins.intellij.caos.utils.nullIfEmpty
import com.openc2e.plugins.intellij.caos.utils.orElse

class CaosScriptSyntaxErrorAnnotator : Annotator {


    override fun annotate(elementIn: PsiElement, holder: AnnotationHolder) {
        if (DumbService.isDumb(elementIn.project))
            return
        val annotationWrapper = AnnotationHolderWrapper(holder)
        val element = elementIn as? CaosScriptCompositeElement
                ?: return
        val variant = element.containingCaosFile?.variant?.toUpperCase()
                ?: ""
        when (element) {
            //is CaosScriptTrailingSpace -> annotateExtraSpaces(element, annotationWrapper)
            is CaosScriptSpaceLike -> annotateExtraSpaces(element, annotationWrapper)
            is CaosScriptSymbolComma -> annotateExtraSpaces(element, annotationWrapper)
            is CaosScriptEqOpNew -> annotateNewEqualityOps(variant, element, annotationWrapper)
            is CaosScriptEnumSceneryStatement -> annotateSceneryEnum(variant, element, annotationWrapper)
            is CaosScriptEnumNextStatement -> annotateBadEnumStatement(variant, element, annotationWrapper)
            is CaosScriptElseIfStatement -> annotateElseIfStatement(variant, element, annotationWrapper)
            is CaosScriptCRetn -> annotateRetnCommand(variant, element, annotationWrapper)
            is CaosScriptExpressionList -> annotateExpressionList(element, annotationWrapper)
            is CaosScriptIsCommandToken -> annotateNotAvailable(element, annotationWrapper)
            is CaosScriptVarToken -> annotateVarToken(variant, element, annotationWrapper)
            is PsiComment -> annotateComment(variant, element, annotationWrapper)
            is CaosScriptIncomplete -> simpleError(element, "invalid element", annotationWrapper)
        }
    }


    private fun annotateElseIfStatement(variant: String, element: CaosScriptElseIfStatement, annotationWrapper: AnnotationHolderWrapper) {
        if (!(variant != "C1" || variant != "C2"))
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
        if (variant != "C1" && variant != "C2")
            return
        annotationWrapper.newErrorAnnotation("Comments are not allowed in version less than CV")
                .range(element)
                .create()
    }

    private fun annotateByteStringR(byteStringR: CaosScriptByteStringR, annotationWrapper: AnnotationHolderWrapper) {

    }

    private fun annotateNewEqualityOps(variant: String, element: CaosScriptEqOpNew, annotationWrapper: AnnotationHolderWrapper) {
        if (variant !in VARIANT_OLD)
            return
        annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.syntax-error-annotator.invalid_eq_operator"))
                .range(element)
                .withFix(TransposeEqOp(element))
                .create()
    }


    private fun annotateSceneryEnum(variant: String, element: CaosScriptEnumSceneryStatement, annotationWrapper: AnnotationHolderWrapper) {
        if (variant == "C2")
            return
        annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.escn-only-on-c2-error-message"))
                .range(element.escnHeader.cEscn)
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

    private fun annotateBadEnumStatement(variant: String, element: CaosScriptEnumNextStatement, annotationWrapper: AnnotationHolderWrapper) {
        val header = element.emumHeaderCommand.cEnum
                ?: return
        if (header.kEnum != null)
            return
        if (!(variant == "C2" || variant == "C1"))
            return
        header.kEpas?.let {
            annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.bad-enum-error-message", it.text.toUpperCase()))
                    .range(it)
                    .create()
            return
        }
        if (variant != "C1")
            return
        val badElement = header.kEsee ?: header.kEtch ?: header.kEpas
        ?: return // return if enum type is ENUM
        annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.bad-enum-error-message", badElement.text.toUpperCase()))
                .range(badElement)
                .create()
    }

    private fun annotateExpressionList(element: CaosScriptExpressionList, annotationWrapper: AnnotationHolderWrapper) {
        annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.stray-expressions-are-not-allowed"))
                .range(element)
                .create()
    }

    private fun annotateNotAvailable(element: CaosScriptIsCommandToken, annotationWrapper: AnnotationHolderWrapper) {
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

        val variant = element.variant.nullIfEmpty()
                ?: return
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
        annotationWrapper
                .newErrorAnnotation(error)
                .range(element)
                .create()
    }

    private fun getVariantString(variantsIn: List<String>): String {
        val variants = sortVariants(variantsIn)
        val numVariants = variants.size
        return when {
            numVariants == variants.intersect(listOf("C3", "DS")).size -> "C3+"
            numVariants == variants.intersect(listOf("CV", "C3", "DS")).size -> "CV+"
            numVariants == variants.intersect(listOf("C2", "CV", "C3", "DS")).size -> "C2+"
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

    private fun annotateVarToken(variantIn: String, element: CaosScriptVarToken, annotationWrapper: AnnotationHolderWrapper) {
        val variant = variantIn.toUpperCase()
        val variants: String = if (element.varX != null) {
            if (variant != "C1" && variant != "C2")
                "C1,C2"
            else
                return
        } else if (element.vaXx != null) {
            if (variant == "C1")
                "C2+"
            else
                return
        } else if (element.obvX != null) {
            if (variant != "C1" && variant != "C2") {
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
            if (variant == "C1" || variant == "C2")
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
        private val VARIANT_OLD = listOf("C1", "C2")
        private val IS_COMMA_OR_SPACE = "[\\s,]+".toRegex()
    }

}