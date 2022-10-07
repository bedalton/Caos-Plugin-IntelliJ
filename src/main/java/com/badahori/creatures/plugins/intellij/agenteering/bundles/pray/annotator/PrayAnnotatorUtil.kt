package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.annotator

import bedalton.creatures.util.FileNameUtil
import bedalton.creatures.util.stripSurroundingQuotes
import bedalton.creatures.util.toArrayOf
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.stubs.PrayTagStruct
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support.PrayDependencyCategories
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support.PrayDependencyCategories.dependencyCategoryName
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support.PrayTags
import com.badahori.creatures.plugins.intellij.agenteering.caos.annotators.newErrorAnnotation
import com.badahori.creatures.plugins.intellij.agenteering.caos.annotators.newWarningAnnotation
import com.badahori.creatures.plugins.intellij.agenteering.caos.annotators.newWeakWarningAnnotation
import com.badahori.creatures.plugins.intellij.agenteering.caos.annotators.withFixes
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.AgentMessages
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.PrayTagValue
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.WHITESPACE
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.lang.annotation.AnnotationHolder


object PrayAnnotatorUtil {
    fun annotateValue(allTags: List<PrayTagStruct<*>>?, tagRaw: String, valueRaw: PrayTagValue, holder: AnnotationHolder) {
        mAnnotateValue(allTags, tagRaw, valueRaw, holder)
    }
}

private val FLOAT_REGEX = "[+-]?\\d*\\.\\d+".toRegex()


private fun annotateTagTypeError(tag: String, valueRaw: PrayTagValue, holder: AnnotationHolder): Boolean {

    // If is official tag, get whether it should be an int or string
    val requiresString = PrayTags.requiresString(tag)
        ?: return false

    // There is no parseable tag value
    if (valueRaw.valueAsInt == null && valueRaw.valueAsString == null) {
        if (FLOAT_REGEX.matches(valueRaw.text)) {
            holder.newErrorAnnotation(AgentMessages.message("pray.annotator.tag-values.invalid-float-value"))
                .range(valueRaw)
                .create()
        } else  {
            // This should not happen....
            // not sure why PrayTagValue element would be created for something that is not a string, int or float
            LOGGER.severe("Tag value: <${valueRaw.text}> is neither an int nor a string....")
            return false
        }
    }

    // Check if expected type and actual type match
    if (requiresString == valueRaw.isStringValue) {
        // Return if matching
        return true
    }
    val replacementValue = if (requiresString) {
        if (valueRaw.isNumberValue)
            CaosScriptReplaceElementFix(
                valueRaw,
            "\"${valueRaw.text}\"",
                "Convert integer to string"
            )
        else
            null
    } else {
        valueRaw.valueAsString?.toIntOrNull()?.let {
            CaosScriptReplaceElementFix(
                valueRaw,
                "$it",
                "Convert string to integer value"
            )
        }
    }?.let {
        arrayOf(it)
    } ?: emptyArray()

    // String was not quoted
    val error = if (requiresString)
        AgentMessages.message("pray.annotator.tag-values.expects-string")
    else
        AgentMessages.message("pray.annotator.tag-values.expects-integer")

    // Create actual annotation with possible fixes
    holder.newErrorAnnotation(error)
        .range(valueRaw)
        .withFixes(*replacementValue)
        .create()
    return false
}


private fun mAnnotateValue(allTags: List<PrayTagStruct<*>>?, tagRaw: String, valueRaw: PrayTagValue, holder: AnnotationHolder) {
    val tagCleaned = tagRaw
        .trim()
        .stripSurroundingQuotes()
        .replace(WHITESPACE, " ")

    val tag = PrayTags.getCorrectedCase(tagCleaned)
        ?: PrayTags.normalize(tagCleaned)
        ?: (if (PrayTags.isOfficialTag(tagCleaned, true)) tagCleaned else null)
        ?: return
    val requiresString = PrayTags.requiresString(tag)
    if (requiresString == null) {
        LOGGER.severe("Tag '$tag' recognized but required value type was not set")
        return
    }

    if (!annotateTagTypeError(tag, valueRaw, holder)) {
        return
    }
    if (requiresString) {
        val stringValue = valueRaw.valueAsString
            ?: return
        annotateStringValue(tag, valueRaw, stringValue, holder)
    } else {
        val intValue = valueRaw.valueAsInt
            ?: return
        annotateIntegerValue(allTags, tag, valueRaw, intValue, holder)
    }
}

