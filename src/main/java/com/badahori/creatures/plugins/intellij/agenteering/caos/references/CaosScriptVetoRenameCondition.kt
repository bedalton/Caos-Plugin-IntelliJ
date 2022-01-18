package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiElement

class CaosScriptVetoRenameCondition : Condition<PsiElement> {
    override fun value(element: PsiElement?): Boolean {
        if (element == null)
            return true
        if (element is CaosScriptSubroutineName || element is CaosScriptSubroutine)
            return false
        if (element is CaosScriptQuoteStringLiteral || element is CaosScriptStringText)
            return false
        if (element is CaosScriptNamedGameVar)
            return false
        return true
    }
}