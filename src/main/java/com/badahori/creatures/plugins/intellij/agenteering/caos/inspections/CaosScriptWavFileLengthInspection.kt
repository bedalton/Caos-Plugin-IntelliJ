package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptArgument
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptQuoteStringLiteral
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.parameter
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

class CaosScriptWavFileLengthInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = CaosBundle.message("caos.inspection.invalid-wav-file-name-length.display-name")
    override fun getGroupDisplayName(): String = CaosBundle.message("caos.intentions.family")
    override fun getShortName(): String = CaosBundle.message("caos.inspection.invalid-wav-file-name-length.short-name")
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitQuoteStringLiteral(o: CaosScriptQuoteStringLiteral) {
                super.visitQuoteStringLiteral(o)
                validate(o, holder)
            }
        }
    }

    private fun validate(o: CaosScriptQuoteStringLiteral, problemsHolder: ProblemsHolder) {
        val variant = o.variant
            ?: return
        val name = o.getParentOfType(CaosScriptArgument::class.java)
            ?.parameter
            ?.valuesList
            ?.get(variant)
            ?.name
            ?: return

        if (!name.startsWith("File.")) {
            return
        }
        if (!name.uppercase().contains("WAV")) {
            return
        }
        val filename = o.stringValue
        if (filename.length == 4) {
            return
        }
        problemsHolder.registerProblem(
            o,
            CaosBundle.message("caos.inspection.invalid-wav-file-name-length.message")
        )
    }
}