package com.openc2e.plugins.intellij.caos.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.caos.fixes.CaosScriptExpandCommasIntentionAction
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCAssignment
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCommandCall
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptExpectsValue

class CaosScriptHelperActionAnnotator : Annotator{
    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        val wrapper = AnnotationHolderWrapper(annotationHolder)
        when {
            element is CaosScriptCommandCall -> wrapper.newInfoAnnotation(null)
                    .range(element)
                    .withFix(CaosScriptExpandCommasIntentionAction)
                    .create()
            element is CaosScriptExpectsValue -> {
                val assignment = element.parent as? CaosScriptCAssignment
                if (assignment != null && assignment.commandString.toUpperCase() == "CLAS") {
                    // Allow Changing Clas
                }
            }
        }
    }
}