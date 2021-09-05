package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptIsVariable
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptNamedGameVar
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVarToken
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

class CaosScriptVariableTypeMismatchInspection : LocalInspectionTool() {


    override fun getDisplayName(): String = "Rndv value is always the same"
    override fun getGroupDisplayName(): String = CaosBundle.message("caos.intentions.family")
    override fun getShortName(): String = "RndvIsTheSame"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor() {
            override fun visitVarToken(variable: CaosScriptVarToken) {
                checkIndexedVariableType(variable)
            }

            override fun visitNamedGameVar(variable: CaosScriptNamedGameVar) {
                checkIndexedVariableType(variable)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun checkIndexedVariableType(varToken: CaosScriptIsVariable) {
        TODO()
    }



}