@file:Suppress("SpellCheckingInspection")

package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import icons.CaosScriptIcons
import javax.swing.Icon

class PrayColorSettingsPage : ColorSettingsPage {

    override fun getIcon(): Icon? {
        return CaosScriptIcons.CAOS_FILE_ICON
    }

    override fun getHighlighter(): SyntaxHighlighter {
        return PraySyntaxHighlighter()
    }

    override fun getDemoText(): String {
        return """
        "en-GB"
        
        (- 
            A Block Comment 
            that spans more than one line 
        -)
        
        # A single line comment
       
        group DSAG <blockName>"An Agent"</blockName>

        <officialTag>"Agent Type"</officialTag> 0
        <officialTag>"Agent Description"</officialTag> "A short description of the agent"
        <customTag>"Custom Tag"</customTag> "Hello Panda!"
        <customTag>"Another custom tag"</customTag> 113
        
        # Scripts
        <officialTag>"Script Count"</officialTag> 1
        <numberedTag>"Script <tagNumber>1</tagNumber>"</numberedTag> @ "Pika-Pika!.cos"
        
        # Dependencies
        <officialTag>"Dependency Count"</officialTag> 1
        <numberedTag>"Dependency Category <tagNumber>1</tagNumber>"</numberedTag> 2
        <numberedTag>"Dependency <tagNumber>1</tagNumber>"</numberedTag> "Pika!.c16"
        
        inline FILE <blockName>"Pika!.c16"</blockName> "Pika!.c16"
        inline GENE <blockName>"my-highlander.gen"</blockName> "highlander.gen"
        """.trimIndent()
    }

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> {
        return DESCRIPTORS
    }

    override fun getColorDescriptors(): Array<ColorDescriptor> {
        return ColorDescriptor.EMPTY_ARRAY
    }

    override fun getDisplayName(): String {
        return "PRAY"
    }

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? {
        return XMLDESCRIPTORS
    }

    companion object {
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Block Comment", PraySyntaxHighlighter.BLOCK_COMMENT),
            AttributesDescriptor("Line Comment", PraySyntaxHighlighter.LINE_COMMENT),
            AttributesDescriptor("Keywords", PraySyntaxHighlighter.KEYWORDS),
            AttributesDescriptor("Block Tag", PraySyntaxHighlighter.BLOCK_TAG),
            AttributesDescriptor("Block Name", PraySyntaxHighlighter.BLOCK_NAME),
            AttributesDescriptor("Official Tag", PraySyntaxHighlighter.OFFICIAL_TAG),
            AttributesDescriptor("User Defined Tag", PraySyntaxHighlighter.CUSTOM_TAG),
            AttributesDescriptor("Numbered Tag", PraySyntaxHighlighter.NUMBERED_TAG),
            AttributesDescriptor("Numbered Tag's Number", PraySyntaxHighlighter.NUMBERED_TAG_NUMBER),
            AttributesDescriptor("Strings", PraySyntaxHighlighter.STRING),
            AttributesDescriptor("Numbers", PraySyntaxHighlighter.NUMBER),
            AttributesDescriptor("@", PraySyntaxHighlighter.AT_SYMBOL),
        )

        private val XMLDESCRIPTORS: HashMap<String, TextAttributesKey> = hashMapOf(
            "officialTag" to PraySyntaxHighlighter.OFFICIAL_TAG,
            "customTag" to PraySyntaxHighlighter.CUSTOM_TAG,
            "blockName" to PraySyntaxHighlighter.BLOCK_NAME,
            "numberedTag" to PraySyntaxHighlighter.NUMBERED_TAG,
            "tagNumber" to PraySyntaxHighlighter.NUMBERED_TAG_NUMBER
        )
    }
}
