package com.openc2e.plugins.intellij.caos.formatting;

import com.openc2e.plugins.intellij.caos.psi.util.next
import com.intellij.formatting.Block
import com.intellij.formatting.Spacing
import com.intellij.lang.ASTNode
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.formatter.common.AbstractBlock
import com.openc2e.plugins.intellij.caos.lexer.CaosScriptTypes
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptEqOp
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptExpectsValueOfType
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptExpression
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptIsCommandToken
import java.util.logging.Logger

internal val LOGGER: Logger by lazy {
    Logger.getLogger("#CaosScriptSpacingProcessor")
}

class CaosScriptSpacingProcessor(private val myNode: ASTNode, private val mySettings: CommonCodeStyleSettings) {

    private val noneSpace by lazy { Spacing.createSpacing(0, 0, 0, mySettings.KEEP_LINE_BREAKS, 0) }
    private val oneSpace by lazy { Spacing.createSpacing(1, 1, 0, mySettings.KEEP_LINE_BREAKS, 0) }
    private val anySpace by lazy { Spacing.createSpacing(0, 1, 0, mySettings.KEEP_LINE_BREAKS, 0) }

    fun getSpacing(child1: Block?, child2: Block?): Spacing? {
        if (child1 !is AbstractBlock || child2 !is AbstractBlock) {
            return null
        }
        val node1 = child1.node
        val type1 = node1.elementType
        val node2 = child2.node
        val type2 = node2.elementType
        if (node1.next?.text.orEmpty().contains("\n")) {
            val lineFeeds = if (mySettings.KEEP_LINE_BREAKS) 1 else 0
            return Spacing.createSpacing(0, 0, lineFeeds, mySettings.KEEP_LINE_BREAKS, 0)
        }
        if (type1 == CaosScriptTypes.CaosScript_COMMA || type2 == CaosScriptTypes.CaosScript_COMMA)
            return noneSpace

        return when (node1.psi) {
            is CaosScriptExpectsValueOfType -> spaceAfterIsValueOfType(node2)
            is CaosScriptIsCommandToken -> spaceAfterIsCommandToken(node2)
            is CaosScriptExpression -> oneSpace
            is CaosScriptEqOp -> oneSpace
            else -> anySpace
        }

    }

    private fun spaceAfterIsCommandToken(node2: ASTNode): Spacing? {
        if (node2.elementType == CaosScriptTypes.CaosScript_COMMA) {
            return Spacing.createSpacing(0, 0, 0, false, 0)
        }
        if (node2.psi is CaosScriptExpectsValueOfType) {
            return Spacing.createSpacing(1, 1, 0, false, 0)
        }
        return null
    }

    private fun spaceAfterIsValueOfType(node2: ASTNode): Spacing? {
        if (node2.elementType == CaosScriptTypes.CaosScript_COMMA) {
            return Spacing.createSpacing(0, 0, 0, false, 0)
        }
        if (node2.psi is CaosScriptExpectsValueOfType) {
            return Spacing.createSpacing(1, 1, 0, false, 0)
        }
        return null
    }
}