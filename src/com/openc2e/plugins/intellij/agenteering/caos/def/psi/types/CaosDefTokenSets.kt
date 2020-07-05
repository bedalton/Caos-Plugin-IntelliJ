package com.openc2e.plugins.intellij.agenteering.caos.def.psi.types

import com.intellij.psi.tree.TokenSet
import com.openc2e.plugins.intellij.agenteering.caos.def.lexer.CaosDefTypes.*

class CaosDefTokenSets {

    companion object {
        @JvmStatic
        val COMMENTS = TokenSet.create(
                CaosDef_DOC_COMMENT,
                CaosDef_LINE_COMMENT
        )

        val COMMENT_PARTS = TokenSet.create(
                CaosDef_DOC_COMMENT_VARIABLE_TYPE,
                CaosDef_DOC_COMMENT_PARAM,
                CaosDef_DOC_COMMENT_PARAM_TEXT,
                CaosDef_DOC_COMMENT_OPEN,
                CaosDef_DOC_COMMENT_CLOSE,
                CaosDef_DOC_COMMENT_LINE,
                CaosDef_DOC_COMMENT_RETURN,
                CaosDef_COMMENT_TEXT_LITERAL,
                CaosDef_LEADING_ASTRISK
        )

        val COMMENT_AT_KEYWORDS = TokenSet.create(
                CaosDef_AT_RETURN,
                CaosDef_LVALUE,
                CaosDef_RVALUE,
                CaosDef_AT_LVALUE,
                CaosDef_AT_RVALUE,
                CaosDef_AT_PARAM,
                CaosDef_AT_VARIANT
        )
    }
}