package com.badahori.creatures.plugins.intellij.agenteering.caos.annotators

import com.badahori.creatures.plugins.intellij.agenteering.att.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosScriptInferenceUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.highlighting.CaosScriptSyntaxHighlighter
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle.message
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Cob
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommandType.LVALUE
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant.UNKNOWN
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import kotlin.math.abs
import kotlin.math.floor

class CaosScriptSyntaxErrorAnnotator : Annotator, DumbAware {

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
            is CaosScriptEqOpOld -> annotateOldEqualityOps(variant, element, holder)
            is CaosScriptEqOpNew -> annotateNewEqualityOps(variant, element, holder)
            is CaosScriptEqualityExpressionPlus -> annotateEqualityExpressionPlus(variant, element, holder)
            is CaosScriptElseIfStatement -> annotateElseIfStatement(variant, element, holder)
            is CaosScriptCRetn -> annotateRetnCommand(element, holder)
            is CaosScriptToken -> annotateToken(element, holder)
            is CaosScriptQuoteStringLiteral -> annotateDoubleQuoteString(variant, element, holder)
            is CaosScriptC1String -> annotateC1String(variant, element, holder)
            is CaosScriptIsCommandToken -> annotateNotAvailable(variant, element, holder)
            is CaosScriptVarToken -> annotateVarToken(variant, element, holder)
            is CaosScriptNumber -> annotateNumber(variant, element, holder)
            is CaosScriptErrorRvalue -> holder
                .newErrorAnnotation("Unrecognized rvalue")
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
                DeleteElementFix("Delete extraneous rvalue", element)
            )
            is CaosScriptSwiftEscapeLiteral -> {

                val allowSwift =
                    PsiTreeUtil.collectElementsOfType(element.containingFile, CaosScriptAtDirectiveComment::class.java)
                        .any { comment ->
                            swiftRegex.matches(comment.text.trim())
                        }
                if (!allowSwift) {
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
                    "CAOS2Cob property"
                else
                    "CAOS2Pray tag"
                simpleError(
                    element,
                    message("caos.annotator.syntax-error-annotator.too-many-tag-values", directiveType),
                    holder
                )
            }
        }
    }

    /**
     * Further annotates an error element as it can possibly be an LValue used as a command
     */
    private fun annotateErrorElement(variant: CaosVariant, element: PsiElement, holder: AnnotationHolder) {
        val command = element.text
        val commandUpperCase = element.text.toUpperCase()
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
                .withFix(CaosScriptInsertBeforeFix("Insert '$setv' before $command", setv, element))
                .create()
        }
    }

    /**
     * Annotates a number if it is a float used outside C2+
     */
    private fun annotateNumber(variant: CaosVariant, element: CaosScriptNumber, holder: AnnotationHolder) {
        if (variant != CaosVariant.C1 || element.float == null)
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
        holder: AnnotationHolder
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
                holder.newErrorAnnotation("Tokens must be 4 characters long")
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
        wrapper: AnnotationHolder
    ) {
        if (variant.isNotOld || quoteStringLiteral.parent is CaosScriptCaos2Value)
            return
        wrapper.newErrorAnnotation(message("caos.annotator.syntax-error-annotator.out-of-variant-quote-string"))
            .range(quoteStringLiteral)
            .withFix(CaosScriptFixQuoteType(quoteStringLiteral, '[', ']'))
            .create()
    }

    /**
     * Annotates a C1 string as invalid when assigned or used in CV+
     */
    private fun annotateC1String(variant: CaosVariant, element: CaosScriptC1String, wrapper: AnnotationHolder) {
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
            } else if (element.getParentOfType(CaosScriptArgument::class.java)?.parent is CaosScriptCAssignment) {
                wrapper.newErrorAnnotation(
                    message(
                        "caos.annotator.syntax-error-annotator.variable-string-assignments-not-allowed",
                        variant
                    )
                )
                    .range(element)
                    .create()
            }
            return
        }
        wrapper.newErrorAnnotation(message("caos.annotator.syntax-error-annotator.out-of-variant-c1-string"))
            .range(element)
            .withFix(CaosScriptFixQuoteType(element, '"'))
            .create()
    }


    /**
     * Annotates ELIF as invalid in C1/C2
     */
    private fun annotateElseIfStatement(
        variant: CaosVariant,
        element: CaosScriptElseIfStatement,
        holder: AnnotationHolder
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
        vararg fixes: IntentionAction
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
        if (element.text.toLowerCase().let { it != "bt" && it != "bf" }) {
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

    private fun annotateVarToken(variant: CaosVariant, element: CaosScriptVarToken, holder: AnnotationHolder) {
        if (variant.isOld) {
            if (element.parent?.parent is CaosScriptEqualityExpression) {
                // TODO: Is this check really necessary, in older variants
                // a string cannot be assigned to a string anyways,
                // so a variable is guaranteed to have an int (either literal or agent id)
                val type = if (DumbService.isDumb(element.project))
                    listOf(CaosExpressionValueType.VARIABLE)
                else
                    CaosScriptInferenceUtil.getInferredType(element)
                if (type != null && type.all { it.isStringType || !it.isByteStringLike }) {
                    holder.newErrorAnnotation(
                        message(
                            "caos.annotator.syntax-error-annotator.string-comparisons-not-allowed",
                            variant
                        )
                    )
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

        val error = message("caos.annotator.syntax-error-annotator.invalid-var-type-for-variant", varName, variants)
        holder
            .newErrorAnnotation(error)
            .range(element)
            .create()
    }

    private fun annotateErrorCommand(
        variant: CaosVariant,
        errorCommand: CaosScriptErrorCommand,
        holder: AnnotationHolder
    ) {
        val rawTokens = errorCommand
            .text
            .toUpperCase()
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
            holder: AnnotationHolder
        ) {
            // Colorize element as token
            if (element.isOrHasParentOfType(CaosScriptRKwNone::class.java) && element.hasParentOfType(
                    CaosScriptTokenRvalue::class.java
                )
            ) {
                holder.colorize(element, CaosScriptSyntaxHighlighter.TOKEN)
                return
            }
            val commandText = element.commandString.toUpperCase()

            if (element is CaosScriptCKwInvalidLoopTerminator) {
                annotateInvalidLoopTerminator(element, holder)
            }
            // Get error annotation, run create if needed
            getErrorCommandAnnotation(variant, element, commandText, holder)?.create()
        }
    }

}

