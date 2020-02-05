package com.openc2e.plugins.intellij.caos.psi.types

import com.intellij.psi.TokenType
import com.intellij.psi.tree.TokenSet
import com.openc2e.plugins.intellij.caos.lexer.CaosScriptTypes.*

class CaosScriptTokenSets {

    companion object {

        @JvmStatic
        val EQ_OPS = TokenSet.create(
                CaosScript_EQ_OP,
                CaosScript_EQ_OP_OLD,
                CaosScript_EQ_OP_OLD_,
                CaosScript_EQ_OP_NEW,
                CaosScript_EQ_OP_NEW_
        )
        @JvmStatic
        val COMMENTS = TokenSet.create(
                CaosScript_COMMENT_LITERAL
        )

        @JvmStatic
        val WHITE_SPACE_LIKE = TokenSet.create(
                CaosScript_SPACE_,
                CaosScript_COMMENT_LITERAL,
                TokenType.WHITE_SPACE
        )

        @JvmStatic
        val NUMBER_LITERALS = TokenSet.create(
                CaosScript_INT,
                CaosScript_DECIMAL,
                CaosScript_PLUS
        )

        @JvmStatic
        val STRING_LIKE = TokenSet.create(
                CaosScript_ANIMATION_STRING,
                CaosScript_BYTE_STRING,
                CaosScript_ANIM_R,
                CaosScript_QUOTE_STRING,
                CaosScript_STRING_LITERAL,
                CaosScript_TEXT_LITERAL,
                CaosScript_OPEN_BRACKET,
                CaosScript_CLOSE_BRACKET
        )

        val LITERALS: TokenSet = TokenSet.create(
                CaosScript_INT,
                CaosScript_DECIMAL,
                CaosScript_NUMBER,
                CaosScript_STRING_LITERAL,
                CaosScript_BYTE_STRING,
                CaosScript_ANIMATION_STRING,
                CaosScript_QUOTE_STRING
        )

        @JvmStatic
        val KEYWORDS = TokenSet.create(
                CaosScript_DOIF,
                CaosScript_ELIF,
                CaosScript_ELSE,
                CaosScript_ENDI,
                CaosScript_ENUM,
                CaosScript_NEXT,
                CaosScript_ESCN,
                CaosScript_NSCN,
                CaosScript_ETCH,
                CaosScript_ESEE,
                CaosScript_LOOP,
                CaosScript_EVER,
                CaosScript_UNTL,
                CaosScript_ENDM,
                CaosScript_EQ_OP,
                CaosScript_SUBR,
                CaosScript_ISCR,
                CaosScript_SCRP,
                CaosScript_REPS,
                CaosScript_REPE,
                CaosScript_ISCR
        )

        @JvmStatic
        val Variables = TokenSet.create(
                CaosScript_VAR_X,
                CaosScript_VA_XX,
                CaosScript_OBV_X,
                CaosScript_OV_XX,
                CaosScript_MV_XX
        )

        @JvmStatic
        val ANIMATION_STRING = TokenSet.create(
                CaosScript_ANIMATION_STRING,
                CaosScript_BYTE_STRING,
                CaosScript_ANIM_R
        )

        @JvmStatic
        val ScriptTerminators = TokenSet.create(
                CaosScript_SCRP,
                CaosScript_ISCR,
                CaosScript_ENDM
        )
    }
}