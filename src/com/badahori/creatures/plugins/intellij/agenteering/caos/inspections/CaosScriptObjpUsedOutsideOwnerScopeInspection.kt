package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.equalsIgnoreCase
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.utils.orTrue
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor

class CaosScriptObjpUsedOutsideOwnerScopeInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = CaosBundle.message("caos.inspection.objp-outside-ownr-scope.display-name")
    override fun getGroupDisplayName(): String = CaosBundle.message("caos.intentions.family")
    override fun getShortName(): String = CaosBundle.message("caos.inspection.objp-outside-ownr-scope.short-name")

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitRKwNone(o: CaosScriptRKwNone) {
                super.visitRKwNone(o)
                if (o.kObjp != null)
                    annotateObjP(o, holder)
            }

            override fun visitLKwNone(o: CaosScriptLKwNone) {
                super.visitLKwNone(o)
                if (o.kObjp != null)
                    annotateObjP(o, holder)
            }
        }
    }

    private fun annotateObjP(element: PsiElement, holder: ProblemsHolder) {
        if ((element.containingFile as? CaosScriptFile)?.variant?.isNotOld.orFalse())
            return
        holder.registerProblem(element, CaosBundle.message("caos.inspection.objp-outside-ownr-scope.message"))
    }
}