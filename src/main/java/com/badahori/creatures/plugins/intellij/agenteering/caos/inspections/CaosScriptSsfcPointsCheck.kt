package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCSsfc
import com.badahori.creatures.plugins.intellij.agenteering.utils.toIntSafe
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

class CaosScriptSsfcPointsCheck : LocalInspectionTool() {

    override fun getDisplayName(): String = "SSFC number of arguments"
    override fun getGroupDisplayName(): String = CaosBundle.message("caos.intentions.family")
    override fun getShortName(): String = "SSFCArgumentsCheck"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor() {
            override fun visitCSsfc(o: CaosScriptCSsfc) {
                super.visitCSsfc(o)
                annotateSsfc(o, holder)
            }
        }
    }

    private fun annotateSsfc(element: CaosScriptCSsfc, holder: ProblemsHolder) {
        val arguments = element.arguments
        val numberOfPoints = arguments.getOrNull(1)?.text?.toIntSafe() ?: return
        val expectedNumberOfPointArguments = (numberOfPoints * 2)
        val numberOfArguments = arguments.size
        val expectedNumberOfArguments = expectedNumberOfPointArguments + 2
        when {
            numberOfArguments == expectedNumberOfArguments -> return
            numberOfArguments < expectedNumberOfArguments -> holder.registerProblem(arguments[1], CaosBundle.message("caos.inspections.ssfc.not-enough-arguments-error", numberOfPoints, (expectedNumberOfArguments - numberOfArguments)))
            numberOfArguments > expectedNumberOfArguments -> {
                (expectedNumberOfArguments .. numberOfArguments).forEach {i ->
                    val argument = arguments.getOrNull(i)
                            ?: return
                    holder.registerProblem(argument, CaosBundle.message("caos.inspection.ssf.too-many-arguments-error", expectedNumberOfArguments, numberOfArguments))
                }
            }
        }
    }
}