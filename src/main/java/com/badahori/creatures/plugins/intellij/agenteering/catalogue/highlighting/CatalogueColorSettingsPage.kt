@file:Suppress("SpellCheckingInspection")

package com.badahori.creatures.plugins.intellij.agenteering.catalogue.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import icons.CaosScriptIcons
import javax.swing.Icon

class CatalogueColorSettingsPage : ColorSettingsPage {

    override fun getIcon(): Icon {
        return CaosScriptIcons.CATALOGUE_FILE_ICON
    }

    override fun getHighlighter(): SyntaxHighlighter {
        return CatalogueSyntaxHighlighter()
    }

    override fun getDemoText(): String {
        return """
            TAG <tag_name>"genera"</tag_name>
            "Norn"
            "Grendel"
            "Ettin"
            "Geat"
            
            # Tag with values that show that strings with only numbers, show up differently
            TAG <tag_name>"tags with numbers"</tag_name>
            "1"
            "One"
            "2"
            "Two"
            
            # Chemical overrides
            ARRAY OVERRIDE <array_name>"chemicals"</array_name> 10
            # 0-10
            "<NOTHING>"
            "Lactate"
            "Pyruvate"
            "Glucose"
            "Glycogen"
            "Starch"
            "Fatty acid"
            "Cholesterol"
            "Triglyceride"
            "Adipose tissue"
        """.trimIndent()
    }

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> {
        return DESCRIPTORS
    }

    override fun getColorDescriptors(): Array<ColorDescriptor> {
        return ColorDescriptor.EMPTY_ARRAY
    }

    override fun getDisplayName(): String {
        return "Catalogue Files"
    }

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> {
        return XMLDESCRIPTORS
    }

    companion object {
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Names", CatalogueSyntaxHighlighter.NAME),
            AttributesDescriptor("Tag Names", CatalogueSyntaxHighlighter.TAG_NAME),
            AttributesDescriptor("Array Names", CatalogueSyntaxHighlighter.ARRAY_NAME),
            AttributesDescriptor("Keywords", CatalogueSyntaxHighlighter.KEYWORD),
            AttributesDescriptor("Strings", CatalogueSyntaxHighlighter.STRING),
            AttributesDescriptor("Int Strings", CatalogueSyntaxHighlighter.INT_STRING),
            AttributesDescriptor("Modifiers", CatalogueSyntaxHighlighter.MODIFIER),
            AttributesDescriptor("Numbers", CatalogueSyntaxHighlighter.NUMBER),
            AttributesDescriptor("Comments", CatalogueSyntaxHighlighter.COMMENT),
        )

        private val XMLDESCRIPTORS: HashMap<String, TextAttributesKey> = hashMapOf(
            "tag_name" to CatalogueSyntaxHighlighter.TAG_NAME,
            "array_name" to CatalogueSyntaxHighlighter.ARRAY_NAME
        )
    }
}