private fun annotateStringValue(tag: String, tagValueElement: PrayTagValue, string: String, holder: AnnotationHolder) {
    when (tag) {
        "Agent Description",
        "Agent Description-es",
        "Agent Description-fr",
        "Agent Description-it",
        "Agent Description-de",
        "Agent Description-nl" -> if (string.length > PrayTags.MAX_DESCRIPTION_LENGTH) {
            val error = AgentMessages.message("pray.annotator.tag-values.agent-description-too-long", PrayTags.MAX_DESCRIPTION_LENGTH)
            holder.newWeakWarningAnnotation(error)
                .range(tagValueElement)
                .create()
        }
        "Web Icon Animation String" -> if (string.contains(' ') || string.contains('[')) {
            val error = AgentMessages.message("pray.annotator.tag-values.icon-animation-string-invalid")
            holder.newWarningAnnotation(error)
                .range(tagValueElement)
                .create()
        }
        "Agent Animation Gallery",
        "Egg Gallery male",
        "Egg Gallery female" -> if (FileNameUtil.getExtension(string).nullIfEmpty() != null) {
            val error = AgentMessages.message("pray.annotator.tag-values.gallery-should-not-have-extension", tag)
            val fixes = FileNameUtil.getFileNameWithoutExtension(string)?.let { fileNameWithoutExtension ->
                CaosScriptReplaceElementFix(
                    tagValueElement,
                    "\"${fileNameWithoutExtension}\"",
                    "Remove file extension"
                )
            }?.toArrayOf() ?: emptyArray()
            holder.newWarningAnnotation(error)
                .range(tagValueElement)
                .withFixes(*fixes)
                .create()

        }
    }
}


private fun annotateIntegerValue(allTags: List<PrayTagStruct<*>>?, tag: String, tagValueElement: PrayTagValue, intValue: Int, holder: AnnotationHolder) {
    if (PrayTags.DEPENDENCY_CATEGORY_TAG.matches(tag)) {
        val dependencyIndex = PrayTags.DEPENDENCY_CATEGORY_TAG.matchEntire(tag)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?: return
        if (allTags != null) {
            annotateDependencyCategory(allTags, tagValueElement, dependencyIndex, intValue, holder)
        }
        return
    }
    when(tag) {
        "Agent Sprite First Image" -> if (intValue < 0) {
            holder.newErrorAnnotation(AgentMessages.message("pray.annotator.tag-values.agent-first-image-less-than-zero"))
                .range(tagValueElement)
                .create()
        }

    }
}


@Suppress("UNUSED_PARAMETER")
private fun annotateDependencyCategory(otherTags: List<PrayTagStruct<*>>, tagValueElement: PrayTagValue, dependencyIndex: Int, categoryId: Int, holder: AnnotationHolder) {
    // Get corresponding tag
//    val dependencyTagRegex = PrayTags.DEPENDENCY_TAG_FUZZY
//    val dependencyTag = otherTags
//        .filterIsInstance<PrayTagStruct.StringTag>()
//        .sortedBy {
//            it.indexInFile
//        }
//        .firstOrNull check@{ tag ->
//            PrayTags.getNumberedTagIndex(dependencyTagRegex, tag.tag) == dependencyIndex
//        }
//        ?: return
//    annotateDependencyCategoryNumberIsIdeal(tagValueElement, categoryId, dependencyTag, holder)
    annotateDependencyCategoryNumberIsValid(tagValueElement, categoryId, holder)
}

//private fun annotateDependencyCategoryNumberIsIdeal(tagValueElement: PrayTagValue, categoryId: Int, dependencyTag: PrayTagStruct.StringTag, holder: AnnotationHolder) {
//    val dependencyFileName = dependencyTag.value
//        .trim()
//        .nullIfEmpty()
//        ?: return
//    val bestCategory = getBestCategory(dependencyFileName)
//        ?: return
//    if (bestCategory == categoryId)
//        return
//    val categoryName = dependencyCategoryName(categoryId, false)
//    val bestCategoryName = dependencyCategoryName(bestCategory, false)
//        ?: return
//    val message = AgentMessages.message(
//        "fixes.dependency-category.not-ideal-category",
//        bestCategoryName,
//        bestCategory,
//        categoryName ?: "INVALID!",
//        categoryId
//    )
//    holder.newWeakWarningAnnotation(message)
//        .range(tagValueElement)
//        .withFix(CaosScriptReplaceElementFix(
//            tagValueElement,
//            "$bestCategory",
//            AgentMessages.message(
//                "fixes.dependency-category.not-ideal-category.replace-with",
//                bestCategory,
//                bestCategoryName
//            )
//        ))
//        .create()
//}

private fun annotateDependencyCategoryNumberIsValid(tagValueElement: PrayTagValue, intValue: Int, holder: AnnotationHolder) {
    val error = when (intValue) {
        !in 0..11 -> AgentMessages.message("error.dependency-category.dependency-category-invalid", intValue)
        in PrayDependencyCategories.privateCategoryDirectories -> AgentMessages.message(
            "error.dependency-category.directory-restricted",
            intValue,
            dependencyCategoryName(intValue, false)!!
        )
        else -> return
    }
    val fixes = PrayDependencyCategories
        .userAccessible
        .map { (categoryId, directory) ->
            CaosScriptReplaceElementFix(
                tagValueElement,
                "$categoryId",
                AgentMessages.message(
                    "fixes.dependency-category.replace-dependency-category",
                    directory,
                    categoryId
                )
            )
        }
    holder.newErrorAnnotation(error)
        .range(tagValueElement)
        .withFixes(*fixes.toTypedArray())
        .create()
}