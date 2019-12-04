package com.openc2e.plugins.intellij.caos.def.psi.types

import com.intellij.psi.tree.TokenSet
import com.openc2e.plugins.intellij.caos.def.lexer.CaosDefTypes

class CaosDefTokenSets {

    companion object {
        @JvmStatic
        val COMMENTS = TokenSet.create(
                CaosDefTypes.CaosDef_DOC_COMMENT,
                CaosDefTypes.CaosDef_INLINE_DOC_COMMENT
        )
    }
}