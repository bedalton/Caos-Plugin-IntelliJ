package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCAssignment
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRGend
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings
import com.intellij.codeInspection.*
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor

class CaosCommandUndocumentedCommandUsage : LocalInspectionTool() {
    override fun getDisplayName(): String = "Undocumented command usage"
    override fun getGroupDisplayName(): String = "CaosScript"
    override fun getShortName(): String = "UndocumentedCommandUsage"
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitRGend(o: CaosScriptRGend) {
                super.visitRGend(o)
                annotateGend(o, holder)
            }
        }
    }

    private fun annotateGend(o: CaosScriptRGend, holder: ProblemsHolder) {
        if (CaosScriptProjectSettings.isIgnoredUndocumentedCommand("GEND")) {
            holder.registerProblem(o, "", ProblemHighlightType.INFORMATION, AlterIgnoredUndocumentedCommand("GEND", false))
            return
        }
        holder.registerProblem(o, "GEND is an undocumented command and results may be unexpected", AlterIgnoredUndocumentedCommand("GEND", true))
    }

}

private class AlterIgnoredUndocumentedCommand(val command:String, val ignore:Boolean) : LocalQuickFix {
    override fun getFamilyName(): String = "CaosScript"

    override fun getName(): String = if (ignore) "Add $command to ignored list" else "Remove $command from ignored list"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        CaosScriptProjectSettings.ignoreUndocumentedCommand(command, ignore)
    }

}