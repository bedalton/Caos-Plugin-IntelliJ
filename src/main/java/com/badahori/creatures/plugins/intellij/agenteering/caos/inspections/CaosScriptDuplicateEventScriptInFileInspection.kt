package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.endOffsetInParent
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.startOffset
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil

class CaosScriptDuplicateEventScriptInFileInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = CaosBundle.message("caos.inspection.duplicate-event-number-in-file.display-name")
    override fun getGroupDisplayName(): String = CaosBundle.message("caos.intentions.family")
    override fun getShortName(): String = CaosBundle.message("caos.inspection.duplicate-event-number-in-file.short-name")
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitEventScript(o: CaosScriptEventScript) {
                validate(o, holder)
                super.visitEventScript(o)
            }
        }
    }

    private fun validate(thisEventScript: CaosScriptEventScript, problemsHolder: ProblemsHolder) {
        val family = thisEventScript.family
        val genus = thisEventScript.genus
        val species = thisEventScript.species
        val eventNumber = thisEventScript.eventNumber
        val startIndex = thisEventScript.startOffset
        val containingFile = thisEventScript.containingFile
        val exists = PsiTreeUtil.collectElementsOfType(containingFile, CaosScriptEventScript::class.java)
                .filterNot { thisEventScript.isEquivalentTo(it) }
                .any {
                    it.startOffset < startIndex && it.family == family && it.genus == genus && it.species == species
                }
        if (exists) {
            val endOffset = thisEventScript.eventNumberElement?.endOffsetInParent
                    ?: thisEventScript.textLength - 1
            val textRangeInParent = TextRange(thisEventScript.cScrp.endOffsetInParent + 1, endOffset)
            problemsHolder.registerProblem(thisEventScript, textRangeInParent, CaosBundle.message("caos.inspection.duplicate-event-number-in-file.message", family, genus, species, eventNumber))
        }
    }
}