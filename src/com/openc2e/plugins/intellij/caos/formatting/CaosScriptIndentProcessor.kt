package com.openc2e.plugins.intellij.caos.formatting

import com.intellij.formatting.ChildAttributes
import com.intellij.formatting.Indent
import com.intellij.lang.ASTNode
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.openc2e.plugins.intellij.caos.lexer.CaosScriptTypes
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCodeBlock
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptMacro
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptTokenSets
import com.openc2e.plugins.intellij.caos.psi.util.getParentOfType
import com.openc2e.plugins.intellij.caos.psi.util.getPreviousNonEmptyNode

class CaosScriptIndentProcessor(private val settings: CommonCodeStyleSettings, private val caosSettings:CaosScriptCodeStyleSettings) {
    fun getChildIndent(node: ASTNode): Indent? {
        if (!caosSettings.INDENT_BLOCKS)
            return Indent.getNoneIndent()
        val elementType = node.elementType
        var firstChild:ASTNode? = node
        while (firstChild != null && firstChild.elementType !in CaosScriptTokenSets.BLOCK_ENDS) {
            firstChild = firstChild.firstChildNode
        }
        if (firstChild?.elementType in CaosScriptTokenSets.BLOCK_ENDS)
            return Indent.getNoneIndent()
        if (node.treeParent?.elementType == CaosScriptTypes.CaosScript_CODE_BLOCK && node.treeParent?.treeParent?.elementType != CaosScriptTypes.CaosScript_MACRO && node.treeParent?.treeParent?.elementType != CaosScriptTypes.CaosScript_EVENT_SCRIPT)
            return Indent.getNormalIndent()
        return Indent.getNoneIndent()
    }
}