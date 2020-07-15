package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.intellij.formatting.Indent
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCodeBlock
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptMacro
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getParentOfType

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
        val parent = node.psi.parent
        if (parent is CaosScriptCodeBlock) {
            val parentBlock = parent.parent
            if (parentBlock is CaosScriptMacro || parentBlock is CaosScriptEventScript) {
                return Indent.getNoneIndent()
            }
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