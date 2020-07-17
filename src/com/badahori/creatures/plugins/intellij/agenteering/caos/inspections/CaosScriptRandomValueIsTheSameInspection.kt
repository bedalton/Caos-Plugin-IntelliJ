package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCRndv
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

class CaosScriptRandomValueIsTheSameInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = "Rndv value is always the same"
    override fun getGroupDisplayName(): String = "CaosScript"
    override fun getShortName(): String = "RndvIsTheSame"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor() {
            override fun visitCRndv(o: CaosScriptCRndv) {
                super.visitCRndv(o)
                annotateRndv(o, holder)
            }
        }
    }

    private fun annotateRndv(element: CaosScriptCRndv, holder: ProblemsHolder) {
        val variant = element.containingCaosFile?.variant
                ?: return
        if (variant.isNotOld)
            return
        val minMax:Pair<Int?, Int?> = element.rndvIntRange
        val min = minMax.first ?: return
        val max = minMax.second ?: return
        if (min < max)
            return
        if (minMax.first != minMax.second) {
            return
        }
        holder.registerProblem(element, CaosBundle.message("caos.annotator.command-annotator.rndv-result-is-the-same", min))
    }
}