package com.openc2e.plugins.intellij.caos.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.caos.fixes.CaosScriptTokenToLowerCaseFix
import com.openc2e.plugins.intellij.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.caos.lang.CaosVariant
import com.openc2e.plugins.intellij.caos.lang.variant
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptShouldBeLowerCase
import com.openc2e.plugins.intellij.caos.psi.impl.containingCaosFile

class CaosScriptCommandCaseAnnotator : Annotator {

    override fun annotate(elementIn: PsiElement, annotationHolder: AnnotationHolder) {
        val token = elementIn as? CaosScriptShouldBeLowerCase
                ?: return
        val variant = token.containingCaosFile.variant
        if (variant != CaosVariant.C1 && variant != CaosVariant.C2)
            return
        val annotationWrapper = AnnotationHolderWrapper(annotationHolder)
        annotateC1Token(variant, token, annotationWrapper)
    }

    private fun annotateC1Token(variant: CaosVariant, token:CaosScriptShouldBeLowerCase, annotationWrapper: AnnotationHolderWrapper) {
        val commandString = token.text
        if (commandString.toLowerCase() == commandString) {
            return
        }
        val fix = CaosScriptTokenToLowerCaseFix(token)
        annotationWrapper
                .newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.invalid-command-case", variant))
                .range(token)
                .newFix(fix)
                .batch()
                .registerFix()
                .create()
    }
}


