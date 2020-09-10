package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptEventScriptIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.endOffset
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.endOffsetInParent
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.startOffset
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor

class CaosScriptDuplicateEventScriptInModuleInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = CaosBundle.message("caos.inspection.duplicate-event-number-in-module.display-name")
    override fun getGroupDisplayName(): String = CaosBundle.message("caos.intentions.family")
    override fun getShortName(): String = CaosBundle.message("caos.inspection.duplicate-event-number-in-module.short-name")
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitEventScript(o: CaosScriptEventScript) {
                validate(o, holder)
                super.visitEventScript(o)
            }
        }
    }

    private fun validate(thisEventScript: CaosScriptEventScript, problemsHolder: ProblemsHolder) {
        val variant = thisEventScript.containingCaosFile?.variant
                ?: return
        val family = thisEventScript.family
        val genus = thisEventScript.genus
        val species = thisEventScript.species
        val eventNumber = thisEventScript.eventNumber
        val key = CaosScriptEventScriptIndex.toIndexKey(family, genus, species, eventNumber)
        val containingFile = thisEventScript.containingFile
        val moduleFilePath = thisEventScript.containingFile?.module?.moduleFilePath
        val exists = CaosScriptEventScriptIndex.instance[key, thisEventScript.project]
                .any {anEventScript ->
                    // Checks against containing file, as duplicate event numbers in a single file
                    // is covered in another inspection
                    anEventScript.containingFile?.let { aFile ->
                        !aFile.isEquivalentTo(containingFile) && aFile.module?.moduleFilePath == moduleFilePath
                    }.orFalse()
                }
        if (exists) {
            val endOffset = thisEventScript.eventNumberElement?.endOffsetInParent
                    ?: thisEventScript.textLength - 1
            val textRangeInParent = TextRange(thisEventScript.cScrp.endOffsetInParent + 1, endOffset)
            problemsHolder.registerProblem(thisEventScript, textRangeInParent, CaosBundle.message("caos.inspection.duplicate-event-number-in-module.message", family, genus, species, eventNumber))
        }
    }
}