package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueItemName
import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class CaosScriptVetoRenameCondition : Condition<PsiElement> {
    @Suppress("RedundantIf")
    override fun value(element: PsiElement?): Boolean {
        if (element == null) {
            return true
        }

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

        if (element is CaosScriptToken) {
            return false
        }

        if (element is CatalogueItemName) {
            return false
        }

        return true
    }
}