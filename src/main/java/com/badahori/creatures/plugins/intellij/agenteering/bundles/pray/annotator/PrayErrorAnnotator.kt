package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.annotator

import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support.PrayTags
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support.PrayTags.DEPENDENCY
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support.PrayTags.DEPENDENCY_CATEGORY
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support.PrayTags.SCRIPT
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.stubs.PrayTagStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.annotators.newErrorAnnotation
import com.badahori.creatures.plugins.intellij.agenteering.caos.annotators.newWarningAnnotation
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.DeleteElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.AgentMessages
import com.badahori.creatures.plugins.intellij.agenteering.utils.WHITESPACE
import com.badahori.creatures.plugins.intellij.agenteering.utils.getParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.startOffset
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement


// Regex Is strict
private val DEPENDENCY_REGEX = "Dependency (\\d+)".toRegex()
private val DEPENDENCY_CATEGORY_REGEX = "Dependency Category (\\d+)".toRegex()
private val SCRIPT_REGEX = "Script (\\d+)".toRegex()

private const val DEPENDENCY_COUNT = "Dependency Count"
private const val SCRIPT_COUNT = "Script Count"
private val fuzzyDependencyCount = "Dependency\\s*Count".toRegex(RegexOption.IGNORE_CASE)
private val fuzzyScriptCount = "Script\\s*Count".toRegex(RegexOption.IGNORE_CASE)

class PrayErrorAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is PrayString -> annotateStringError(element, holder)
            is PrayTagTagName -> annotateTagNameError(element, holder)
            is PrayTagTagValue -> annotatePrayTagValue(element, holder)
            is PrayElement -> {
                element.agentBlock?.let { agentBlock ->
                    annotatePrayAgent(agentBlock, holder)
                }
            }
            //is PrayAgentBlock -> annotatePrayAgent(element, holder)
        }
    }
}


private fun annotateStringError(string: PrayString, holder: AnnotationHolder) {
    if (string.singleQuoString != null) {
        holder.newErrorAnnotation(AgentMessages.message("pray.annotator.single-quote-strings-invalid"))
            .range(string)
            .withFix(
                CaosScriptReplaceElementFix(
                    string,
                    "\"${string.stringValue}\"",
                    "Replace single quotes with double"
                )
            )
            .create()
    }
}


private fun getElementCountTagValue(element: PsiElement, type: Int): Int? {
    // Get the parent agent block as count tags are scoped to the containing agent
    val tags = element.getParentOfType(PrayAgentBlock::class.java)
        ?.tagStructs
        ?: return null

    // Get strict match regex
    val countTag = when (type) {
        SCRIPT -> SCRIPT_COUNT
        DEPENDENCY -> DEPENDENCY_COUNT
        else -> throw Exception("Unhandled Numbered tag type int $type")
    }

    // Get fallback regex
    val regex = when (type) {
        SCRIPT -> fuzzyScriptCount
        DEPENDENCY -> fuzzyDependencyCount
        else -> throw Exception("Unhandled Numbered tag type int $type")
    }

    val tag = tags.firstOrNull {
        it.tag == countTag
    } ?: tags.firstOrNull {
        regex.matches(it.tag)
    }
    ?: return null

    return tag.value as? Int
}


private fun annotateTagNameError(element: PrayTagTagName, holder: AnnotationHolder) {
    val tagName = element.string.stringValue.trim()
        .nullIfEmpty()
        ?: return

    annotateDuplicateTag(element, tagName, holder)
    if (annotateNumberedTag(element, tagName, holder))
        return


}

private fun annotateNumberedTag(element: PsiElement, tagName: String, holder: AnnotationHolder): Boolean {
    val (type, index) = PrayTags.getNumberedTagParts(tagName)
        ?: return false
    val strictMatch = when (type) {
        DEPENDENCY -> DEPENDENCY_REGEX.matches(tagName)
        DEPENDENCY_CATEGORY -> DEPENDENCY_CATEGORY_REGEX.matches(tagName)
        SCRIPT -> SCRIPT_REGEX.matches(tagName)
        else -> throw Exception("Unhandled numbered tag type '$type'")
    }
    if (!strictMatch) {
        val correctedTag = when (type) {
            DEPENDENCY -> "Dependency $index"
            DEPENDENCY_CATEGORY -> "Dependency Category $index"
            SCRIPT -> "Script $index"
            else -> throw Exception("Unhandled numbered tag type '$type'")
        }
        holder.newWarningAnnotation(
            AgentMessages.message("pray.inspections.tags.incorrect-numbered-tag", correctedTag)
        )
            .range(element)
            .withFix(CaosScriptReplaceElementFix(
                element,
                correctedTag.let { tag ->
                    if (element.text[0].let { it == '"' || it == '\'' })
                        "\"$tag\""
                    else
                        tag
                },
                "Replace tag with '$correctedTag'"
            ))
            .create()
    }

    val tagGroup = when (type) {
        DEPENDENCY -> "Dependency"
        DEPENDENCY_CATEGORY -> "Dependency Category"
        SCRIPT -> "Script"
        else -> throw Exception("Unhandled numbered tag type '$type'")
    }
    if (index < 1) {
        holder.newErrorAnnotation(
            AgentMessages.message("pray.annotator.numbered-tag.tags-start-at-1", tagGroup.toLowerCase())
        )
            .range(element)
            .create()
        return true
    }
    val max = getElementCountTagValue(element, if (type == DEPENDENCY_CATEGORY) DEPENDENCY else type)
    if (max == null) {
        val countTag = when (type) {
            DEPENDENCY, DEPENDENCY_CATEGORY -> "Dependency Count"
            SCRIPT -> "Script Count"
            else -> throw Exception("Unhandled numbered tag type '$type'")
        }
        holder.newErrorAnnotation(AgentMessages.message("pray.annotator.numbered-tag.missing-group-count", countTag))
            .range(element)
            .create()
    } else if (max == 0) {
        holder.newErrorAnnotation(AgentMessages.message("pray.annotator.numbered-tag.max-is-zero", tagGroup))
            .range(element)
            .create()
        return true
    } else if (index > max) {
        holder.newErrorAnnotation(AgentMessages.message("pray.annotator.numbered-tag.out-of-range", tagGroup, max))
            .range(element)
            .create()
        return true
    }
    return true
}


