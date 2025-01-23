package com.badahori.creatures.plugins.intellij.agenteering.caos.fixes


import com.bedalton.log.Log
import com.intellij.codeInsight.generation.CommenterDataHolder
import com.intellij.codeInsight.generation.SelfManagingCommenter
import com.intellij.lang.Commenter
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import kotlin.math.min

/**
 * Comments out CAOS code and is aware of CAOS2 comments `*#` and comments them out appropriately
 */
class CaosScriptSelfManagingCommenter : Commenter, SelfManagingCommenter<CommenterDataHolder> {

    override fun getLineCommentPrefix(): String {
        return COMMENT_PREFIX
    }

    override fun getCommentPrefix(line: Int, document: Document, data: CommenterDataHolder): String {
        return lineCommentPrefix
    }

    override fun getBlockCommentPrefix(): String? = null
    override fun getBlockCommentSuffix(): String? = null

    override fun getCommentedBlockCommentPrefix(): String? = null
    override fun getCommentedBlockCommentSuffix(): String? = null

    override fun getBlockCommentPrefix(selectionStart: Int, document: Document, data: CommenterDataHolder): String? =
        null

    override fun getBlockCommentSuffix(selectionEnd: Int, document: Document, data: CommenterDataHolder): String? = null

    override fun isLineCommented(line: Int, offset: Int, document: Document, data: CommenterDataHolder): Boolean {
        val text = getLineText(line, document).first.trim()
        return text.startsWith(COMMENT_PREFIX) && !text.startsWith(CAOS2_PREFIX)
    }

    override fun insertBlockComment(
        startOffset: Int,
        endOffset: Int,
        document: Document,
        data: CommenterDataHolder,
    ): TextRange? = null

    override fun uncommentBlockComment(
        startOffset: Int,
        endOffset: Int,
        document: Document,
        data: CommenterDataHolder,
    ) = Unit

    override fun commentLine(line: Int, offset: Int, document: Document, data: CommenterDataHolder) {
        val text = getLineText(line, document).first
        val commentPrefix = if (text.isEmpty()) {
            "*"
        } else {
            "* "
        }
        document.insertString(offset, commentPrefix)
    }

    override fun uncommentLine(line: Int, offset: Int, document: Document, data: CommenterDataHolder) {

        val (text, range) = getLineText(line, document)

        var offsetInLine = -1

        for ((i, char) in text.withIndex()) {
            if (char == ' ' || char == '\t') {
                continue
            }
            offsetInLine = i
            break
        }

        if (offsetInLine == -1) {
            return
        }

        val textLength = text.length

        if (text.getOrNull(0) != '*') {
            Log.e("Called to uncomment line, but line is not commented; Line: <$text>")
            return
        }

        val lineStart = range.startOffset
        val absoluteOffset = lineStart + offsetInLine

        val length = if (textLength >= (offsetInLine + 2) && text[offsetInLine + 1] == ' ') {
            2
        } else {
            1
        }
        document.deleteString(absoluteOffset, absoluteOffset + length)
    }

    override fun createLineCommentingState(
        startLine: Int,
        endLine: Int,
        document: Document,
        file: PsiFile,
    ): CommenterDataHolder? = null

    override fun createBlockCommentingState(
        selectionStart: Int,
        selectionEnd: Int,
        document: Document,
        file: PsiFile,
    ): CommenterDataHolder? = null

    override fun getBlockCommentRange(
        selectionStart: Int,
        selectionEnd: Int,
        document: Document,
        data: CommenterDataHolder,
    ): TextRange? {
        return null
    }

    companion object {
        private const val COMMENT_PREFIX = "*"
        private const val CAOS2_PREFIX = "*#"

        private fun getLineText(line: Int, document: Document, max: Int? = null): Pair<String, TextRange> {
            val lineStart = document.getLineStartOffset(line)
            val lineEnd = if (max != null) {
                min(lineStart + max, document.getLineEndOffset(line))
            } else {
                document.getLineEndOffset(line)
            }
            return Pair(document.getText(TextRange(lineStart, lineEnd)), TextRange(lineStart, lineEnd))
        }
    }

}