package com.badahori.creatures.plugins.intellij.agenteering.caos.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptCollapseNewLineIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptExpandCommasIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CollapseChar
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCAssignment
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCommandLike
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSpaceLikeOrNewline
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.next

class CaosScriptHelperActionAnnotator : Annotator {
    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        val wrapper = AnnotationHolderWrapper(annotationHolder)
        when {
            element is CaosScriptCommandLike -> {
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
                } else if (next is CaosScriptSpaceLikeOrNewline && next.textContains('\n')){
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
                val assignment = element.parent as? CaosScriptCAssignment
                if (assignment != null && assignment.commandString.toUpperCase() == "CLAS") {
                    // Allow Changing Clas
                }
            }
        }
    }
    companion object {
        private val COMMAS_ONLY_REGEX = "[,]+".toRegex()
    }
}