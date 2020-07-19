package com.badahori.creatures.plugins.intellij.agenteering.caos.annotators

import com.badahori.creatures.plugins.intellij.agenteering.caos.completion.GenerateBitFlagIntegerIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefTypeDefinitionElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.isVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptCollapseNewLineIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptExpandCommasIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CollapseChar
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.next
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.toIntSafe
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.variant
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement

class CaosScriptHelperActionAnnotator : Annotator {
    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        val wrapper = AnnotationHolderWrapper(annotationHolder)
        val variant = element.containingFile.module?.variant ?: CaosVariant.UNKNOWN
        when {
            element is CaosScriptCommandLike -> {
                (element as? CaosScriptCAssignment)?.let {
                    annotateAssignment(variant, it, wrapper)
                }
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
                if (COMMAS_ONLY_REGEX.matches(next.text)) {
                    wrapper.newInfoAnnotation(null)
                            .range(element)
                            .withFix(CaosScriptExpandCommasIntentionAction)
                            .create()
                } else if (next is CaosScriptSpaceLikeOrNewline && next.textContains('\n')) {
                    wrapper.newInfoAnnotation(null)
                            .range(element)
                            .withFix(CaosScriptCollapseNewLineIntentionAction(CollapseChar.COMMA))
                            .withFix(CaosScriptCollapseNewLineIntentionAction(CollapseChar.SPACE))
                            .create()
                } else {
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
            }
            element is CaosScriptSpaceLikeOrNewline -> {
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
            else -> {
            }
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
        val commandTypeDef = CaosDefCommandElementsByNameIndex
                .Instance[commandString, project]
                .filter {
                    it.isVariant(variant)
                }
                .mapNotNull {
                    it.returnTypeStruct?.type?.typedef
                }
                .firstOrNull()
                ?: return
        val typedefWithBitFlags = CaosDefTypeDefinitionElementsByNameIndex
                .Instance[commandTypeDef, project]
                .firstOrNull {
                    it.isBitflags && it.isVariant(variant)
                }
                ?: return
        wrapper.newInfoAnnotation(null)
                .range(addTo)
                .withFix(GenerateBitFlagIntegerIntentionAction(addTo, typedefWithBitFlags.typeName, typedefWithBitFlags.keys, currentValue))
                .create()
    }

    companion object {
        private val COMMAS_ONLY_REGEX = "[,]+".toRegex()
    }
}