package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.highlighting

import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lexer.PrayTypes.*
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.lexer.PrayLexerAdapter
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptLexerAdapter
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTempTextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import org.w3c.dom.Text
import java.awt.Color

class  PraySyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer {
        return PrayLexerAdapter()
    }

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        if (tokenType == null) {
            return EMPTY_KEYS
        }
        val attrKey: TextAttributesKey? = when (tokenType) {
            Pray_LANGUAGE_STRING -> STRING
            Pray_AT -> AT_SYMBOL
            Pray_DOUBLE_QUO_STRING -> STRING
            Pray_SINGLE_QUO_STRING -> STRING
            Pray_BLOCK_TAG_LITERAL -> BLOCK_TAG
            Pray_INT, Pray_FLOAT -> NUMBER
            Pray_BLOCK_COMMENT -> BLOCK_COMMENT
            Pray_LINE_COMMENT -> LINE_COMMENT
            Pray_INLINE -> KEYWORDS
            Pray_GROUP -> KEYWORDS
            else -> null
        }
        return if (attrKey != null) arrayOf(attrKey) else EMPTY_KEYS
    }

    companion object {
        private val EMPTY_KEYS:Array<TextAttributesKey> = arrayOf()
//        @JvmStatic
//        val ID:TextAttributesKey = createTextAttributesKey("PRAY_ID", DefaultLanguageHighlighterColors.IDENTIFIER)
        @JvmStatic
        val KEYWORDS:TextAttributesKey = createTextAttributesKey("PRAY_KEYWORDS", DefaultLanguageHighlighterColors.KEYWORD)
        @JvmStatic
        val STRING:TextAttributesKey = createTextAttributesKey("PRAY_STRING", DefaultLanguageHighlighterColors.STRING)
        @JvmStatic
        val AT_SYMBOL: TextAttributesKey = createTextAttributesKey("PRAY_AT_SYMBOL", KEYWORDS)
        @JvmStatic
        val NUMBER: TextAttributesKey = createTextAttributesKey("PRAY_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
        @JvmStatic
        val CUSTOM_TAG: TextAttributesKey = createTextAttributesKey("PRAY_TAG_CUSTOM", DefaultLanguageHighlighterColors.REASSIGNED_PARAMETER)
        @JvmStatic
        val OFFICIAL_TAG: TextAttributesKey = createTextAttributesKey("PRAY_TAG_OFFICIAL", DefaultLanguageHighlighterColors.KEYWORD)
        @JvmStatic
        val NUMBERED_TAG: TextAttributesKey = createTextAttributesKey("PRAY_NUMBERED_TAG", DefaultLanguageHighlighterColors.KEYWORD)
        @JvmStatic
        val NUMBERED_TAG_NUMBER: TextAttributesKey = createTextAttributesKey("PRAY_NUMBERED_TAG_NUMBER", DefaultLanguageHighlighterColors.FUNCTION_CALL)
        @JvmStatic
        val BLOCK_TAG: TextAttributesKey = createTextAttributesKey("PRAY_BLOCK_TAG", DefaultLanguageHighlighterColors.CLASS_NAME)
        @JvmStatic
        val BLOCK_NAME: TextAttributesKey = createTextAttributesKey("PRAY_BLOCK_NAME", DefaultLanguageHighlighterColors.INTERFACE_NAME)
        @JvmStatic
        val BLOCK_COMMENT: TextAttributesKey = createTextAttributesKey("PRAY_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT)
        @JvmStatic
        val LINE_COMMENT: TextAttributesKey = createTextAttributesKey("PRAY_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
    }
}

@Suppress("unused")
private val TextAttributesKey.asErrorAttribute:TextAttributesKey get() {
    val attributes = this.defaultAttributes.clone()
    attributes.effectType = EffectType.WAVE_UNDERSCORE
    attributes.effectColor = Color.RED.brighter()
    attributes.errorStripeColor = Color.RED.brighter()
    val name = this.externalName + "_ERROR"
    val temp = createTempTextAttributesKey(name, attributes)
    return createTextAttributesKey(name, temp)
}

private var copyNumber = 1

internal fun TextAttributesKey.copy(nameIn:String? = null) : TextAttributesKey {
    val name = nameIn ?: (this.externalName + "(${copyNumber++})")
    val attributes = this.defaultAttributes.clone()
    val temp = createTempTextAttributesKey(name, attributes)
    return createTextAttributesKey(name, temp)
}

internal infix fun TextAttributesKey.like(other: TextAttributesKey): Boolean {
    return this.defaultAttributes like other.defaultAttributes
}

internal infix fun TextAttributes.like(other: TextAttributes): Boolean {
    if (this.effectColor != other.effectColor)
        return false
    if (this.effectType != other.effectType)
        return false
    if (this.foregroundColor != other.foregroundColor)
        return false
    if (this.backgroundColor != other.backgroundColor)
        return false
    if (this.errorStripeColor != other.errorStripeColor)
        return false
    if (this.fontType != other.fontType)
        return false
    return this.flyweight == other.flyweight
}