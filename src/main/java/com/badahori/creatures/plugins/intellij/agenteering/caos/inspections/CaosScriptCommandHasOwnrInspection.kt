package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCommandLike
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptIsCommandToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor

class CaosScriptCommandHasOwnrInspection : LocalInspectionTool() {
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
        element.getParentOfType(CaosScriptEventScript::class.java)?.let { script:CaosScriptEventScript ->
            if (script.family != 4 && REQUIRES_CREATURE_OWNR.matches(element.text)) {
                holder.registerProblem(element, "''{0}'' command expects Creature OWNR object.")
            }
            return
        }
        holder.registerProblem(element, "''{0}'' command requires an OWNR object (ie. Should be used in an event script)")
    }

    private fun requiresOwnr(element:CaosScriptIsCommandToken) : Boolean {
        return (element.parent as? CaosScriptCommandLike)
                ?.commandDefinition
                ?.requiresOwnr
                ?: false
    }

    companion object {
        val REQUIRES_CREATURE_OWNR = "[Gg][Ee][Nn][Dd]|[_][Ii][Tt][_]".toRegex()
    }
}

private class AlterIgnoredUndocumentedCommand(val command:String, val ignore:Boolean) : LocalQuickFix {
    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")

    override fun getName(): String = if (ignore) "Add $command to ignored list" else "Remove $command from ignored list"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        CaosScriptProjectSettings.ignoreUndocumentedCommand(command, ignore)
    }

}