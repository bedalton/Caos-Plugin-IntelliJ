package com.badahori.creatures.plugins.intellij.agenteering.caos.annotators

import com.badahori.creatures.plugins.intellij.agenteering.caos.completion.GenerateBitFlagIntegerIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.completion.GenerateClasIntegerAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptCollapseNewLineIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptExpandCommasIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CollapseChar
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.next
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.previous
import com.badahori.creatures.plugins.intellij.agenteering.utils.toIntSafe
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement

/**
 * Adds helper actions to command elements
 */
class CaosScriptHelperActionAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when(element) {
            is CaosScriptCommandCall -> {
                var child: PsiElement? = element.firstChild
                if (child != null && child !is CaosScriptCommandLike) {
                    child = child.firstChild
                }
                (child as? CaosScriptCAssignment)?.let {
                    val variant = element.variant ?: CaosVariant.UNKNOWN
                    annotateAssignment(variant, it, holder)
                }
                //Annotate
                addExpandCollapseLinesActions(element, holder)
            }
            is CaosScriptRvalue -> annotateExpectsValue(element, holder)
            is CaosScriptSpaceLikeOrNewline -> expandCollapseOnSpaceOrNewline(element, holder)
        }
    }

    private fun annotateExpectsValue(argument:CaosScriptRvalue, holder: AnnotationHolder) {
        val index = argument.index
        val command = argument.getParentOfType(CaosScriptCommandElement::class.java)
                ?: return
        val token = command
                .commandString
                ?.toUpperCase()
                ?: return
        when {
            token == "SETV" && index == 1 -> {
                val previousToken = command.arguments.getOrNull(0)?.text?.toUpperCase()
                        ?: return
                when (previousToken) {
                    "CLAS" -> AnnotationHolderWrapper(holder)
                            .newInfoAnnotation("")
                            .range(argument)
                            .withFix(GenerateClasIntegerAction(argument))
                            .create()
                }
            }
        }
    }

    private fun addExpandCollapseLinesActions(element: CaosScriptCommandLike, holder: AnnotationHolder) {
        val next = element.next

        val wrapper = AnnotationHolderWrapper(holder)
        // If there are only commas next, simply allow for expansion
        if (next != null && COMMAS_ONLY_REGEX.matches(next.text)) {
            wrapper
                    .newInfoAnnotation(null)
                    .range(element)
                    .withFix(CaosScriptExpandCommasIntentionAction)
                    .create()
            // if next is a newline element, allow collapse with commas or spaces
        } else if (next is CaosScriptSpaceLikeOrNewline && next.textContains('\n')) {
            wrapper
                    .newInfoAnnotation(null)
                    .range(element)
                    .withFixes(
                            CaosScriptCollapseNewLineIntentionAction(CollapseChar.COMMA),
                            CaosScriptCollapseNewLineIntentionAction(CollapseChar.SPACE)
                    )
                    .create()
        } else {
            // Next does not include commas or newlines
            // Always allow expand lines
            val fixes:MutableList<IntentionAction> = mutableListOf(CaosScriptExpandCommasIntentionAction)
            // If there are newlines in file, also allow collapsing of lines
            if (element.containingFile.text.contains("\n")) {
                fixes.add(CaosScriptCollapseNewLineIntentionAction(CollapseChar.COMMA))
                fixes.add(CaosScriptCollapseNewLineIntentionAction(CollapseChar.SPACE))
            }
            wrapper
                    .newInfoAnnotation(null)
                    .range(element)
                    .withFixes(*fixes.toTypedArray())
                    .create()
        }
    }

    /**
     *
     */
    private fun expandCollapseOnSpaceOrNewline(element: CaosScriptSpaceLikeOrNewline, holder: AnnotationHolder) {
        val fixes = mutableListOf<IntentionAction>()
        val isAtEnd = element.next == null
        if (element.text == "," || element.text == " " || isAtEnd) {
            fixes.add(CaosScriptExpandCommasIntentionAction)
        }
        if (element.text.contains("\n") || isAtEnd) {
            fixes.add(CaosScriptCollapseNewLineIntentionAction(CollapseChar.COMMA))
            fixes.add(CaosScriptCollapseNewLineIntentionAction(CollapseChar.SPACE))
        }
        AnnotationHolderWrapper(holder)
                .newInfoAnnotation(null)
                .range(element)
                .withFixes(*fixes.toTypedArray())
                .create()
    }

    private fun annotateAssignment(variant: CaosVariant, assignment: CaosScriptCAssignment, holder: AnnotationHolder) {
        val commandDefinition = assignment.lvalue?.commandDefinition
                ?: return
        val valuesList = commandDefinition.returnValuesList[variant]
                ?: return
        for (addTo in assignment.arguments) {
            if (addTo is CaosScriptLvalue || (addTo as? CaosScriptRvalue)?.varToken != null)
                continue
            val currentValue= addTo.text.toIntSafe() ?: 0
            val range = if (addTo.text.isNotEmpty())
                addTo
            else
                addTo.next ?: addTo.previous ?: return
            val bitFlagsGenerator = GenerateBitFlagIntegerIntentionAction(
                    addTo,
                    valuesList.name,
                    valuesList.values,
                    currentValue
            )
            AnnotationHolderWrapper(holder)
                    .newInfoAnnotation(null)
                    .range(range)
                    .withFix(bitFlagsGenerator)
                    .create()
        }
    }

    companion object {
        private val COMMAS_ONLY_REGEX = "[,]+".toRegex()
    }
}