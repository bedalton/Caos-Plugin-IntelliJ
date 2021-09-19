package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.util

import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.stubs.PrayTagStruct
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.PrayTagValue
import com.badahori.creatures.plugins.intellij.agenteering.caos.references.PrayQuoteStringReference
import com.badahori.creatures.plugins.intellij.agenteering.utils.startOffset
import com.intellij.psi.PsiElement
import stripSurroundingQuotes

object PrayPsiImplUtil {

    @JvmStatic
    fun getStringValue(string: PrayString): String {
        return string.incompleteString
            ?.text
            ?.substring(1)
            ?: string
                .text
                .stripSurroundingQuotes()
    }

    @JvmStatic
    fun getValueAsString(element: PrayTagTagValue): String? {
        return element.string?.stringValue
    }


    @JvmStatic
    fun getValueAsString(tag: PrayPrayTag): String? {
        return tag.stub?.stringValue ?: tag.tagTagValue.valueAsString
    }

    @JvmStatic
    fun getValueAsInt(tag: PrayPrayTag): Int? {
        return tag.stub?.intValue ?: tag.tagTagValue.valueAsInt
    }

    @JvmStatic
    fun getStringValue(element: CaosScriptCompositeElement): String? {
        val text = element.text
        if (text.startsWith('"') || text.endsWith('\''))
            return element.text.stripSurroundingQuotes()
        return null
    }

    @JvmStatic
    fun getValueAsInt(element: CaosScriptCompositeElement): Int? {
        return element.text.toIntOrNull()
    }

    @JvmStatic
    fun getInputFileNameString(inlineFile: PrayInlineFile): String? {
        return inlineFile.inputFileName?.text?.stripSurroundingQuotes()
    }

    @JvmStatic
    fun getOutputFileNameString(inlineFile: PrayInlineFile): String? {
        return inlineFile.outputFileName?.text?.stripSurroundingQuotes()
    }

    @JvmStatic
    fun isInt(number: PrayNumber): Boolean {
        return number.int != null
    }

    @JvmStatic
    fun getTagName(tag: PrayPrayTag): String {
        return tag.stub?.tagName ?: tag.tagTagName.stringValue
    }

    @JvmStatic
    fun getTagName(tag: PrayInlineText): String {
        return tag.tagTagName.stringValue
    }

    @JvmStatic
    fun getInputFileNameAsString(tag: PrayInlineText): String? {
        return tag.inputFileName?.stringValue
    }

    @JvmStatic
    fun getTagName(tagTag: PrayTagTagName): String {
        return tagTag.text.stripSurroundingQuotes()
    }

    @JvmStatic
    fun isFloat(number: PrayNumber): Boolean {
        return number.float != null
    }

    @JvmStatic
    fun isNumberValue(tag: PrayPrayTag): Boolean {
        return tag.stub?.isInt ?: (tag.tagTagValue.number != null)
    }

    @JvmStatic
    fun isStringValue(tag: PrayPrayTag): Boolean {
        return tag.stub?.isString ?: (tag.tagTagValue.string != null)
    }

    @JvmStatic
    fun isNumberValue(@Suppress("UNUSED_PARAMETER") tag: PrayInlineText): Boolean {
        return false
    }


    @JvmStatic
    fun getValueAsInt(@Suppress("UNUSED_PARAMETER") tag: PrayInlineText): Int? {
        return null
    }

    @JvmStatic
    fun getValueAsString(tag: PrayInlineText): String? {
        return tag.inputFileNameAsString
    }

    @JvmStatic
    fun isStringValue(@Suppress("UNUSED_PARAMETER") tag: PrayInlineText): Boolean {
        return true
    }

    @JvmStatic
    fun isNumberValue(tag: PrayTagTagValue): Boolean {
        return tag.number != null
    }

    @JvmStatic
    fun isStringValue(tag: PrayTagTagValue): Boolean {
        return tag.string != null
    }



