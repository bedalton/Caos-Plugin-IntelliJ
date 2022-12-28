package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.util.elementType


class CaosScriptBlock internal constructor(
    node: ASTNode,
    wrap: Wrap?,
    alignment: Alignment?,
    val settings: CommonCodeStyleSettings,
    private val caosSettings: CaosScriptCodeStyleSettings?
) : AbstractBlock(node, wrap, alignment) {

    private val spacingProcessor: CaosScriptSpacingProcessor by lazy {
        CaosScriptSpacingProcessor(node, settings)
    }
    private val indentProcessor: CaosScriptIndentProcessor by lazy {
        if (caosSettings == null) {
//            LOGGER.info("CAOS settings are null, Using null indent processor")
            CaosScriptNullIndentProcessor
        } else {
            CaosScriptIndentProcessorImpl(caosSettings)
        }
    }

    override fun buildChildren(): List<Block> {
        val blocks: MutableList<Block> = mutableListOf()
        var child: ASTNode? = myNode.firstChildNode
        while (child != null) {
            if (child.elementType !in CaosScriptTokenSets.WHITESPACES || child.text.isNotBlank()) {
                val block: Block = CaosScriptBlock(
                    child,
                    NONE_WRAP,
                    Alignment.createAlignment(),
                    settings,
                    caosSettings
                )
                blocks.add(block)
            }
            child = child.treeNext
        }
        return blocks
    }

    override fun getChildAttributes(newIndex: Int): ChildAttributes {
        if (caosSettings == null) {
            return nullIndent
        }
        if (caosSettings.indentNone()) {
            return noneIndent
        }
        val psi = myNode.psi
            ?: return noneIndent

        if (psi is CaosScriptCommentBlock || psi.hasParentOfType(CaosScriptCommentBlock::class.java) || psi.getPreviousNonEmptySibling(
                true
            ) is CaosScriptCommentBlock
        ) {
            return ChildAttributes(Indent.getAbsoluteNoneIndent(), null)
        }

        val cursorElement = psi.editor?.let { it.cursorElementInside(psi.textRange) ?: it.primaryCursorElement }
            ?: return noneIndent
        if (psi is CaosScriptScriptElement) {
            return if (psi.scriptTerminator == null) {
                noneIndent
            } else {
                normalIndent
            }
        }
        if (psi is CaosScriptHasCodeBlock) {
            if (cursorElement.tokenType == TokenType.WHITE_SPACE) {
                val previous = cursorElement.getPreviousNonEmptySibling(true)
                if (previous is CaosScriptCIscr || previous is CaosScriptCRscr || previous is CaosScriptCScrp) {
                    if (previous.getParentOfType(CaosScriptScriptElement::class.java)?.scriptTerminator != null) {
                        return normalIndent
                    } else {
//                        LOGGER.info("Previous is token: iscr|rscr|scrp and parent has null script terminator: (${psi.elementType})${psi.text}")
                        noneIndent
                    }
                }
                if (previous is CaosScriptCodeBlockLine || previous is CaosScriptCodeBlock || previous?.parent is CaosScriptCodeBlock) {
//                    LOGGER.info("HasScriptBlockPreviously: (${psi.elementType})${psi.text}")
                    return normalIndent
                }
                val next = cursorElement.getNextNonEmptySibling(true)
                if (next is CaosScriptCodeBlockLine || next is CaosScriptCodeBlock || next?.parent is CaosScriptCodeBlock) {
//                    LOGGER.info("Next is code block: ${psi.elementType}${psi.text}")
                    return normalIndent
                }
                if (cursorElement.parent is CaosScriptScriptElement) {
                    return if ((cursorElement.parent as CaosScriptScriptElement).scriptTerminator != null) {
//                        LOGGER.info("Parent is script with terminator: (${psi.elementType})${psi.text}")
                        normalIndent
                    } else {
                        noneIndent
                    }
                }
            }
            return if (psi is CaosScriptScriptElement) {
                if (cursorElement.parent == psi || psi.scriptTerminator == null) {
                    noneIndent
                } else {
//                    LOGGER.info("PSI is script with script terminator: (${psi.elementType})${psi.text}")
                    normalIndent
                }
            } else {
                if (cursorElement.isOrHasParentOfType(CaosScriptCommentBlock::class.java)) {
                    absoluteNoneIndent
                }else {
                    val previousElement = cursorElement.getPreviousNonEmptySibling(true)
                    if (previousElement == null || previousElement.isOrHasParentOfType(CaosScriptCommentBlock::class.java)
                            .orFalse()
                    ) {
                        absoluteNoneIndent
                    } else {
//                        LOGGER.info("Previous is comment  with comment block parent: (${psi.elementType})${psi.text}")
                        normalIndent
                    }
                }
            }
        }

        if (psi.parent is CaosScriptScriptElement) {
            val parent = psi.parent as CaosScriptScriptElement
            if (parent.scriptTerminator == null || parent is CaosScriptMacro) {
                return noneIndent
            }
            if (!caosSettings.INDENT_SCRP) {
                return absoluteNoneIndent
            }
//            LOGGER.info("PSI.parent is Script element: (${psi.elementType})${psi.text}")
            return normalIndent
        }

        // Needs to check if my node is a DoifStatement parent
        // Because the end of any child (internal doif block, elif, else) are considered the end,
        // meaning the last line would never be indented
        if (psi is CaosScriptDoifStatement) {
//            LOGGER.info("PSI element is doif statement: (${psi.elementType})${psi.text}")
            return normalIndent
        }
        if (psi.parent?.tokenType in listOf(
                CaosScriptTypes.CaosScript_DOIF_STATEMENT_STATEMENT,
                CaosScriptTypes.CaosScript_ELSE_IF_STATEMENT,
                CaosScriptTypes.CaosScript_ELSE_STATEMENT
            )
        ) {
            return normalIndent
        }
        if (psi is CaosScriptCodeBlockLine) {
            return normalIndent
        }
        if (psi.tokenType in CaosScriptTokenSets.WHITESPACES) {
            return normalIndent
        }

        if (cursorElement.tokenType == TokenType.WHITE_SPACE) {
            val previous = cursorElement.getPreviousNonEmptySibling(true)
            if (previous is CaosScriptCodeBlockLine || previous is CaosScriptCodeBlock || previous?.parent is CaosScriptCodeBlock) {
                return normalIndent
            }
            val next = cursorElement.getNextNonEmptySibling(true)
            if (next is CaosScriptCodeBlockLine || next is CaosScriptCodeBlock || next?.parent is CaosScriptCodeBlock) {
                return normalIndent
            }
        }
//        LOGGER.info("PSI element matches nothing, so none indent: (${psi.elementType})${psi.text}")
        return noneIndent
    }

    override fun getIndent(): Indent? {
        if (caosSettings == null) {
            return null
        }
        if (caosSettings.indentNone()) {
            return Indent.getNoneIndent()
        }
        return indentProcessor.getChildIndent(node)
    }

    override fun getSpacing(child1: Block?, child2: Block): Spacing? {
        return spacingProcessor.getSpacing(child1, child2)
    }

    override fun isLeaf(): Boolean {
        return myNode.firstChildNode == null
    }

    companion object {
        private val normalIndent = ChildAttributes(Indent.getNormalIndent(), null)
        private val noneIndent = ChildAttributes(Indent.getNoneIndent(), null)
        private val absoluteNoneIndent = ChildAttributes(Indent.getAbsoluteNoneIndent(), null)
        private val nullIndent = ChildAttributes(null, null)
    }

}


internal val NONE_WRAP: Wrap = Wrap.createWrap(WrapType.NONE, false)