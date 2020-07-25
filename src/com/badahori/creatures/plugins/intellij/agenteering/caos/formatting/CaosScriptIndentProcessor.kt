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
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.previous
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.editor
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.orFalse
import kotlin.math.abs

class CaosScriptIndentProcessor(private val settings: CommonCodeStyleSettings, private val caosSettings: CaosScriptCodeStyleSettings) {
    fun getChildIndent(node: ASTNode): Indent? {
        if (!caosSettings.INDENT_BLOCKS) {
            return Indent.getNoneIndent()
        }
        val elementType = node.elementType
        val firstChild: ASTNode? = node

        // Handle indent if cursor is at block end (ie. NEXT, ENDI)
        if (firstChild?.elementType in CaosScriptTokenSets.BLOCK_ENDS) {
            val cursor = firstChild?.psi?.editor?.caretModel?.offset
                    ?: -1
            // Cursor is actually at token, not just some newline before end
            if (abs(firstChild!!.startOffset - cursor) <= 2) {
                return Indent.getNoneIndent()
            }
        }
        val parent = node.psi.parent
        if (parent is CaosScriptCodeBlock) {
            val parentBlock = parent.parent
            if (parentBlock is CaosScriptMacro || parentBlock is CaosScriptEventScript) {
                return Indent.getNoneIndent()
            }
            return Indent.getNormalIndent()
        } else if (elementType == TokenType.WHITE_SPACE && node.psi?.getParentOfType(CaosScriptCodeBlock::class.java)?.parent?.let {it !is CaosScriptMacro && it !is CaosScriptEventScript}.orFalse()) {
            return Indent.getNormalIndent()
        } else if (node.treeParent?.elementType == CaosScriptTypes.CaosScript_CODE_BLOCK) {
            // Check if block parent is Event Script or Macro
            node.treeParent?.treeParent?.elementType?.let {
                if (it != CaosScriptTypes.CaosScript_MACRO || it != CaosScriptTypes.CaosScript_EVENT_SCRIPT)
                    return Indent.getNormalIndent()
            }
        }
        return Indent.getNoneIndent()
    }
}