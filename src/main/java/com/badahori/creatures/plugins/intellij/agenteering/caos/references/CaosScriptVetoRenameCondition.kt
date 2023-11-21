package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.parentParameter
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueItemName
import com.badahori.creatures.plugins.intellij.agenteering.utils.hasParentOfType
import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class CaosScriptVetoRenameCondition : Condition<PsiElement> {

    /**
     * Return true if file rename should be vetoed
     */
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
        if (element is CaosScriptQuoteStringLiteral) {
//            if (element.hasParentOfType(CaosScriptCaos2Block::class.java)) {
//                return true
//            }
//            val argument = element.getParentOfType(CaosScriptArgument::class.java)
//                ?: return false
//            val parameter = argument.parentParameter
//                ?: return false
//            val variant = element.variant
//            val valuesList = parameter.valuesList[variant ?: CaosVariant.DS]
//                ?: return false
//            return valuesList.name.startsWith("File.")
            return false
        }
        if (element is CaosScriptStringText) {
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