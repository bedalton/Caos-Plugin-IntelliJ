package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCommandElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptIsCommandToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptScriptElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

class CaosScriptRequiresOwnrInspection : LocalInspectionTool() {
    override fun getDisplayName(): String = "Command used without OWNR object"
    override fun getGroupDisplayName(): String = CaosBundle.message("caos.intentions.family")
    override fun getShortName(): String = "NoOwnrObject"
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
        element.getParentOfType(CaosScriptScriptElement::class.java)?.let {
            return
        }
        holder.registerProblem(element, CaosBundle.message("caos.inspections.ownr-inspection.requires-ownr", element.commandString.toUpperCase()))
    }

    private fun requiresOwnr(element:CaosScriptIsCommandToken) : Boolean {
        val variant = element.variant
                ?: return false
        return (element.parent as? CaosScriptCommandElement)
                ?.commandDefinition
                ?.requiresOwnrIsError(variant)
                ?: false
    }
}
