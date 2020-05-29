package com.openc2e.plugins.intellij.caos.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.caos.fixes.CaosScriptEtchOnC1QuickFix
import com.openc2e.plugins.intellij.caos.fixes.CaosScriptReplaceWordFix
import com.openc2e.plugins.intellij.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFile
import com.openc2e.plugins.intellij.caos.lang.variant
import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.psi.util.LOGGER
import com.openc2e.plugins.intellij.caos.utils.matchCase

class CaosScriptEnumAnnotator : Annotator {
    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        val variant = (element.containingFile as? CaosScriptFile).variant
        val annotationWrapper = AnnotationHolderWrapper(annotationHolder)
        LOGGER.info("Annotate variable enums statements. Type: ${element.javaClass.canonicalName}")
        when (element) {
            is CaosScriptCEnum -> element.getParentOfType(CaosScriptEnumNextStatement::class.java)?.let { annotateBadEnumStatement(variant, it, annotationWrapper) }
            is CaosScriptCNext -> annotateNext(element, annotationWrapper)
            is CaosScriptCNscn -> annotateNscn(variant, element, annotationWrapper)
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

    private fun annotateNscn(variant: String, element: CaosScriptCNscn, annotationWrapper: AnnotationHolderWrapper) {
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

    private fun annotateSceneryEnum(variant: String, element: CaosScriptEnumSceneryStatement, annotationWrapper: AnnotationHolderWrapper) {
        if (variant == "C2")
            return
        annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.escn-only-on-c2-error-message"))
                .range(element.escnHeader.cEscn)
                .create()
    }

    private fun annotateBadEnumStatement(variant: String, element: CaosScriptEnumNextStatement, annotationWrapper: AnnotationHolderWrapper) {
        LOGGER.info("Annotating possibly bad enum statement")
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
        if (variant != "C1")
            return
        val badElement = header.kEsee ?: header.kEtch ?: header.kEpas
        ?: return // return if enum type is ENUM
        LOGGER.info("Annotating bad enum ${badElement.text}")
        var builder = annotationWrapper.newErrorAnnotation(CaosBundle.message("caos.annotator.command-annotator.bad-enum-error-message", badElement.text.toUpperCase()))
                .range(badElement)
        if (badElement.text.toUpperCase() == "ETCH") {
            builder = builder.withFix(CaosScriptEtchOnC1QuickFix(element))
        }
        builder.create()
    }
}