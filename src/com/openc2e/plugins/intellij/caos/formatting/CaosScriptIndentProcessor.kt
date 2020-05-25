package com.openc2e.plugins.intellij.caos.formatting

import com.intellij.formatting.Indent
import com.intellij.lang.ASTNode
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.openc2e.plugins.intellij.caos.lexer.CaosScriptTypes

class CaosScriptIndentProcessor(private val settings: CommonCodeStyleSettings, private val caosSettings:CaosScriptCodeStyleSettings) {
    fun getChildIndent(node: ASTNode): Indent? {
        if (!caosSettings.INDENT_BLOCKS)
            return Indent.getNoneIndent()
        val elementType = node.elementType
        if (elementType != CaosScriptTypes.CaosScript_CODE_BLOCK_LINE)
            return Indent.getNoneIndent()
        val parent = node.treeParent
        return when (parent?.treeParent?.elementType) {
            CaosScriptTypes.CaosScript_EVENT_SCRIPT -> Indent.getNoneIndent()
            CaosScriptTypes.CaosScript_MACRO -> Indent.getNoneIndent()
            else -> Indent.getNormalIndent()
        }
    }
}