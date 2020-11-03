package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.intellij.formatting.Block
import com.intellij.formatting.Spacing
import com.intellij.lang.ASTNode
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.formatter.common.AbstractBlock
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.isDirectlyPrecededByNewline
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.next
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import java.util.logging.Logger

internal val LOGGER: Logger by lazy {
    Logger.getLogger("#CaosScriptSpacingProcessor")
}

class CaosScriptSpacingProcessor(private val myNode: ASTNode, private val mySettings: CommonCodeStyleSettings) {

    private val noneSpace by lazy { Spacing.createSpacing(0, 0, 0, mySettings.KEEP_LINE_BREAKS, 0) }
    private val oneSpace by lazy { Spacing.createSpacing(1, 1, 0, mySettings.KEEP_LINE_BREAKS, Int.MAX_VALUE) }
    private val anySpace by lazy { Spacing.createSpacing(0, 1, 0, mySettings.KEEP_LINE_BREAKS, Int.MAX_VALUE) }

    fun getSpacing(child1: Block?, child2: Block?): Spacing? {
        if (child1 !is AbstractBlock || child2 !is AbstractBlock) {
            return noneSpace
        }
        val keepBlankLines = 2
        val type = myNode.elementType
        val node1 = child1.node
        val type1 = node1.elementType
        val node2 = child2.node
        val type2 = node2.elementType
        val types = listOf(type, type1, type2)

        if (commaTypes.intersect(types).isNotEmpty())
            return noneSpace
        if (myNode.next?.isDirectlyPrecededByNewline().orFalse())
            return Spacing.createSpacing(0, 0, 0, mySettings.KEEP_LINE_BREAKS, keepBlankLines)
        if (node2.isDirectlyPrecededByNewline())
            return Spacing.createSpacing(0, 0, 0, mySettings.KEEP_LINE_BREAKS, keepBlankLines)
        if (node1.next?.text.orEmpty().contains("\n")) {
            val lineFeeds = if (mySettings.KEEP_LINE_BREAKS) 1 else 0
            return Spacing.createSpacing(0, 0, lineFeeds, mySettings.KEEP_LINE_BREAKS, keepBlankLines)
        }
        return when (node1.psi) {
            is CaosScriptExpectsValueOfType -> spaceAfterIsValueOfType(node2,keepBlankLines)
            is CaosScriptIsCommandToken -> spaceAfterIsCommandToken(node2,keepBlankLines)
            is CaosScriptLiteral -> oneSpace
            is CaosScriptEqOp -> oneSpace
            else -> anySpace
        }

    }

    private fun spaceAfterIsCommandToken(node2: ASTNode, keepBlankLines:Int): Spacing? {
        if (node2.elementType in commaTypes) {
            return Spacing.createSpacing(0, 0, 0, false, keepBlankLines)
        }
        if (node2.psi is CaosScriptExpectsValueOfType) {
            return Spacing.createSpacing(1, 1, 0, false, keepBlankLines)
        }
        return null
    }

    private fun spaceAfterIsValueOfType(node2: ASTNode, keepBlankLines:Int): Spacing? {
        if (node2.elementType in commaTypes || node2.isDirectlyPrecededByNewline()) {
            return Spacing.createSpacing(0, 0, 0, false, keepBlankLines)
        }
        if (node2.psi is CaosScriptExpectsValueOfType) {
            return Spacing.createSpacing(1, 1, 0, false, keepBlankLines)
        }
        return null
    }

    companion object {
        val commaTypes = listOf(CaosScriptTypes.CaosScript_COMMA, CaosScriptTypes.CaosScript_SYMBOL_COMMA, CaosScriptTypes.CaosScript_SPACE_LIKE_OR_NEWLINE)
    }
}