package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCommandElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptIsCommandToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElementVisitor

class CaosScriptRequiresOwnrWithWarningInspection : LocalInspectionTool(), DumbAware {
    override fun getDisplayName(): String = "Non-error generating use of OWNR object command without OWNR object"
    override fun getGroupDisplayName(): String = CaosBundle.message("caos.intentions.family")
    override fun getShortName(): String = "NoOwnrObjectWarning"
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitIsCommandToken(o: CaosScriptIsCommandToken) {
                super.visitIsCommandToken(o)
                annotateCommand(o, holder)
            }
        }
    }

    private fun annotateCommand(element: CaosScriptIsCommandToken, holder: ProblemsHolder) {
        if (!requiresOwnr(element))
            return
        element.getParentOfType(CaosScriptEventScript::class.java)?.let { script:CaosScriptEventScript ->
            if ((script.family != 4 && script.family != 0) && REQUIRES_CREATURE_OWNR.matches(element.text)) {
                holder.registerProblem(element, CaosBundle.message("caos.inspections.ownr-inspection.requires-creature-ownr", element.commandString.uppercase()))
            }
            return
        }
        holder.registerProblem(element, CaosBundle.message("caos.inspections.ownr-inspection.requires-ownr", element.commandString.uppercase()))
    }

    private fun requiresOwnr(element:CaosScriptIsCommandToken) : Boolean {
        val variant = element.variant
                ?: return false
        return (element.parent as? CaosScriptCommandElement)
                ?.commandDefinition
                ?.requiresOwnrIsWarning(variant)
                ?: false
    }

    companion object {
        val REQUIRES_CREATURE_OWNR = "[Gg][Ee][Nn][Dd]|[_][Ii][Tt][_]|[Aa][Tt][Tt][Nn]".toRegex()
    }
}