package com.openc2e.plugins.intellij.caos.formatting

import com.intellij.formatting.Indent
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.openc2e.plugins.intellij.caos.lexer.CaosScriptTypes
import com.openc2e.plugins.intellij.caos.psi.api.*
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptTokenSets
import com.openc2e.plugins.intellij.caos.psi.util.getParentOfType
import com.openc2e.plugins.intellij.caos.psi.util.previous

class CaosScriptIndentProcessor(private val settings: CommonCodeStyleSettings, private val caosSettings: CaosScriptCodeStyleSettings) {
    fun getChildIndent(node: ASTNode): Indent? {
        if (!caosSettings.INDENT_BLOCKS)
            return Indent.getNoneIndent()
        val elementType = node.elementType
        var firstChild: ASTNode? = node
        /*while (firstChild != null && firstChild.elementType !in CaosScriptTokenSets.BLOCK_ENDS) {
            firstChild = firstChild.firstChildNode
        }*/
        if (firstChild?.elementType in CaosScriptTokenSets.BLOCK_ENDS) {
            return Indent.getNoneIndent()
        }
        if (elementType == CaosScriptTypes.CaosScript_CODE_BLOCK_LINE) {
            val parentBlock = node.psi.parent?.parent
            if (parentBlock is CaosScriptMacro || parentBlock is CaosScriptEventScript)
                return Indent.getNoneIndent()
            return Indent.getNormalIndent()
        } else if (elementType == TokenType.WHITE_SPACE && node.psi?.getParentOfType(CaosScriptCodeBlock::class.java)?.parent !is CaosScriptMacro) {
            return Indent.getNormalIndent()
        } else if (node.treeParent?.elementType == CaosScriptTypes.CaosScript_CODE_BLOCK) {
            // Check if block parent is Event Script or Macro
            node.treeParent?.treeParent?.elementType?.let {
                if (it == CaosScriptTypes.CaosScript_MACRO || it == CaosScriptTypes.CaosScript_EVENT_SCRIPT)
                    return Indent.getNormalIndent()
            }
        }
        return Indent.getNoneIndent()
    }
}