package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.highlighting

import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support.PrayTags
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.annotators.colorize
import com.badahori.creatures.plugins.intellij.agenteering.utils.like
import com.badahori.creatures.plugins.intellij.agenteering.utils.startOffset
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement

class PRAYHighlighterAnnotator: Annotator, DumbAware {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is PrayString -> annotateString(element, holder)
            is PrayBlockName -> holder.colorize(element, PraySyntaxHighlighter.BLOCK_NAME)
            is PrayInlineFile -> annotateInlineFile(element, holder)
        }
    }
}


private fun annotateString(element: PrayString, holder: AnnotationHolder) {
    if (element.parent !is PrayTagTagName) {
        return
    }
    val tagName = element.stringValue
    val numberedTagNumberRange = PrayTags.getNumberRange(tagName, element.startOffset)
    if (numberedTagNumberRange != null) {
        holder.colorize(element, PraySyntaxHighlighter.NUMBERED_TAG)
        holder.colorize(numberedTagNumberRange, PraySyntaxHighlighter.NUMBERED_TAG_NUMBER)
    } else if (PrayTags.isOfficialTag(tagName, element.getParentOfType(PrayAgentBlock::class.java)?.blockTagString like "EGGS"))
        holder.colorize(element, PraySyntaxHighlighter.OFFICIAL_TAG)
    else
        holder.colorize(element, PraySyntaxHighlighter.CUSTOM_TAG)
}

private fun annotateInlineFile(element: PrayInlineFile, wrapper: AnnotationHolder) {
    element.outputFileName?.string?.let { outputFile ->
        wrapper.colorize(outputFile, PraySyntaxHighlighter.BLOCK_NAME)
    }
}