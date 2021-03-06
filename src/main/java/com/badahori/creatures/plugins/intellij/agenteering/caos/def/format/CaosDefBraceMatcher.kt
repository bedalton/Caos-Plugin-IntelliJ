package com.badahori.creatures.plugins.intellij.agenteering.caos.def.format

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lexer.CaosDefTypes.*

class CaosDefBraceMatcher : PairedBraceMatcher {

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int {
        return openingBraceOffset
    }

    override fun getPairs(): Array<BracePair> {
        return PAIRS
    }

    override fun isPairedBracesAllowedBeforeType(p0: IElementType, p1: IElementType?): Boolean {
        return true
    }

    companion object {
        val PAIRS = listOf(
                BracePair(CaosDef_OPEN_BRACKET, CaosDef_CLOSE_BRACKET, true),
                BracePair(CaosDef_OPEN_PAREN, CaosDef_CLOSE_PAREN, true),
                BracePair(CaosDef_OPEN_BRACE, CaosDef_CLOSE_BRACE, true)
        ).toTypedArray()
    }

}