package com.openc2e.plugins.intellij.agenteering.caos.def.format

import com.intellij.codeInsight.editorActions.enter.EnterBetweenBracesDelegate

class CaosDefEnterBetweenBracesDelegate : EnterBetweenBracesDelegate() {
    override fun isBracePair(lBrace: Char, rBrace: Char): Boolean {
        return (lBrace == '(' && rBrace == ')') || (lBrace == '[' && rBrace == ']')
    }
}
