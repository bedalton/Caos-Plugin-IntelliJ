package com.badahori.creatures.plugins.intellij.agenteering.caos.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptEtchOnC1QuickFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceWordFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.matchCase

class CaosScriptEnumAnnotator : Annotator {
    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        val variant = (element.containingFile as? CaosScriptFile).variant
        val annotationWrapper = AnnotationHolderWrapper(annotationHolder)
        when (element) {
            is CaosScriptCEnum -> element.getParentOfType(CaosScriptEnumNextStatement::class.java)?.let { annotateBadEnumStatement(variant, it, annotationWrapper) }
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
            val enum = parent.enumHeaderCommand.cEnum.text.toUpperCase()
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
        if (cNscn != null) {
            val enum = element.enumHeaderCommand.cEnum.text
            val next = "NEXT".matchCase(cNscn.text)
            annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.enum-terminator-invalid", enum, "NEXT", "NSCN"))
                    .range(cNscn)
                    .withFix(CaosScriptReplaceWordFix(next, cNscn))
                    .create()
        }
        val header = element.enumHeaderCommand.cEnum
        if (header.kEnum != null)
            return
        if (variant !in CaosScriptSyntaxErrorAnnotator.VARIANT_OLD)
            return
        header.kEpas?.let {
            annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.bad-enum-error-message", it.text.toUpperCase()))
                    .range(it)
                    .create()
            return
        }
        if (variant != CaosVariant.C1)
            return
        val badElement = header.firstChild
        ?: return // return if enum type is ENUM
        var builder = annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.bad-enum-error-message", badElement.text.toUpperCase()))
                .range(badElement)
        if (badElement.text.toUpperCase() == "ETCH") {
            builder = builder.withFix(CaosScriptEtchOnC1QuickFix(element))
        }
        builder.create()
    }
}