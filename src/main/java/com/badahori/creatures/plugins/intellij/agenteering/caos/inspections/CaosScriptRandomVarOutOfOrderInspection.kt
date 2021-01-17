package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReorderRndvParameters
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCRndv
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

class CaosScriptRandomVarOutOfOrderInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = "RNDV values out of order"
    override fun getGroupDisplayName(): String = CaosBundle.message("caos.intentions.family")
    override fun getShortName(): String = "RNDVOutOfOrder"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor() {
            override fun visitCRndv(o: CaosScriptCRndv) {
                super.visitCRndv(o)
                annotateRNDV(o, holder)
            }
        }
    }

    private fun annotateRNDV(element: CaosScriptCRndv, holder: ProblemsHolder) {
        val variant = element.containingCaosFile?.variant
                ?: return
        if (variant != CaosVariant.C1)
            return
        val minMax:Pair<Int?, Int?> = element.rndvIntRange
        val min = minMax.first ?: return
        val max = minMax.second ?: return
        if (min < max)
            return
        if (minMax.first == minMax.second) {
            holder.registerProblem(element, CaosBundle.message("caos.annotator.syntax-error-annotator.rndv-result-is-the-same", min))
            return
        }
        listOfNotNull(element.minElement, element.maxElement).forEach {
            // Todo Infer min/max values from each variable value
            holder.registerProblem(it, CaosBundle.message("caos.annotator.syntax-error-annotator.rndv-out-of-order"), CaosScriptReorderRndvParameters(element))
        }
    }
}