package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class CaosScriptVetoRenameCondition : Condition<PsiElement> {
    override fun value(element: PsiElement?): Boolean {
        if (element == null)
            return true
        if (element is PsiFile) {
            return false
        }
        if (element !is CaosScriptCompositeElement) {
            return false
        }
        if (element is CaosScriptSubroutineName || element is CaosScriptSubroutine) {
            return false
        }
        if (element is CaosScriptQuoteStringLiteral || element is CaosScriptStringText) {
            return false
        }
        if (element is CaosScriptNamedGameVar) {
            return false
        }
        return true
    }
}