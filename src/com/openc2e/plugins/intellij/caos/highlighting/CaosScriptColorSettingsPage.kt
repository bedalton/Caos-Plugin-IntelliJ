package com.openc2e.plugins.intellij.caos.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.openc2e.plugins.intellij.caos.lang.CaosScriptIcons
import javax.swing.Icon

class CaosScriptColorSettingsPage : ColorSettingsPage {

    override fun getIcon(): Icon? {
        return CaosScriptIcons.CAOS_FILE_ICON
    }

    override fun getHighlighter(): SyntaxHighlighter {
        return CaosScriptSyntaxHighlighter()
    }

    override fun getDemoText(): String {
        return """* An example of an inst(ant) macro script
<keyword>inst</keyword>
new: simp <token>heli</token> 1 0 0 0
clas 1234567
doif var1 eq 10
    setv obv1 12
    tick 50
    sndv [heli.wav]
    base 4 anim <anim>[0123R]</anim>
    enum 4 1 0
        chem 57 10
    next
endi
slim
endm
* An example of a event script
scrp 2 3 6 9
    dbg: outs "Testing... 1,2,3"
    subv mv00 18
    doif mv00 <> 0
        tick 0
        stop
    else
        anim <anim>[10 11 12 13]</anim>
    endi
    tick mv00
endm
        """
    }

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> {
        return DESCRIPTORS
    }

    override fun getColorDescriptors(): Array<ColorDescriptor> {
        return ColorDescriptor.EMPTY_ARRAY
    }

    override fun getDisplayName(): String {
        return "Caos Script"
    }

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? {
        return XMLDESCRIPTORS
    }

    companion object {
        private val DESCRIPTORS = arrayOf(
                AttributesDescriptor("Animations", CaosScriptSyntaxHighlighter.ANIMATION),
                AttributesDescriptor("Strings", CaosScriptSyntaxHighlighter.STRING),
                AttributesDescriptor("Literal Token", CaosScriptSyntaxHighlighter.TOKEN),
                AttributesDescriptor("Keywords", CaosScriptSyntaxHighlighter.KEYWORDS),
                AttributesDescriptor("Line Comment", CaosScriptSyntaxHighlighter.COMMENT),
                AttributesDescriptor("Var Tokens", CaosScriptSyntaxHighlighter.VAR_TOKEN),
                AttributesDescriptor("Commands", CaosScriptSyntaxHighlighter.WORD_TOKEN),
                AttributesDescriptor("Numbers", CaosScriptSyntaxHighlighter.NUMBER),
                AttributesDescriptor("Equals Keywords", CaosScriptSyntaxHighlighter.EQ_OP_KEYWORD),
                AttributesDescriptor("Equals Symbols", CaosScriptSyntaxHighlighter.SYMBOL)
        )

        private val XMLDESCRIPTORS: HashMap<String, TextAttributesKey> = hashMapOf(
                "keyword" to CaosScriptSyntaxHighlighter.KEYWORDS,
                "token" to CaosScriptSyntaxHighlighter.TOKEN,
                "anim" to CaosScriptSyntaxHighlighter.ANIMATION

        )
    }
}