    @JvmStatic
    fun isNumberValue(@Suppress("UNUSED_PARAMETER") element: PrayInputFileName): Boolean {
        return false
    }

    @JvmStatic
    fun isStringValue(@Suppress("UNUSED_PARAMETER") element: PrayInputFileName): Boolean {
        return true
    }

    @JvmStatic
    fun getValueAsInt(@Suppress("UNUSED_PARAMETER") element: PrayInputFileName): Int? {
        return null
    }

    @JvmStatic
    fun getValueAsString(element: PrayInputFileName): String? {
        val text = element.text
        if (text.startsWith('"') || text.endsWith('\''))
            return element.text.stripSurroundingQuotes()
        return null
    }

    @JvmStatic
    fun getName(element: PrayTagValue): String? {
        return element.valueAsString
    }

    @JvmStatic
    fun setName(element: PrayTagValue): PsiElement {
        return element
    }

    @JvmStatic
    fun getBlockTagString(block: PrayAgentBlock): String? {
        return block.stub?.blockTag ?: block.blockHeader
            .blockTag
            ?.text
    }

    @JvmStatic
    fun getBlockTagString(header: PrayBlockHeader): String? {
        return header.blockTag?.text
    }

    @JvmStatic
    fun getBlockNameString(header: PrayBlockHeader): String? {
        return header.blockName?.text
    }

    @JvmStatic
    fun getBlockNameString(block: PrayAgentBlock): String? {
        return block.stub?.blockName ?: block.blockHeader
            .blockName
            ?.text
            ?.stripSurroundingQuotes()
    }

    @JvmStatic
    fun getTagStructs(block: PrayAgentBlock): List<PrayTagStruct<*>> {
        return block.stub?.tags ?: block
            .blockElementList
            .mapNotNull map@{ element ->
                getElementAsTagStruct(element)
            }
    }

    private fun getElementAsTagStruct(element: PrayBlockElement): PrayTagStruct<*>? {
        element.prayTag?.let { tag ->
            return getElementAsTagStruct(tag)
        }
        element.inlineText?.let { tag ->
            return getElementAsTagStruct(tag)
        }
        return null
    }

    private fun getElementAsTagStruct(tag: PrayPrayTag): PrayTagStruct<*>? {
        val tagName = tag.tagName.nullIfEmpty()
            ?: return null
        return if (tag.isNumberValue) {
            val value = tag.valueAsInt
                ?: return null
            PrayTagStruct.IntTag(
                tag = tagName,
                value = value,
                indexInFile = tag.startOffset
            )
        } else {
            val value = tag.valueAsString
                ?: return null
            PrayTagStruct.StringTag(
                tag = tagName,
                value = value,
                indexInFile = tag.startOffset
            )
        }
    }

    private fun getElementAsTagStruct(tag: PrayInlineText): PrayTagStruct<*>? {
        val tagName = tag.tagName.nullIfEmpty()
            ?: return null
        val value = tag.inputFileNameAsString
            ?: return null
        return PrayTagStruct.InliningTag(
            tag = tagName,
            value = value,
            indexInFile = tag.startOffset
        )
    }

    @JvmStatic
    fun getName(element: PrayString): String {
        return element.stringValue
    }

    @JvmStatic
    fun isClosed(element: PrayString): Boolean {
        val text = element.text
        if (text.length < 2)
            return false
        return text[0] == text.last()
    }

    @JvmStatic
    fun setName(element: PrayString, name: String): PsiElement {
        val newElement = PrayPsiElementFactory.createQuoteString(element.project, name)
            ?: return element
        return element.replace(newElement)
    }


    @JvmStatic
    fun getReference(element: PrayString): PrayQuoteStringReference {
        return PrayQuoteStringReference(element)
    }

    @JvmStatic
    fun isClosed(@Suppress("UNUSED_PARAMETER") element: PrayIncompleteString): Boolean {
        return false
    }
}