package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptNamedGameVar
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptQuoteStringLiteral
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSubroutine
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSubroutineName
import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiElement

class CaosScriptVetoRenameCondition : Condition<PsiElement> {
    override fun value(element: PsiElement?): Boolean {
        if (element == null)
            return false
        if (element is CaosScriptSubroutineName || element is CaosScriptSubroutine)
            return true
        if (element is CaosScriptQuoteStringLiteral)
            return true
        return false
    }
}