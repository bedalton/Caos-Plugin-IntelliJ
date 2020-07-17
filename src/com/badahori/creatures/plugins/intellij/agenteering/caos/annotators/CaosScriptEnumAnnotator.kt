package com.badahori.creatures.plugins.intellij.agenteering.caos.annotators

import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptEtchOnC1QuickFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceWordFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.matchCase
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement

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

    private fun annotateSceneryEnum(variant: CaosVariant, element: CaosScriptEnumSceneryStatement, annotationWrapper: AnnotationHolderWrapper) {
        if (variant == CaosVariant.C2)
            return
        annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.escn-only-on-c2-error-message"))
                .range(element.escnHeader.cEscn)
                .create()
    }

    private fun annotateBadEnumStatement(variant: CaosVariant, element: CaosScriptEnumNextStatement, annotationWrapper: AnnotationHolderWrapper) {
        val cNscn = element.cNscn
        val enumToken = element.enumHeaderCommand.commandToken
        val enumText = enumToken.text.toUpperCase()
        if (cNscn != null) {
            val next = "NEXT".matchCase(cNscn.text)
            annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.enum-terminator-invalid", enumText, "NEXT", "NSCN"))
                    .range(cNscn)
                    .withFix(CaosScriptReplaceWordFix(next, cNscn))
                    .create()
        }
        if (enumText == "ENUM")
            return
        if (variant.isNotOld)
            return
        if (enumText != "ECON" && variant == CaosVariant.C2) {
            return
        }

        var builder = annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.bad-enum-error-message", enumText))
                .range(enumToken)
        if (enumText == "ETCH") {
            builder = builder.withFix(CaosScriptEtchOnC1QuickFix(element))
        }
        builder.create()
    }
}