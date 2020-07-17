package com.badahori.creatures.plugins.intellij.agenteering.caos.annotators

import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptTokenToLowerCaseFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptShouldBeLowerCase
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement

class CaosScriptCommandCaseAnnotator : Annotator {

    override fun annotate(elementIn: PsiElement, annotationHolder: AnnotationHolder) {
        val token = elementIn as? CaosScriptShouldBeLowerCase
                ?: return
        val variant = token.containingCaosFile?.variant
                ?: return
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


