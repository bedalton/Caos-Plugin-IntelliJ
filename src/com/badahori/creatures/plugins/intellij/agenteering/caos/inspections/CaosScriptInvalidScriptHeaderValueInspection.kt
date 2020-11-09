package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.toIntSafe
import com.badahori.creatures.plugins.intellij.agenteering.utils.upperCaseFirstLetter
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor

class CaosScriptInvalidScriptHeaderValueInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = CaosBundle.message("caos.inspection.invalid-script-header-value.display-name")
    override fun getGroupDisplayName(): String = CaosBundle.message("caos.intentions.family")
    override fun getShortName(): String = CaosBundle.message("caos.inspection.invalid-script-header-value.short-name")
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitFamily(o: CaosScriptFamily) {
                if (o.parent?.parent is CaosScriptEventScript)
                    validate(o, "family", holder)
                super.visitFamily(o)
            }

            override fun visitGenus(o: CaosScriptGenus) {
                if (o.parent?.parent is CaosScriptEventScript)
                    validate(o, "genus", holder)
                super.visitGenus(o)
            }

            override fun visitSpecies(o: CaosScriptSpecies) {
                if (o.parent?.parent is CaosScriptEventScript)
                    validate(o, "species", holder)
                super.visitSpecies(o)
            }

            override fun visitEventNumberElement(o: CaosScriptEventNumberElement) {
                if (o.parent is CaosScriptEventScript)
                    validate(o, "event number", holder)
                super.visitEventNumberElement(o)
            }
        }
    }

    private fun validate(o: PsiElement, name: String, problemsHolder: ProblemsHolder) {
        val variant = (o.containingFile as? CaosScriptFile)?.variant
                ?: return
        val value = o.text.toIntSafe()
        if (value == null) {
            problemsHolder.registerProblem(o, CaosBundle.message("caos.inspection.invalid-script-header-value.value-is-not-constant.message", name.upperCaseFirstLetter()))
            return
        }
        val max = if (variant == CaosVariant.C1) 255 else 65535

        if (max >= value)
            return
        problemsHolder.registerProblem(o, CaosBundle.message("caos.inspection.invalid-script-header-value.value-over-max.message", name.upperCaseFirstLetter(), max, value % max))
    }
}