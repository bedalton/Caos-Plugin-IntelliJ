@file:Suppress("UnstableApiUsage")

package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.annotator

import com.badahori.creatures.plugins.intellij.agenteering.caos.annotators.newErrorAnnotation
import com.badahori.creatures.plugins.intellij.agenteering.caos.annotators.newWeakWarningAnnotation
import com.badahori.creatures.plugins.intellij.agenteering.caos.annotators.withFixes
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.DeleteElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.AgentMessages
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Pray
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import bedalton.creatures.common.util.stripSurroundingQuotes
import com.badahori.creatures.plugins.intellij.agenteering.caos.annotators.newInfoAnnotation
import com.badahori.creatures.plugins.intellij.agenteering.caos.completion.LinkFilesInsertHandler
import com.intellij.openapi.project.DumbAware

class Caos2PrayAnnotator: Annotator, DumbAware {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is CaosScriptCaos2CommandName -> validateCommandName(element, holder)
            is CaosScriptCaos2Tag -> annotateTag(element, holder)
        }
    }
}


private fun annotateTag(element: CaosScriptCaos2Tag, holder: AnnotationHolder) {
    if (!element.containingCaosFile?.isCaos2Pray.orFalse())
        return
    val tagNameElement = element.caos2TagName
    val tagName = tagNameElement.stringValue.replace(WHITESPACE, " ")
        .nullIfEmpty()
        ?: return
    annotateDuplicateTag(tagNameElement, tagName, holder)
    val value = element.caos2Value
        ?: return
    PrayAnnotatorUtil.annotateValue(null, tagName, value, holder)
}


private val AGENT_NAME_COMMAND_REGEX = "((C3|DS)-?Name)|[a-zA-Z0-9_]{4}-Name".toRegex(RegexOption.IGNORE_CASE)

/**
 * Validates a COB comment directive, to ensure that it actually exists
 */
private fun validateCommandName(element: CaosScriptCaos2CommandName, holder: AnnotationHolder) {
    if (!element.containingCaosFile?.isCaos2Pray.orFalse())
        return
    val file = element.containingFile
    val actions = PrayCommand.getActionCommands()
    for(action in actions) {
        val insertAction = LinkFilesInsertHandler(action, true)
        if (!insertAction.isAvailable(file)) {
            continue
        }
        holder.newInfoAnnotation(null)
            .range(element)
            .withFix(insertAction)
            .create()
    }
    val tagNameRaw = element.text
    if (tagNameRaw.matches(AGENT_NAME_COMMAND_REGEX) && tagNameRaw notLike "Link" && tagNameRaw notLike "RSCR")
        return
    val tagName = element.text.replace(WHITESPACE_OR_DASH, " ").nullIfEmpty()
        ?: return
    val command = PrayCommand.fromString(tagName)
    if (command != null) {
        return
    }
    val similar = getFixesForSimilar(element, tagName)
        .toTypedArray()
    val error = "'$tagNameRaw' is not a recognized PRAY command"
    holder.newErrorAnnotation(error)
        .range(element)
        .withFixes(*similar)
        .create()
}

private fun getFixesForSimilar(element: PsiElement, tagName: String) : List<CaosScriptReplaceElementFix> {
    return PrayCommand.getCommands()
        .map { aTag ->
            aTag.keyStrings.minByOrNull { it.levenshteinDistance(tagName) }!!.let { key ->
                Pair(key, key.levenshteinDistance(tagName))
            }
        }.filter {
            it.second < 7
        }
        .map {
            CaosScriptReplaceElementFix(
                element,
                it.first,
                AgentMessages.message("caos2commands.fixes.replace-command", it.first)
            )
        }
}


private fun annotateDuplicateTag(element: CaosScriptCaos2TagName, tag: String, holder: AnnotationHolder) {
    val offset = element.startOffset
    val matches = element.containingCaosFile?.prayTags
        ?.filter {
            it.indexInFile < offset && it.tag.replace(WHITESPACE, " ").equals(tag, ignoreCase = true)
        }
        ?.nullIfEmpty()
        ?: return
    // Is a strict match
    val first = matches.firstOrNull{ element.text.stripSurroundingQuotes() == it.tag }?.tag
    var annotation = if (first != null) {
        val error = AgentMessages.message("pray.annotator.duplicate-tags.duplicate-tag-error", first)
        holder.newErrorAnnotation(error)
    } else {
        val error = AgentMessages.message("pray.annotator.duplicate-tags.almost-duplicate-tag-error",
            matches.minByOrNull { it.indexInFile }!!.tag)
        holder.newWeakWarningAnnotation(error)
    }
    val parentComment = element.getParentOfType(CaosScriptCaos2BlockComment::class.java)
    if (parentComment != null) {
        annotation = annotation.withFix(DeleteElementFix("Remove duplicate tag element", parentComment))
    }
    annotation
        .range(element)
        .create()
}