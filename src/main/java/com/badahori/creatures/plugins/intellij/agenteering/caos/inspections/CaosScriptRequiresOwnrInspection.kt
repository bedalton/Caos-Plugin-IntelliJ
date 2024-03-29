package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptVarTokenGroup
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElementVisitor

class CaosScriptRequiresOwnrInspection : LocalInspectionTool(), DumbAware {
    override fun getDisplayName(): String = "Command used without OWNR object"
    override fun getGroupDisplayName(): String = CAOSScript
    override fun getShortName(): String = "NoOwnrObject"
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitIsCommandToken(o: CaosScriptIsCommandToken) {
                super.visitIsCommandToken(o)
                annotateCommand(o, holder)
            }

            override fun visitVarToken(o: CaosScriptVarToken) {
                super.visitVarToken(o)
                if (o.varGroup == CaosScriptVarTokenGroup.MVxx) {
                    val scriptParent = o.getParentOfType(CaosScriptScriptElement::class.java)
                    if (scriptParent is CaosScriptEventScript) {
                        return
                    }
                    holder.registerProblem(o, CaosBundle.message("caos.inspections.ownr-inspection.mvxx-requires-ownr"))
                }
            }
        }
    }

    private fun annotateCommand(element: CaosScriptIsCommandToken, holder: ProblemsHolder) {
        if (!requiresOwnr(element))
            return
        element.getParentOfType(CaosScriptScriptElement::class.java)?.let {
            return
        }
        holder.registerProblem(element, CaosBundle.message("caos.inspections.ownr-inspection.requires-ownr", element.commandString.uppercase()))
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
