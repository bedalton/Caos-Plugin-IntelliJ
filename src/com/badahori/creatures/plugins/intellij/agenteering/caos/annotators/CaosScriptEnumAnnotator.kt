package com.badahori.creatures.plugins.intellij.agenteering.caos.annotators

import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptEtchOnC1QuickFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceWordFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.matchCase
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement

/**
 * Annotate ENUM statements including ESCN, ESEE, ETCH, ECON, etc
 */
class CaosScriptEnumAnnotator : Annotator {
    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        val variant = (element.containingFile as? CaosScriptFile)?.variant
                ?: return
        val annotationWrapper = AnnotationHolderWrapper(annotationHolder)
        when (element) {
            is CaosScriptEnumNextStatement -> annotateBadEnumStatement(variant, element, annotationWrapper)
            is CaosScriptCNext -> annotateNext(element, annotationWrapper)
            is CaosScriptCNscn -> annotateNscn(element, annotationWrapper)
            is CaosScriptEnumSceneryStatement -> annotateSceneryEnum(variant, element, annotationWrapper)
        }
    }

    /**
     * Annotates [NEXT] if used in ESCN...NSCN
     * ENUM..NSCN is allowed in grammar in case it is accidentally used by user
     * this annotation marks it as invalid as it should be
     */
    private fun annotateNext(element: CaosScriptCNext, annotationWrapper: AnnotationHolderWrapper) {
        val parent = element.getParentOfType(CaosScriptHasCodeBlock::class.java)
        if (parent == null) {
            annotationWrapper.newErrorAnnotation("NEXT should not be used outside of enum")
                    .range(element)
                    .create()
            return
        }
        if (parent is CaosScriptEnumSceneryStatement) {
            val next = "NSCN".matchCase(element.text)
            annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.enum-terminator-invalid", "ESCN", "NSCN", "NEXT"))
                    .range(element)
                    .withFix(CaosScriptReplaceWordFix(next, element))
                    .create()
        }
    }

    /**
     * Annotate NSCN if used in ENUM..NEXT
     * Grammar allows construct on off chance it is used by accident. This marks it as an error
     */
    private fun annotateNscn(element: CaosScriptCNscn, annotationWrapper: AnnotationHolderWrapper) {
        val parent = element.getParentOfType(CaosScriptHasCodeBlock::class.java)

        if (parent == null) {
            annotationWrapper.newErrorAnnotation("NSCN should not be used outside of ESCN..NSCN enum")
                    .range(element)
                    .create()
            return
        }

        if (parent is CaosScriptEnumNextStatement) {
            val enum = parent.enumHeaderCommand.commandStringUpper
            val next = "NEXT".matchCase(element.text)
            annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.enum-terminator-invalid", enum, "NEXT", "NSCN"))
                    .range(element)
                    .withFix(CaosScriptReplaceWordFix(next, element))
                    .create()
        }
    }

    /**
     * Annotates ESCN..NSCN on all variants other than C2
     */
    private fun annotateSceneryEnum(variant: CaosVariant, element: CaosScriptEnumSceneryStatement, annotationWrapper: AnnotationHolderWrapper) {
        if (variant == CaosVariant.C2)
            return
        annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.escn-only-on-c2-error-message"))
                .range(element.escnHeader.cEscn)
                .create()
    }

    /**
     * Annotates use of enum constructs on wrong variants
     */
    private fun annotateBadEnumStatement(variant: CaosVariant, element: CaosScriptEnumNextStatement, annotationWrapper: AnnotationHolderWrapper) {
        val enumToken = element.enumHeaderCommand.commandToken
        val enumText = enumToken.text.toUpperCase()
        // All variants support ENUM..NEXT
        if (enumText == "ENUM")
            return
        // Non-Old variants support all but ESCN..NSCN, but that is marked separately
        if (variant.isNotOld)
            return

        // C2 can handle all enums except ECON.
        if (enumText != "ECON" && variant == CaosVariant.C2) {
            return
        }

        // Mark statement as error in C1 and C2
        var builder = annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.bad-enum-error-message", enumText))
                .range(enumToken)

        // Add optional fix to ETCH, though it is quite experimental
        if (enumText == "ETCH") {
            builder = builder.withFix(CaosScriptEtchOnC1QuickFix(element))
        }
        builder.create()
    }
}