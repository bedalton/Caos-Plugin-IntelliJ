package com.badahori.creatures.plugins.intellij.agenteering.caos.annotators

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptTokenToLowerCaseFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptShouldBeLowerCase
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.hasParentOfType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement


/**
 * Annotates command case errors
 * In C1 and C2 all command and var names must be lowercase
 */
class CaosScriptCommandCaseAnnotator : Annotator {

    /**
     * Process each element in file
     */
    override fun annotate(elementIn: PsiElement, annotationHolder: AnnotationHolder) {
        val token = elementIn as? CaosScriptShouldBeLowerCase
                ?: return
        val variant = token.containingCaosFile?.variant
                ?: return
        // If variant is not C1 or C2, return
        if (variant.isNotOld || elementIn.hasParentOfType(CaosDefCompositeElement::class.java))
            return
        annotateC1Token(variant, token, annotationHolder)
    }

    /**
     * Actually annotate the token
     */
    private fun annotateC1Token(variant: CaosVariant, token:CaosScriptShouldBeLowerCase, annotationWrapper: AnnotationHolder) {
        val commandString = token.text
        if (commandString.toLowerCase() == commandString) {
            return
        }
        val fix = CaosScriptTokenToLowerCaseFix(token)
        annotationWrapper
                .newErrorAnnotation(CaosBundle.message("caos.annotator.syntax-error-annotator.invalid-command-case", variant))
                .range(token)
                .newFix(fix)
                .batch()
                .registerFix()
                .create()
    }
}


