package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.annotator

import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayAgentBlock
import com.intellij.lang.annotation.AnnotationHolder


@Suppress("UNUSED_PARAMETER")
internal fun annotatePrayAgent(agent: PrayAgentBlock, holder: AnnotationHolder) {
    //LOGGER.info("Annotating pray agent: ${agent.blockTagString} \"${agent.blockNameString}\"")
    //val tagElements = agent.blockElementList.mapNotNull { it.prayTag ?: it.inlineText }
    //annotateDuplicateTags(tagElements,  holder)
}
//
//private fun annotateDuplicateTags(tags: List<PrayTag>, holder: AnnotationHolder) {
//    val tagNames = tags.map { it.tagName.toLowerCase().replace(WHITESPACE, " ") }
//    LOGGER.info("Tag Structs: $tagNames")
//    val duplicateTags = tagNames
//        .filter { tag ->
//            tagNames.count { it == tag } > 1
//        }
//        .distinct()
//        .map {
//            val regexString = it.replace(" ", "\\s+")
//            LOGGER.info("Tag Regex String: $regexString")
//            regexString.toRegex(RegexOption.IGNORE_CASE)
//        }
//    if (duplicateTags.isEmpty()) {
//        LOGGER.info("No duplicate tags")
//        return
//    }
//
//    for (tagRegex in duplicateTags) {
//        // Find duplicated tags, and drop first to mark all following duplicates
//        val duplicated = tags
//            .filter {
//                tagRegex.matches(it.tagName)?.apply {
//                    LOGGER.info ("Regex: <${tagRegex.pattern}> matches <${it.tagName}>: $this")
//                }
//            }
//            .sortedBy { it.startOffset }
//            .drop(1)
//
//        // Ensure there is a tag to annotate
//        if (duplicated.isEmpty()) {
//            LOGGER.warning("Tag marked as duplicate, but only one tag returned from filter")
//            continue
//        }
//
//        // Loop through duplicated tags, and mark with error
//        for (tag in duplicated) {
//            val errorElement = if (tag is PrayPrayTag)
//                tag.tagTagName
//            else if (tag is CaosScriptCaos2Tag)
//                tag.caos2TagName
//            else
//                tag.getChildOfType(PrayTagName::class.java)
//                    ?: tag
//            holder
//                .newErrorAnnotation(
//                    AgentMessages.message(
//                        "pray.annotator.duplicate-tags.duplicate-tag-error",
//                        tag.tagName
//                    )
//                )
//                .range(errorElement)
//                .create()
//        }
//
//    }
//}
