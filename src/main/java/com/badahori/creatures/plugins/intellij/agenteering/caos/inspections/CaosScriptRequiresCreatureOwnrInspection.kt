package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLib
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCommandElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptIsCommandToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

class CaosScriptRequiresCreatureOwnrInspection : LocalInspectionTool() {
    override fun getDisplayName(): String = "Requires creature ownr object"
    override fun getGroupDisplayName(): String = CaosBundle.message("caos.intentions.family")
    override fun getShortName(): String = "RequiresCreatureOwnr"
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitIsCommandToken(o: CaosScriptIsCommandToken) {
                super.visitIsCommandToken(o)
                annotateCommand(o, holder)
            }
        }
    }

    private fun annotateCommand(element: CaosScriptIsCommandToken, holder: ProblemsHolder) {
        val requiresCreatureOwnr = (element.parent as? CaosScriptCommandElement)?.commandDefinition?.requiresCreatureOwnr
            ?: return
        if (!requiresCreatureOwnr)
            return
        element.getParentOfType(CaosScriptEventScript::class.java)?.let { script: CaosScriptEventScript ->
            if (hasCreatureOwner(script)) {
                return
            }
        }
        holder.registerProblem(element, CaosBundle.message("caos.inspections.ownr-inspection.requires-creature-ownr", element.commandString.toUpperCase()))
    }

    private fun hasCreatureOwner(eventScript:CaosScriptEventScript) : Boolean {
        if (eventScript.family.let { it == 0 || it == 4})
            return true
        val variant = eventScript.variant
            ?: return true
        val eventNumbers = CaosLibs[variant].valuesList("EventNumbers")
            ?: return true
        val eventValue = eventNumbers[eventScript.eventNumber]
            ?: return false
        return eventValue.name.toLowerCase().contains("extra")
    }
}
