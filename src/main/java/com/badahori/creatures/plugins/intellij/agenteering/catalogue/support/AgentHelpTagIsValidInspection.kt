package com.badahori.creatures.plugins.intellij.agenteering.catalogue.support

import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueItemName
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueVisitor
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElementVisitor

class AgentHelpTagIsValidInspection : LocalInspectionTool(), DumbAware {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CatalogueVisitor() {
            override fun visitItemName(o: CatalogueItemName) {
                annotateItemName(o, holder)
            }
        }
    }


    private fun annotateItemName(element: CatalogueItemName, holder: ProblemsHolder) {
        val text = element.name
        val parts = agentHelpRegex
            .matchEntire(text)
            ?.groupValues
            ?.drop(1)
        if (parts != null && parts.size == 4) {
            val expected = "Agent Help ${parts[1]} ${parts[2]} ${parts[3]}"
            if (text != expected) {
                val errorMessage: String
                val fixText: String
                if (parts[0] != "Agent Help") {
                    errorMessage = CaosBundle.message("catalogue.errors.invalid-agent-help-tag-case")
                    fixText = CaosBundle.message("catalogue.errors.invalid-agent-help-tag-case-fix", parts[0])
                } else {
                    errorMessage = CaosBundle.message("catalogue.errors.invalid-agent-help-tag-invalid-formatting")
                    fixText = CaosBundle.message("catalogue.errors.invalid-agent-help-tag-invalid-formatting.fix", expected)
                }
                holder.registerProblem(
                    element,
                    errorMessage,
                    CaosScriptReplaceElementFix(
                        element,
                        "\"$expected\"",
                        fixText
                    )
                )

            }
        }
    }


    companion object {
        val agentHelpRegex = "\\s*(agent\\s*help)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s*".toRegex(RegexOption.IGNORE_CASE)
    }
}