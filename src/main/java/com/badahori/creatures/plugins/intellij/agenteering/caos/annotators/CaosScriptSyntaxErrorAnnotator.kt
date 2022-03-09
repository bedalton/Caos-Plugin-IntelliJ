package com.badahori.creatures.plugins.intellij.agenteering.caos.annotators

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Cob
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Pray
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.highlighting.CaosScriptSyntaxHighlighter
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle.message
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Cob
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommandType.LVALUE
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant.C1
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant.UNKNOWN
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.parameter
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import kotlin.math.abs
import kotlin.math.floor

class CaosScriptSyntaxErrorAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element.isOrHasParentOfType(CaosDefCompositeElement::class.java))
            return
        if (!element.isPhysical)
            return

        // Get variant for element's parent file
        val variant = (element.containingFile as? CaosScriptFile)?.variant
        // Cannot validate without a variant. Return early if null
            ?: return
        // Annotate element based on type
        if (element is PsiComment)
            return
        when (element) {
            is PsiErrorElement -> holder.newErrorAnnotation(element.errorDescription)
                .range(element)
                .afterEndOfLine()
                .create()
            is CaosScriptEqOpOld -> annotateOldEqualityOps(variant, element, holder)
            is CaosScriptEqOpNew -> annotateNewEqualityOps(variant, element, holder)
            is CaosScriptEqualityExpressionPlus -> annotateEqualityExpressionPlus(variant, element, holder)
            is CaosScriptElseIfStatement -> annotateElseIfStatement(variant, element, holder)
            is CaosScriptCRetn -> annotateRetnCommand(element, holder)
            is CaosScriptToken -> annotateToken(element, holder)
            is CaosScriptQuoteStringLiteral -> annotateDoubleQuoteString(variant, element, holder)
            is CaosScriptC1String -> annotateC1String(variant, element, holder)
            is CaosScriptByteString -> annotateByteString(variant, element, holder)
            is CaosScriptAnimationString -> annotateByteString(variant, element, holder)
            is CaosScriptIsCommandToken -> annotateNotAvailable(variant, element, holder)
            is CaosScriptNumber -> annotateNumber(variant, element, holder)
            is CaosScriptErrorRvalue -> holder
                .newErrorAnnotation(message("caos.annotator.syntax-error-annotator.invalid-rvalue"))
                .range(element)
                .create()
            is CaosScriptErrorCommand -> annotateErrorCommand(variant, element, holder)
            is CaosScriptCharacter -> {
                val charChar = element.charChar?.text ?: ""
                val charLength = charChar.length
                val isEscapedChar = charChar.getOrNull(0) == '\\'
                val validTextLength = if (isEscapedChar) 2 else 1
                if (charLength == validTextLength) {
                    if (isEscapedChar) {
                        if (charChar[1] !in VALID_ESCAPE_CHARS) {
                            val message = message("caos.annotator.syntax-error-annotator.char-escape-invalid")
                            holder.newWeakWarningAnnotation(message)
                                .range(element.charChar!!)
                                .textAttributes(CaosScriptSyntaxHighlighter.INVALID_STRING_ESCAPE)
                                .create()
                        } else {
                            holder.colorize(element.charChar!!, DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE)
                        }
                    }
                    return
                }
                if (isEscapedChar && charLength == 1) {
                    simpleError(
                        element.charChar ?: element,
                        message("caos.annotator.syntax-error-annotator.char-incomplete-escape"),
                        holder
                    )
                }
                simpleError(
                    element.charChar ?: element,
                    message("caos.annotator.syntax-error-annotator.char-value-too-long"),
                    holder
                )
            }
            is CaosScriptErrorLvalue -> {
                val variableType = when {
                    element.number != null -> "number"
                    element.quoteStringLiteral != null -> (element.parent?.parent as? CaosScriptNamedGameVar)?.varType?.token
                        ?: "string"
                    element.c1String != null -> "string"
                    element.animationString != null -> "animation"
                    element.byteString != null -> "byte-string"
                    else -> "literal"
                }
                simpleError(
                    element,
                    message("caos.annotator.syntax-error-annotator.variable-expected", variableType),
                    holder
                )
            }
            is CaosScriptIncomplete -> {
                if (element.hasParentOfType(CaosScriptSubroutineName::class.java)) {
                    return
                }
                simpleError(element, message("caos.annotator.syntax-error-annotator.invalid-element"), holder)
            }
            //is CaosScriptCAssignment -> annotateSetvCompoundLvalue(variant, element, holder)
            is CaosScriptOutOfCommandLiteral -> simpleError(
                element,
                message("caos.annotator.syntax-error-annotator.out-of-command-literal"),
                holder,
                DeleteElementFix(message("caos.fixes.delete-extra-rvalue"), element)
            )
            is CaosScriptSwiftEscapeLiteral -> {
                if (!allowSwift(element.containingFile)) {
                    simpleError(element, message("caos.annotator.syntax-error-annotator.invalid-element"), holder)
                } else if (element.textLength < 4) {
                    simpleError(element, message("caos.annotator.syntax-error-annotator.swift-value-empty"), holder)
                }
            }
            is CaosScriptJsElement -> {
                val allowJs =
                    PsiTreeUtil.collectElementsOfType(element.containingFile, CaosScriptAtDirectiveComment::class.java)
                        .any { comment ->
                            allowJsRegex.matches(comment.text.trim())
                        }
                if (!allowJs) {
                    simpleError(element, message("caos.annotator.syntax-error-annotator.invalid-element"), holder)
                } else if (element.textLength < 4) {
                    simpleError(element, message("caos.annotator.syntax-error-annotator.js-value-empty"), holder)
                }
            }
            is LeafPsiElement -> {
                if (element.parent is PsiErrorElement)
                    annotateErrorElement(variant, element, holder)
            }
            is CaosScriptCaos2CommentErrorValue -> {
                val isCaos2Cob = (element.containingFile as? CaosScriptFile)?.isCaos2Cob ?: false
                val directiveType = if (isCaos2Cob)
                    message("caos.general.property-type", CAOS2Cob)
                else
                    message("caos.general.tag-type", CAOS2Pray)
                simpleError(
                    element,
                    message("caos.annotator.syntax-error-annotator.too-many-tag-values", directiveType),
                    holder
                )
            }
            is CaosScriptFamily -> annotateClassifierArgument(element.rvalue, "Family", holder)
            is CaosScriptGenus -> annotateClassifierArgument(element.rvalue, "Genus", holder)
            is CaosScriptSpecies -> annotateClassifierArgument(element.rvalue, "Species", holder)
        }
    }

    /**
     * Further annotates an error element as it can possibly be an LValue used as a command
     */
    private fun annotateErrorElement(variant: CaosVariant, element: PsiElement, holder: AnnotationHolder) {
        val command = element.text
        val commandUpperCase = element.text.uppercase()
        val isLvalue =
            commandUpperCase == "CLAS" || commandUpperCase == "CLS2" || CaosLibs[variant][LVALUE][command] != null
        if (isLvalue) {
            val setv = "SETV".matchCase(command)
            holder
                .newErrorAnnotation(
                    message(
                        "caos.annotator.syntax-error-annotator.clas-is-lvalue-and-requires-setv",
                        commandUpperCase
                    )
                )
                .range(element)
                .withFix(CaosScriptInsertBeforeFix(message("caos.fixes.insert-before", setv, command), setv, element))
                .create()
        }
    }

    /**
     * Annotates a number if it is a float used outside C2+
     */
    private fun annotateNumber(variant: CaosVariant, element: CaosScriptNumber, holder: AnnotationHolder) {
        if (variant != C1 || element.float == null)
            return
        val floatValue = element.text.toFloat()
        var builder =
            holder.newErrorAnnotation(message("caos.annotator.syntax-error-annotator.float-value-not-allowed-in-variant"))
                .range(element)
                .withFix(CaosScriptRoundNumberFix(element, floatValue, true))
        if (abs(floatValue - floor(floatValue)) > 0.00001)
            builder = builder.withFix(CaosScriptRoundNumberFix(element, floatValue, false))
        builder.create()
    }

    private fun annotateEqualityExpressionPlus(
        variant: CaosVariant,
        element: CaosScriptEqualityExpressionPlus,
        holder: AnnotationHolder,
    ) {
        if (variant.isNotOld)
            return
        holder.newErrorAnnotation(message("caos.annotator.syntax-error-annotator.compound-equality-operator-not-allowed"))
            .range(element)
            .create()
    }

    /**
     * Annotates a token as unexpected if not found in proper Token parent
     */
    private fun annotateToken(element: CaosScriptToken, holder: AnnotationHolder) {
        if (element.hasParentOfType(CaosScriptTokenRvalue::class.java)) {
            if (element.variant?.isOld.orFalse() && element.textLength != 4) {
                holder.newErrorAnnotation(message("caos.annotator.syntax-error-annotator.invalid-c1e-token"))
                    .range(element)
                    .create()
            }
            return
        }
        holder.newErrorAnnotation("Unexpected token")
            .range(element)
            .create()
    }

    /**
     * Annotates a string in double quotes as out of variant in C1/C2
     */
    private fun annotateDoubleQuoteString(
        variant: CaosVariant,
        quoteStringLiteral: CaosScriptQuoteStringLiteral,
        wrapper: AnnotationHolder,
    ) {
        if (variant.isNotOld || quoteStringLiteral.parent is CaosScriptCaos2Value)
            return

        if (quoteStringLiteral.parent?.parent !is CaosScriptEqualityExpressionPrime) {
            wrapper
                .newErrorAnnotation(
                    message(
                        "caos.annotator.syntax-error-annotator.string-comparisons-not-allowed",
                        variant
                    )
                )
                .range(quoteStringLiteral)
                .create()
        }
        wrapper.newErrorAnnotation(message("caos.annotator.syntax-error-annotator.out-of-variant-quote-string"))
            .range(quoteStringLiteral)
            .withFix(CaosScriptFixQuoteType(quoteStringLiteral, '[', ']'))
            .create()
    }

    /**
     * Annotates a C1 string as invalid when assigned or used in CV+
     */
    private fun annotateC1String(variant: CaosVariant, element: PsiElement, wrapper: AnnotationHolder) {
        if (variant.isOld) {
            if (element.parent?.parent is CaosScriptEqualityExpressionPrime) {
                wrapper.newErrorAnnotation(
                    message(
                        "caos.annotator.syntax-error-annotator.string-comparisons-not-allowed",
                        variant
                    )
                )
                    .range(element)
                    .create()
            } else {
                // String is used as argument in C1e
                val argument = (element.parent as? CaosScriptArgument)
                if (argument?.parent is CaosScriptCAssignment) {
                    wrapper
                        .newErrorAnnotation(
                            message(
                                "caos.annotator.syntax-error-annotator.variable-string-assignments-not-allowed",
                                variant
                            )
                        )
                        .range(element)
                        .create()
                    return
                }
                val parameter = argument?.parameter
                if (parameter?.type != CaosExpressionValueType.C1_STRING && parameter?.type != CaosExpressionValueType.STRING) {
                    wrapper
                        .newErrorAnnotation(
                            message(
                                "caos.annotator.syntax-error-annotator.strings-are-not-rvalues",
                                variant
                            )
                        )
                        .range(element)
                        .create()
                }
            }
        } else {
            wrapper.newErrorAnnotation(message("caos.annotator.syntax-error-annotator.out-of-variant-c1-string"))
                .range(element)
                .withFix(CaosScriptFixQuoteType(element, '"'))
                .create()
        }
    }


    /**
     * Annotates ELIF as invalid in C1/C2
     */
    private fun annotateElseIfStatement(
        variant: CaosVariant,
        element: CaosScriptElseIfStatement,
        holder: AnnotationHolder,
    ) {
        if (variant.isNotOld)
            return
        holder.newErrorAnnotation(message("caos.annotator.syntax-error-annotator.elif-not-available"))
            .range(element.cElif)
            .create()
    }

    /**
     * Annotates an error message on an element
     */
    @Suppress("SameParameterValue")
    private fun simpleError(element: PsiElement, message: String, holder: AnnotationHolder) {
        holder.newErrorAnnotation(message)
            .range(element)
            .create()
    }

    /**
     * Annotates an error message on an element
     */
    @Suppress("SameParameterValue")
    private fun simpleError(
        element: PsiElement,
        message: String,
        holder: AnnotationHolder,
        vararg fixes: IntentionAction,
    ) {
        holder.newErrorAnnotation(message)
            .withFixes(*fixes)
            .range(element)
            .create()
    }


    private fun annotateNewEqualityOps(variant: CaosVariant, element: CaosScriptEqOpNew, holder: AnnotationHolder) {
        if (variant.isNotOld || variant == UNKNOWN)
            return
        holder.newErrorAnnotation(message("caos.annotator.syntax-error-annotator.invalid_eq_operator"))
            .range(element)
            .withFix(TransposeEqOp(element))
            .create()
    }

    private fun annotateOldEqualityOps(variant: CaosVariant, element: CaosScriptEqOpOld, holder: AnnotationHolder) {
        if (variant.isOld || variant == UNKNOWN)
            return
        if (element.text.lowercase().let { it != "bt" && it != "bf" }) {
            return
        }
        holder.newErrorAnnotation(message("caos.annotator.syntax-error-annotator.invalid_old-eq_operator"))
            .range(element)
            .create()
    }

    private fun annotateRetnCommand(element: CaosScriptCRetn, holder: AnnotationHolder) {
        if (!element.hasParentOfType(com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSubroutine::class.java)) {
            holder.newErrorAnnotation(message("caos.annotator.syntax-error-annotator.retn-used-outside-of-subr"))
                .range(element)
                .create()
        }
        if (!element.hasParentOfType(CaosScriptNoJump::class.java))
            return
        holder.newErrorAnnotation(message("caos.annotator.syntax-error-annotator.loop-should-not-be-jumped-out-of"))
            .range(element.textRange)
            .create()
    }

    private fun annotateErrorCommand(
        variant: CaosVariant,
        errorCommand: CaosScriptErrorCommand,
        holder: AnnotationHolder,
    ) {
        val rawTokens = errorCommand
            .text
            .uppercase()
            .split(WHITESPACE, 3)
        // Get error as possible error annotation
        val errorAnnotation = rawTokens
            .let { tokens ->
                if (tokens.size > 3)
                    listOf("${tokens[0]} ${tokens[1]} ${tokens[2]}", "${tokens[0]} ${tokens[1]}", tokens[0])
                if (tokens.size > 2)
                    listOf("${tokens[0]} ${tokens[1]}", tokens[0])
                else
                    listOf(tokens[0])
            }
            .mapNotNull { commandToken ->
                getErrorCommandAnnotation(variant, errorCommand, commandToken, holder)
            }
/*
        // Check for length less than 2, meaning one matched.
        // If one matches, that means the command is in the definitions file
        // but not in the BNF parser grammar.
        // TODO should we throw error, or simply return without annotating.
        if (errorAnnotation.size < rawTokens.size) {
            throw Exception("Command found in definitions for element: ${errorCommand.text}, but BNF grammar does not reflect this.")
        }*/
        errorAnnotation.last().create()
    }


    companion object {

        val VALID_ESCAPE_CHARS = listOf('r', 'b', 'n', 't', 'f', '\\', '\'', '"', '/')
        val swiftRegex = "^[*][*]+\\s*([Ff][Oo][Rr]\\s*)?[Ss][Ww][Ii][Ff][Tt]\\s*".toRegex(RegexOption.IGNORE_CASE)
        val allowJsRegex = "^[*][*]+\\s*FOR\\s*JS\\s*|^[*]{2}VARIANT[^\n]*".toRegex(RegexOption.IGNORE_CASE)
        internal fun annotateNotAvailable(
            variant: CaosVariant,
            element: CaosScriptIsCommandToken,
            holder: AnnotationHolder,
        ) {
            // Colorize element as token
            if (element.isOrHasParentOfType(CaosScriptRKwNone::class.java) && element.hasParentOfType(
                    CaosScriptTokenRvalue::class.java
                )
            ) {
                holder.colorize(element, CaosScriptSyntaxHighlighter.TOKEN)
                return
            }
            val commandText = element.commandString.uppercase()

            if (element is CaosScriptCKwInvalidLoopTerminator) {
                annotateInvalidLoopTerminator(element, holder)
            }
            // Get error annotation, run create if needed
            getErrorCommandAnnotation(variant, element, commandText, holder)?.create()
        }
    }

    private fun annotateClassifierArgument(element: CaosScriptRvalue?, partName: String, holder: AnnotationHolder) {
        if (element == null) {
            return
        }

        if (element.parent?.parent?.parent is CaosScriptEventScript) {
            annotateEventScriptArgument(element, partName, holder)
            return
        }


    }

    private fun annotateEventScriptArgument(element: CaosScriptRvalue, partName: String, holder: AnnotationHolder) {
        if (element.isInt) {
            return
        }

        if (element.swiftEscapeLiteral != null && allowSwift(element)) {
            return
        }

        holder.newErrorAnnotation(message("caos.annotator.syntax-error-annotator.classifier-expects-integer-literal",
            partName.lowercase()))
            .range(element)
            .create()
    }

    private fun allowSwift(element: PsiElement): Boolean {
        return PsiTreeUtil.collectElementsOfType(element.containingFile, CaosScriptAtDirectiveComment::class.java)
            .any { comment ->
                swiftRegex.matches(comment.text.trim())
            }
    }

    private fun annotateByteString(variant: CaosVariant, element: PsiElement, holder: AnnotationHolder) {
        if (element.parent?.parent is CaosScriptEqualityExpressionPrime) {
            holder
                .newErrorAnnotation(
                    message(
                        "caos.annotator.syntax-error-annotator.string-comparisons-not-allowed",
                        variant
                    )
                )
                .range(element)
                .create()
            return
        }
        (element.parent as? CaosScriptRvalue)?.parameter?.let { parameter ->
            // IF not animation or byte string
            if (parameter.type != CaosExpressionValueType.BYTE_STRING && parameter.type != CaosExpressionValueType.ANIMATION) {
                annotateC1String(variant, element, holder)
            } else if (variant == C1) {
                if (element is CaosScriptByteString) {
                    if (element.byteStringPoseElementList.size > 1) {
                        holder
                            .newErrorAnnotation(
                                message(
                                    "caos.annotator.syntax-error-annotator.c1e-spaces-in-byte-string",
                                    variant
                                )
                            )
                            .range(element)
                            .create()
                    }
                }
            }
        }
    }

}

