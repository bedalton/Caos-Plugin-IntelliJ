package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

class CaosScriptBraceMatcher : PairedBraceMatcher {
    private val pairs = arrayOf(
            BracePair(CaosScriptTypes.CaosScript_DOUBLE_QUOTE, CaosScriptTypes.CaosScript_DOUBLE_QUOTE, false),
            BracePair(CaosScriptTypes.CaosScript_SINGLE_QUOTE, CaosScriptTypes.CaosScript_SINGLE_QUOTE, false),
            BracePair(CaosScriptTypes.CaosScript_OPEN_BRACKET, CaosScriptTypes.CaosScript_CLOSE_BRACKET, false),
            BracePair(CaosScriptTypes.CaosScript_K_DOIF, CaosScriptTypes.CaosScript_K_ENDI, true),
            BracePair(CaosScriptTypes.CaosScript_K_ENUM, CaosScriptTypes.CaosScript_K_NEXT, true),
            BracePair(CaosScriptTypes.CaosScript_K_ECON, CaosScriptTypes.CaosScript_K_NEXT, true),
            BracePair(CaosScriptTypes.CaosScript_K_EPAS, CaosScriptTypes.CaosScript_K_NEXT, true),
            BracePair(CaosScriptTypes.CaosScript_K_ETCH, CaosScriptTypes.CaosScript_K_NEXT, true),
            BracePair(CaosScriptTypes.CaosScript_K_ESEE, CaosScriptTypes.CaosScript_K_NEXT, true),
            BracePair(CaosScriptTypes.CaosScript_K_ESCN, CaosScriptTypes.CaosScript_K_NSCN, true),
            BracePair(CaosScriptTypes.CaosScript_K_REPS, CaosScriptTypes.CaosScript_K_REPE, true),
            BracePair(CaosScriptTypes.CaosScript_K_LOOP, CaosScriptTypes.CaosScript_K_UNTL, true),
            BracePair(CaosScriptTypes.CaosScript_K_LOOP, CaosScriptTypes.CaosScript_K_EVER, true),
            BracePair(CaosScriptTypes.CaosScript_K_SCRP, CaosScriptTypes.CaosScript_K_ENDM, true),
            BracePair(CaosScriptTypes.CaosScript_K_REPS, CaosScriptTypes.CaosScript_K_REPE, true),
            BracePair(CaosScriptTypes.CaosScript_K_SUBR, CaosScriptTypes.CaosScript_K_RETN, true),
            BracePair(CaosScriptTypes.CaosScript_K_REPS, CaosScriptTypes.CaosScript_K_REPE, true)
    )
    override fun getCodeConstructStart(file: PsiFile?, offset: Int): Int {
        return offset
    }

    override fun getPairs(): Array<BracePair> {
        return pairs
    }

    override fun isPairedBracesAllowedBeforeType(p0: IElementType, p1: IElementType?): Boolean {
        return true
    }

}