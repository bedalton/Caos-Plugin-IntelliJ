package com.openc2e.plugins.intellij.caos.psi.types

import com.intellij.psi.tree.TokenSet
import com.openc2e.plugins.intellij.caos.lexer.CaosTypes

class CaosTokenSets {

    companion object {
        @JvmStatic
        val COMMENTS = TokenSet.create(
                CaosTypes.Caos_COMMENT,
                CaosTypes.Caos_COMMENT_LITERAL
        )
    }
}