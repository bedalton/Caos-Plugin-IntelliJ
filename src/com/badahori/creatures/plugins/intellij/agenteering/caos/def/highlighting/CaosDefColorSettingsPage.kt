package com.badahori.creatures.plugins.intellij.agenteering.caos.def.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import icons.CaosScriptIcons
import javax.swing.Icon

class CaosDefColorSettingsPage : ColorSettingsPage {

    override fun getIcon(): Icon? {
        return CaosScriptIcons.CAOS_DEF_FILE_ICON
    }

    override fun getHighlighter(): SyntaxHighlighter {
        return CaosDefSyntaxHighlighter()
    }

    override fun getDemoText(): String {
        return """ 
/*
 * Play sound effect after a short {<variableLink>delay</variableLink>} if <commandLink>[TARG]</commandLink> object is visible on screen
 * Change volume according to distance from screen
 * #Sounds
 *
 * @param <variableLink>{filename}</variableLink> (<commentVariableType>token</commentVariableType><typeLink>@File.WAV</typeLink>) - 4 letter sound token
 * @param <variableLink>{delay}</variableLink> (<commentVariableType>integer</commentVariableType>)
 *
 */
<command>SNDQ</command> (<variableType>command</variableType>) <variableName>filename</variableName> (<variableType>token</variableType>) <variableName>delay</variableName> (<variableType>integer</variableType>);

<listName>@OiTo</listName> {
	<listKey>0</listKey> = <listValue>No one</listValue>
    # Animals
	<listKey>2</listKey> = <listValue>Kitty</listValue>
	<listKey>4</listKey> = <listValue>The fishies - <listValueDescription>in the deep blue <listCommandLink>[sea]</listCommandLink></listValueDescription>
    #People
    <listKey>8</listKey> = <listValue>Haji</listValue> - <listValueDescription>a punk just like any other boy</listValueDescription>
    <listKey>16</listKey> = <listValue>Skins</listValue> - <listValueDescription>Trevor and his gang</listValueDescription>
	<listKey>8</listKey> = <listValue>The World</listValue>
}
        """
    }

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> {
        return DESCRIPTORS
    }

    override fun getColorDescriptors(): Array<ColorDescriptor> {
        return ColorDescriptor.EMPTY_ARRAY
    }

    override fun getDisplayName(): String {
        return "Caos Command Definitions"
    }

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? {
        return XMLDESCRIPTORS
    }

    companion object {
        private val DESCRIPTORS = arrayOf(
                AttributesDescriptor("Code Block", CaosDefSyntaxHighlighter.CODE_BLOCK),
                AttributesDescriptor("Command", CaosDefSyntaxHighlighter.COMMAND),
                AttributesDescriptor("Command Link", CaosDefSyntaxHighlighter.WORD_LINK),
                AttributesDescriptor("Comment", CaosDefSyntaxHighlighter.COMMENT),
                AttributesDescriptor("Comment Variable Type", CaosDefSyntaxHighlighter.COMMENT_VARIABLE_TYPE),
                AttributesDescriptor("Comment @Tag", CaosDefSyntaxHighlighter.COMMENT_TAG),
                AttributesDescriptor("Comment #Hashtag", CaosDefSyntaxHighlighter.DOC_COMMENT_HASHTAG),
                AttributesDescriptor("Variable Link", CaosDefSyntaxHighlighter.VARIABLE_LINK),
                AttributesDescriptor("Type Link", CaosDefSyntaxHighlighter.TYPE_LINK),
                AttributesDescriptor("Value List Name", CaosDefSyntaxHighlighter.TYPE_DEF_NAME),
                AttributesDescriptor("Value List Key", CaosDefSyntaxHighlighter.TYPE_DEF_KEY),
                AttributesDescriptor("Value List Value", CaosDefSyntaxHighlighter.TYPE_DEF_VALUE),
                AttributesDescriptor("Value List Value Description", CaosDefSyntaxHighlighter.TYPE_DEF_VALUE_DESCRIPTION),
                AttributesDescriptor("In List Command Link", CaosDefSyntaxHighlighter.TYPE_DEF_WORD_LINK),
                AttributesDescriptor("Region Header", CaosDefSyntaxHighlighter.REGION_HEADER),
                AttributesDescriptor("Variable Name", CaosDefSyntaxHighlighter.VARIABLE_NAME),
                AttributesDescriptor("Variable Type", CaosDefSyntaxHighlighter.VARIABLE_TYPE)
        )

        private val XMLDESCRIPTORS: HashMap<String, TextAttributesKey> = hashMapOf(
                "command" to CaosDefSyntaxHighlighter.COMMAND,
                "commandLink" to CaosDefSyntaxHighlighter.WORD_LINK,
                "typeDefCommandLink" to CaosDefSyntaxHighlighter.TYPE_DEF_WORD_LINK,
                "listName" to CaosDefSyntaxHighlighter.TYPE_DEF_NAME,
                "listKey" to CaosDefSyntaxHighlighter.TYPE_DEF_KEY,
                "typeLink" to CaosDefSyntaxHighlighter.TYPE_LINK,
                "variableName" to CaosDefSyntaxHighlighter.VARIABLE_NAME,
                "variableType" to CaosDefSyntaxHighlighter.VARIABLE_TYPE,
                "commentVariableType" to CaosDefSyntaxHighlighter.COMMENT_VARIABLE_TYPE,
                "variableLink" to CaosDefSyntaxHighlighter.VARIABLE_LINK,
                "listValue" to CaosDefSyntaxHighlighter.TYPE_DEF_VALUE,
                "listValueDescription" to CaosDefSyntaxHighlighter.TYPE_DEF_VALUE_DESCRIPTION,
                "listCommandLink" to CaosDefSyntaxHighlighter.TYPE_DEF_WORD_LINK
        )
    }
}