private val agentTypeRegex = "Agent\\s+Type".toRegex(RegexOption.IGNORE_CASE)

private fun annotateAgentType(parentAgent: PrayAgentBlock?, prayTag: PrayPrayTag, holder: AnnotationHolder) {
    val prayTagTag: PrayTagTagName = prayTag.tagTagName
    val prayTagValue = prayTag.tagTagValue
    if (parentAgent == null) {
        holder
            .newErrorAnnotation(
                AgentMessages.message("pray.inspection.required-tags.requires-agent-block", "Agent Type")
            )
            .range(prayTagTag)
            .withFix(DeleteElementFix("Remove invalid \"Agent Type\" tag", prayTag))
            .create()
        return
    }

    val intValue = prayTag.valueAsInt
        ?: return

    if (intValue != 0) {
        holder.newWarningAnnotation(AgentMessages.message("pray.annotator.agent-type-is-non-zero"))
            .range(prayTagValue)
            .withFix(
                CaosScriptReplaceElementFix(
                    prayTagValue,
                    "0",
                    "Set \"Agent Type\" to '0'"
                )
            )
            .create()
    }
}


private fun annotatePrayTagValue(tagValue: PrayTagTagValue, holder: AnnotationHolder) {
    val element = (tagValue.parent as PrayPrayTag)
    val parentAgent = element.parent.parent as PrayAgentBlock
    val tag = element
        .tagName
        .trim()
        .nullIfEmpty()
        ?: return

    if (tag == "Script Count") {
        annotateScriptCount(parentAgent, element, holder)
    } else if (tag == "Dependency Count") {
        annotateDependencyCount(parentAgent, element, holder)
    } else if (agentTypeRegex.matches(tag)) {
        annotateAgentType(parentAgent, element, holder)
        return
    }
    PrayAnnotatorUtil.annotateValue(parentAgent.tagStructs, tag, tagValue, holder)
}

private fun annotateScriptCount(parentAgent: PrayAgentBlock?, element: PrayPrayTag, holder: AnnotationHolder) {
    annotateCountOf(parentAgent, element, "Scripts", PrayTags.SCRIPT_TAG_FUZZY, holder)
}

private fun annotateDependencyCount(parentAgent: PrayAgentBlock?, element: PrayPrayTag, holder: AnnotationHolder) {
    annotateCountOf(parentAgent, element, "Dependency", PrayTags.DEPENDENCY_TAG_FUZZY, holder)
    annotateCountOf(parentAgent, element, "Dependency Category", PrayTags.DEPENDENCY_CATEGORY_TAG_FUZZY, holder)
}

private fun annotateCountOf(parentAgent: PrayAgentBlock?, element: PrayPrayTag, countOf: String, regex: Regex, holder: AnnotationHolder) {
    if (parentAgent == null) {
        holder.newErrorAnnotation(
            AgentMessages.message("pray.inspection.required-tags.requires-agent-block", countOf)
        )
            .range(element)
            .create()
        return
    }
    val expectedCount = element.valueAsInt
        ?: return
    val scriptIndices = getIndices(regex, parentAgent.tagStructs)
        .distinct()
    val actualCount = scriptIndices.size

    if (actualCount > expectedCount) {
        holder.newErrorAnnotation(AgentMessages.message("pray.inspection.required-tags.tag-count.too-many-tags", countOf, ""+expectedCount, actualCount))
            .range(element.tagTagValue)
            .withFix(CaosScriptReplaceElementFix(
                element.tagTagValue,
                "$actualCount",
                "Replace with $countOf count of $actualCount"
            ))
            .create()
        return
    }
    val missing = (1..expectedCount).filter { it !in scriptIndices }
    if (missing.isEmpty())
        return
    holder
        .newErrorAnnotation(AgentMessages.message("pray.inspection.required-tags.tag-count.missing-indices", countOf, missing.joinToString(",")))
        .range(element.tagTagValue)
        .create()
}

private fun getIndices(regex: Regex, tagStructs: List<PrayTagStruct<*>>): List<Int> {
    return tagStructs
        .mapNotNull {
            regex
                .matchEntire(it.tag.trim())
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                ?.toIntOrNull()
        }
}


private fun annotateDuplicateTag(element: PrayTagTagName, tagName: String, holder: AnnotationHolder) {
    val agent = element.getParentOfType(PrayAgentBlock::class.java)
        ?: return
    val tagNameRegex = tagName
        .replace(WHITESPACE, " ")
        .replace(" ", "\\s+")
        .toRegex(RegexOption.IGNORE_CASE)
    val structOfSame = agent.tagStructs.filter { tagNameRegex.matches(it.tag) }
    if (structOfSame.size <= 1)
        return
    val thisIndexInFile = element.startOffset
    val isDuplicate = structOfSame.any { it.indexInFile < thisIndexInFile }

    if (!isDuplicate)
        return
    holder
        .newErrorAnnotation(
            AgentMessages.message(
                "pray.annotator.duplicate-tags.duplicate-tag-error",
                element.stringValue
            )
        )
        .range(element)
        .create()
}