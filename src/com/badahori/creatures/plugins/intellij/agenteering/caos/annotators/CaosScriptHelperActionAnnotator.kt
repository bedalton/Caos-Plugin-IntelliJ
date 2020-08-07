package com.badahori.creatures.plugins.intellij.agenteering.caos.annotators

import com.badahori.creatures.plugins.intellij.agenteering.caos.completion.GenerateBitFlagIntegerIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefValuesListDefinitionElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.isVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptCollapseNewLineIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptExpandCommasIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CollapseChar
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.next
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.toIntSafe
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.variant
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement

/**
 * Adds helper actions to command elements
 */
class CaosScriptHelperActionAnnotator : Annotator {
    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        val wrapper = AnnotationHolderWrapper(annotationHolder)
        val variant = element.containingFile.module?.variant ?: CaosVariant.UNKNOWN
        when {
            element is CaosScriptCommandLike -> {
                // Annnotate assignment commands
                (element as? CaosScriptCAssignment)?.let {
                    annotateAssignment(variant, it, wrapper)
                }
                //Anotatte
                addExpandCollapseLinesActions(element, wrapper)
            }
            // Annotate SpaceLike with expand collapse
            element is CaosScriptSpaceLikeOrNewline -> {
                expandCollapseOnSpaceOrNewline(element, wrapper)
            }
            else -> {
            }
        }
    }

    private fun addExpandCollapseLinesActions(element: CaosScriptCommandLike, wrapper: AnnotationHolderWrapper) {
        val next = element.next
        if (next == null) {
            var intention = wrapper.newInfoAnnotation(null)
                    .range(element)
                    .withFix(CaosScriptExpandCommasIntentionAction)
            if (element.containingFile.text.contains("\n")) {
                intention = intention
                        .withFix(CaosScriptCollapseNewLineIntentionAction(CollapseChar.COMMA))
                        .withFix(CaosScriptCollapseNewLineIntentionAction(CollapseChar.SPACE))
            }
            intention.create()
            return
        }

        // If there are only commas next, simply allow for expansion
        if (COMMAS_ONLY_REGEX.matches(next.text)) {
            wrapper.newInfoAnnotation(null)
                    .range(element)
                    .withFix(CaosScriptExpandCommasIntentionAction)
                    .create()
        // if next is a newline element, allow collapse with commas or spaces
        } else if (next is CaosScriptSpaceLikeOrNewline && next.textContains('\n')) {
            wrapper.newInfoAnnotation(null)
                    .range(element)
                    .withFix(CaosScriptCollapseNewLineIntentionAction(CollapseChar.COMMA))
                    .withFix(CaosScriptCollapseNewLineIntentionAction(CollapseChar.SPACE))
                    .create()
        } else {
            // Next does not include commas or newlines
            // Always allow expand lines
            var intention = wrapper.newInfoAnnotation(null)
                    .range(element)
                    .withFix(CaosScriptExpandCommasIntentionAction)
            // If there are newlines in file, also allow collapsing of lines
            if (element.containingFile.text.contains("\n")) {
                intention = intention
                        .withFix(CaosScriptCollapseNewLineIntentionAction(CollapseChar.COMMA))
                        .withFix(CaosScriptCollapseNewLineIntentionAction(CollapseChar.SPACE))
            }
            intention.create()
            return
        }
    }

    /**
     *
     */
    private fun expandCollapseOnSpaceOrNewline(element:CaosScriptSpaceLikeOrNewline, wrapper: AnnotationHolderWrapper) {
        if (element.text == "," || element.text == " ") {
            wrapper.newInfoAnnotation(null)
                    .range(element)
                    .withFix(CaosScriptExpandCommasIntentionAction)
                    .create()
        }
        if (element.text.contains("\n")) {
            wrapper.newInfoAnnotation(null)
                    .range(element)
                    .withFix(CaosScriptCollapseNewLineIntentionAction(CollapseChar.COMMA))
                    .withFix(CaosScriptCollapseNewLineIntentionAction(CollapseChar.SPACE))
                    .create()
        }
    }

    private fun annotateAssignment(variant:CaosVariant, assignment:CaosScriptCAssignment, wrapper: AnnotationHolderWrapper) {
        val commandString = assignment.lvalue?.commandStringUpper.nullIfEmpty()
                ?: return
        val project = assignment.project
        val addTo = assignment.lastChild
        val currentValue = if (addTo is CaosScriptExpectsValueOfType) {
            addTo.text.toIntSafe() ?: 0
        } else {
            0
        }
        val valuesList = CaosDefCommandElementsByNameIndex
                .Instance[commandString, project]
                .filter {
                    it.isVariant(variant)
                }
                .mapNotNull {
                    it.returnTypeStruct?.type?.valuesList
                }
                .firstOrNull()
                ?: return
        val valuesListWithBitFlags = CaosDefValuesListDefinitionElementsByNameIndex
                .Instance[valuesList, project]
                .firstOrNull {
                    it.isBitflags && it.isVariant(variant)
                }
                ?: return
        wrapper.newInfoAnnotation(null)
                .range(addTo)
                .withFix(GenerateBitFlagIntegerIntentionAction(addTo, valuesListWithBitFlags.typeName, valuesListWithBitFlags.keys, currentValue))
                .create()
    }

    companion object {
        private val COMMAS_ONLY_REGEX = "[,]+".toRegex()
    }
}