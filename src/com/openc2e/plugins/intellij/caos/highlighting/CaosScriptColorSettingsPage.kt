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
<command>new: simp</command> <token>heli</token> 1 0 0 0
<command>clas</command> 1234567
<command>slim</command>
doif var1 <eqOp>eq</eqOp> 10
    <command>setv</command> obv1 12
    <command>tick</command> 50
    <command>sndv</command> [heli.wav]
    <command>base</command> 4 <command>anim</command> <anim>[0123R]</anim>
    enum 4 1 0
        <command>chem</command> 57 10
        <command>gsub</gsub> <subroutine>sbrt</subroutine> 
    next
else
    <command>addv</command> obv1 1
    <command>setv</command> <lvalue>baby</lvalue> <rvalue>tokn</rvalue> <token>mum1</token>
endi
endm
* An example of a event script
scrp 2 3 6 9
    <command>dde: puts</command> [Testing... 1,2,3]
    <command>subv</command> mv00 18
    doif mv00 <symbol>></symbol> 0
        <command>tick</command> 0
        <keyword>stop</keyword>
        <command>setv</command> <lvalue>ob20</lvalue> <rvalue>driv</rvalue> 5
    else
        <command>anim</command> <anim>[10 11 12 13]</anim>
    endi
    <command>tick</command> mv00
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
                AttributesDescriptor("Token Literals", CaosScriptSyntaxHighlighter.TOKEN),
                AttributesDescriptor("Keywords", CaosScriptSyntaxHighlighter.KEYWORDS),
                AttributesDescriptor("Line Comment", CaosScriptSyntaxHighlighter.COMMENT),
                AttributesDescriptor("Var Tokens", CaosScriptSyntaxHighlighter.VAR_TOKEN),
                AttributesDescriptor("Commands", CaosScriptSyntaxHighlighter.COMMAND_TOKEN),
                AttributesDescriptor("Right Value Keywords", CaosScriptSyntaxHighlighter.RVALUE_TOKEN),
                AttributesDescriptor("Left Value Keywords", CaosScriptSyntaxHighlighter.LVALUE_TOKEN),
                AttributesDescriptor("Numbers", CaosScriptSyntaxHighlighter.NUMBER),
                AttributesDescriptor("Equals Keywords", CaosScriptSyntaxHighlighter.EQ_OP_KEYWORD),
                AttributesDescriptor("Equals Symbols", CaosScriptSyntaxHighlighter.SYMBOL),
                AttributesDescriptor("Subroutine Name", CaosScriptSyntaxHighlighter.SUBROUTINE_NAME)
        )

        private val XMLDESCRIPTORS: HashMap<String, TextAttributesKey> = hashMapOf(
                "keyword" to CaosScriptSyntaxHighlighter.KEYWORDS,
                "token" to CaosScriptSyntaxHighlighter.TOKEN,
                "anim" to CaosScriptSyntaxHighlighter.ANIMATION,
                "rvalue" to CaosScriptSyntaxHighlighter.RVALUE_TOKEN,
                "lvalue" to CaosScriptSyntaxHighlighter.LVALUE_TOKEN,
                "command" to CaosScriptSyntaxHighlighter.COMMAND_TOKEN,
                "symbol" to CaosScriptSyntaxHighlighter.SYMBOL,
                "eqOp" to CaosScriptSyntaxHighlighter.EQ_OP_KEYWORD,
                "subroutine" to CaosScriptSyntaxHighlighter.SUBROUTINE_NAME
        )
    }
}
