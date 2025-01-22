package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEqOp
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptHasCodeBlock
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptIsCommandToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRvalue
import com.badahori.creatures.plugins.intellij.agenteering.utils.isDirectlyPrecededByNewline
import com.badahori.creatures.plugins.intellij.agenteering.utils.lineNumber
import com.badahori.creatures.plugins.intellij.agenteering.utils.next
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.formatting.Block
import com.intellij.formatting.Spacing
import com.intellij.lang.ASTNode
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.IElementType
import java.util.logging.Logger
import kotlin.math.max

internal val LOGGER: Logger by lazy {
    Logger.getLogger("#CaosScriptSpacingProcessor")
}

class CaosScriptSpacingProcessor(
    private var variant: CaosVariant?,
    private val myNode: ASTNode,
    mySettings: CommonCodeStyleSettings,
    private val caosSettings: CaosScriptCodeStyleSettings?,
) {

    private val keepLineBreaks = mySettings.KEEP_LINE_BREAKS
    private val allowOnSameLine = caosSettings?.ALLOW_MULTIPLE_COMMANDS_ON_SINGLE_LINE.orFalse()
    private val newlineBetween = if (allowOnSameLine) 0 else 1
    private val maxBlankLines by lazy {
        val max = max(caosSettings?.MAX_BLANK_LINES_BETWEEN_COMMANDS ?: 0, 0)
        if (max == 0) {
            100
        } else {
            max
        }
    }
    private val minBlankLines by lazy {
        val min = max(caosSettings?.MIN_BLANK_LINES_BETWEEN_COMMANDS ?: 0, 0)
        if (min == 0) {
            if (allowOnSameLine) {
                0
            } else {
                1
            }
        } else {
            min + 1
        }
    }
    private val noneSpace by lazy { Spacing.createSpacing(0, 0, 0, keepLineBreaks, maxBlankLines) }
    private val oneSpace by lazy { Spacing.createSpacing(1, 1, 0, keepLineBreaks, maxBlankLines) }
    private val anySpace by lazy { Spacing.createSpacing(0, 1, 0, keepLineBreaks, maxBlankLines) }
    private val keepSpace by lazy {
        Spacing.createKeepingFirstColumnSpacing(
            0,
            Int.MAX_VALUE,
            keepLineBreaks,
            maxBlankLines
        )
    }

    private val spaceBetweenBrackets: Int? by lazy {
        if (variant?.isOld == true) {
            0
        } else {
            when (caosSettings?.SPACE_BETWEEN_BYTE_STRING_AND_BRACKETS) {
                true -> 1
                false -> 0
                else -> null
            }
        }
    }

    fun getSpacing(child1: Block?, child2: Block?): Spacing? {

        if (child1 !is AbstractBlock || child2 !is AbstractBlock) {
            return noneSpace
        }

        val type = myNode.elementType
        val node1 = child1.node
        val type1 = node1.elementType
        val node2 = child2.node
        val type2 = node2.elementType
        val types = listOf(type, type1, type2)

        if (type2 == CaosScriptTypes.CaosScript_COMMENT_START) {
            return Spacing.createSpacing(0, Int.MAX_VALUE, minBlankLines, keepLineBreaks, maxBlankLines)
        }

        if (type1 == CaosScriptTypes.CaosScript_COMMENT_BODY || type1 == CaosScriptTypes.CaosScript_COMMENT_START) {
            if (type2 != CaosScriptTypes.CaosScript_COMMENT_START) {
                return if (caosSettings?.FORCE_BLANK_LINES_AFTER_COMMENT.orFalse()) {
                    Spacing.createSpacing(0, Int.MAX_VALUE, 1 + minBlankLines, keepLineBreaks, maxBlankLines)
                } else {
                    keepSpace
                }
            }
        }

        if (CaosScriptTypes.CaosScript_COMMA in types) {
            return noneSpace
        }

        if (node1.next?.text.orEmpty().contains("\n")) {
            return Spacing.createSpacing(0, 0, minBlankLines, keepLineBreaks, maxBlankLines)
        }

        if (type1 == CaosScriptTypes.CaosScript_EQUAL_SIGN || type2 == CaosScriptTypes.CaosScript_EQUAL_SIGN) {
            return Spacing.createSpacing(1, 1, 0, keepLineBreaks, maxBlankLines)
        }

        if (type2 == CaosScriptTypes.CaosScript_CAOS_2_COMMENT_START) {
            return Spacing.createSpacing(0, 0, 0, keepLineBreaks, 0)
        }

        if (type1 == CaosScriptTypes.CaosScript_CAOS_2_COMMENT_START) {
            return Spacing.createSpacing(1, 1, 0, keepLineBreaks, maxBlankLines)
        }

        if (type1 == CaosScriptTypes.CaosScript_CAOS_2_VALUE || type2 == CaosScriptTypes.CaosScript_CAOS_2_VALUE) {
            return Spacing.createSpacing(1, 1, 0, keepLineBreaks, 0)
        }

        if (type1 == CaosScriptTypes.CaosScript_CAOS_2_COMMAND) {
            return Spacing.createSpacing(1, 1, 0, keepLineBreaks, 0)
        }

        if (type1 == CaosScriptTypes.CaosScript_CAOS_2_BLOCK_HEADER) {
            return Spacing.createSpacing(0, 0, minBlankLines, keepLineBreaks, 0)
        }

        if (type1 == CaosScriptTypes.CaosScript_COMMENT_BLOCK) {
            return Spacing.createSpacing(0, 0, minBlankLines, keepLineBreaks, 0)
        }

        if ((type1 == CaosScriptTypes.CaosScript_OPEN_BRACKET && type2.isByteStringValue) || (type2 == CaosScriptTypes.CaosScript_CLOSE_BRACKET && type1.isByteStringValue)) {
            return if (node1.lineNumber != node2.lineNumber) {
                Spacing.createKeepingFirstColumnSpacing(
                    spaceBetweenBrackets ?: 0,
                    spaceBetweenBrackets ?: Int.MAX_VALUE,
                    keepLineBreaks,
                    maxBlankLines
                )
            } else {
                Spacing.createSpacing(
                    spaceBetweenBrackets ?: 0,
                    spaceBetweenBrackets ?: Int.MAX_VALUE,
                    0,
                    keepLineBreaks,
                    maxBlankLines
                )
            }
        }

        val node2Psi = node2.psi
        if (node2Psi is CaosScriptIsCommandToken) {
            if (node1.treeParent.lastChildNode == node1) {
                return Spacing.createSpacing(0, 0, minBlankLines, keepLineBreaks, maxBlankLines)
            }
        }

        return when (node1.psi) {
            is CaosScriptRvalue -> spaceAfterIsValueOfType(node2, maxBlankLines)
            is CaosScriptIsCommandToken -> spaceAfterIsCommandToken(node2, maxBlankLines)
            is CaosScriptEqOp -> oneSpace
            else -> {
                if (myNode.next?.isDirectlyPrecededByNewline().orFalse()) {
                    return Spacing.createSpacing(0, 0, 0, keepLineBreaks, maxBlankLines)
                } else if (node2.isDirectlyPrecededByNewline()) {
                    return Spacing.createSpacing(0, 0, 0, keepLineBreaks, maxBlankLines)
                } else {
                    anySpace
                }
            }
        }

    }

    private fun spaceAfterIsCommandToken(node2: ASTNode, keepBlankLines: Int): Spacing? {
        if (node2.elementType == CaosScriptTypes.CaosScript_COMMA) {
            return Spacing.createSpacing(0, 0, 0, false, keepBlankLines)
        }
        if (node2.psi.let { it is CaosScriptRvalue || it is CaosScriptHasCodeBlock }) {
            return Spacing.createSpacing(1, 1, 0, false, keepBlankLines)
        }
        return null
    }

    private fun spaceAfterIsValueOfType(node2: ASTNode, keepBlankLines: Int): Spacing? {
        if (node2.elementType == CaosScriptTypes.CaosScript_COMMA || node2.isDirectlyPrecededByNewline()) {
            return Spacing.createSpacing(0, 0, 0, false, keepBlankLines)
        }
        if (node2.psi is CaosScriptRvalue) {
            return Spacing.createSpacing(1, 1, 0, false, keepBlankLines)
        }
        return null
    }

    private val IElementType.isByteStringValue: Boolean get() {
        return this == CaosScriptTypes.CaosScript_BYTE_STRING ||
                this == CaosScriptTypes.CaosScript_BYTE_STRING_POSE_ELEMENT ||
                this == CaosScriptTypes.CaosScript_INT ||
                this == CaosScriptTypes.CaosScript_BYTE_STRING_R ||
                this == CaosScriptTypes.CaosScript_ANIM_R
    }
}