@file:Suppress("SpellCheckingInspection")

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

<valuesListName>@OiTo</valuesListName><valuesListType>:BitFlags</valuesListType> {
	<valuesListKey>0</valuesListKey> = <valuesListValueName>No one</valuesListValueName>
    # Animals
	<valuesListKey>2</valuesListKey> = <valuesListValueName>Kitty</valuesListValueName>
	<valuesListKey>4</valuesListKey> = <valuesListValueName>The fishies</valuesListValueName> - <valuesListValueDescription>in the deep blue <valuesListCommandLink>[sea]</valuesListCommandLink></valuesListValueDescription>
    #People
    <valuesListKey>8</valuesListKey> = <valuesListValueName>Haji</valuesListValueName> - <valuesListValueDescription>a punk just like any other boy</valuesListValueDescription>
    <valuesListKey>16</valuesListKey> = <valuesListValueName>Skins</valuesListValueName> - <valuesListValueDescription>Trevor and his gang</valuesListValueDescription>
	<valuesListKey>8</valuesListKey> = <valuesListValueName>The World</valuesListValueName>
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
                AttributesDescriptor("Command Link", CaosDefSyntaxHighlighter.DOC_COMMENT_WORD_LINK),
                AttributesDescriptor("Comment", CaosDefSyntaxHighlighter.DOC_COMMENT),
                AttributesDescriptor("Comment Variable Type", CaosDefSyntaxHighlighter.DOC_COMMENT_VARIABLE_TYPE),
                AttributesDescriptor("Comment @Tag", CaosDefSyntaxHighlighter.DOC_COMMENT_TAG),
                AttributesDescriptor("Comment #Hashtag", CaosDefSyntaxHighlighter.DOC_COMMENT_HASHTAG),
                AttributesDescriptor("Variable Link", CaosDefSyntaxHighlighter.DOC_COMMENT_VARIABLE_LINK),
                AttributesDescriptor("Type Link", CaosDefSyntaxHighlighter.DOC_COMMENT_TYPE_LINK),
                AttributesDescriptor("Value List Name", CaosDefSyntaxHighlighter.VALUES_LIST_NAME),
                AttributesDescriptor("Value List Type", CaosDefSyntaxHighlighter.VALUES_LIST_TYPE),
                AttributesDescriptor("Value List Key", CaosDefSyntaxHighlighter.VALUES_LIST_VALUE_KEY),
                AttributesDescriptor("Value List Value", CaosDefSyntaxHighlighter.VALUES_LIST_VALUE_NAME),
                AttributesDescriptor("Value List Value Description", CaosDefSyntaxHighlighter.VALUES_LIST_VALUE_DESCRIPTION),
                AttributesDescriptor("In List Command Link", CaosDefSyntaxHighlighter.VALUES_LIST_WORD_LINK),
                AttributesDescriptor("Region Header", CaosDefSyntaxHighlighter.REGION_HEADER),
                AttributesDescriptor("Variable Name", CaosDefSyntaxHighlighter.VARIABLE_NAME),
                AttributesDescriptor("Variable Type", CaosDefSyntaxHighlighter.VARIABLE_TYPE)
        )

        private val XMLDESCRIPTORS: HashMap<String, TextAttributesKey> = hashMapOf(
                "command" to CaosDefSyntaxHighlighter.COMMAND,
                "commandLink" to CaosDefSyntaxHighlighter.DOC_COMMENT_WORD_LINK,
                "typesLink" to CaosDefSyntaxHighlighter.DOC_COMMENT_TYPE_LINK,
                "variableName" to CaosDefSyntaxHighlighter.VARIABLE_NAME,
                "variableType" to CaosDefSyntaxHighlighter.VARIABLE_TYPE,
                "commentVariableType" to CaosDefSyntaxHighlighter.DOC_COMMENT_VARIABLE_TYPE,
                "variableLink" to CaosDefSyntaxHighlighter.DOC_COMMENT_VARIABLE_LINK,
                "valuesListCommandLink" to CaosDefSyntaxHighlighter.VALUES_LIST_WORD_LINK,
                "valuesListName" to CaosDefSyntaxHighlighter.VALUES_LIST_NAME,
                "valuesListType" to CaosDefSyntaxHighlighter.VALUES_LIST_TYPE,
                "valuesListKey" to CaosDefSyntaxHighlighter.VALUES_LIST_VALUE_KEY,
                "valuesListValueName" to CaosDefSyntaxHighlighter.VALUES_LIST_VALUE_NAME,
                "valuesListValueDescription" to CaosDefSyntaxHighlighter.VALUES_LIST_VALUE_DESCRIPTION,
                "valuesListCommandLink" to CaosDefSyntaxHighlighter.VALUES_LIST_WORD_LINK
        )
    }
}
