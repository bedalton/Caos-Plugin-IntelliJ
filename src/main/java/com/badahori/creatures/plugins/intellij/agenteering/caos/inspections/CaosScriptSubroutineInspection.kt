package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.CaosScriptSubroutineIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptScriptBodyElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSubroutine
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSubroutineHeader
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptSubroutineName
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.hasParentOfType
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil

class CaosScriptSubroutineInspection : LocalInspectionTool(), DumbAware {

    override fun getDisplayName(): String = "Subroutine is defined"
    override fun getGroupDisplayName(): String = CaosBundle.message("caos.intentions.family")
    override fun getShortName(): String = "SubroutineIsDefined"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor() {
            override fun visitSubroutineName(subroutineName: CaosScriptSubroutineName) {
                super.visitSubroutineName(subroutineName)
                annotateSubroutineName(subroutineName, holder)
            }
        }
    }

    private fun annotateSubroutineName(element: CaosScriptSubroutineName, holder: ProblemsHolder) {
        val name = element.name
        val variant = element.containingCaosFile?.variant
                ?: return
        if ((variant == CaosVariant.C1 || variant == CaosVariant.C2) && name.length != 4) {
            holder.registerProblem(element, CaosBundle.message("caos.annotator.syntax-error-annotator.subroutine-name-invalid-length", variant))
        }
        if (element.hasParentOfType(CaosScriptSubroutineHeader::class.java))
            return
        val searchScope = GlobalSearchScope.fileScope(element.containingFile)
        val matches = CaosScriptSubroutineIndex.instance[name, element.project, searchScope]
        val parent = element.getParentOfType(CaosScriptScriptBodyElement::class.java)
                ?: return
        val range = element.textRange
        if (matches.any { it.getParentOfType(CaosScriptScriptBodyElement::class.java)?.textRange?.contains(range) == true } || manualSearch(parent, name)) {
            return
        }

        holder.registerProblem(element, CaosBundle.message("caos.annotator.syntax-error-annotator.subroutine-not-found", name))
    }

    private fun manualSearch(containingScript:CaosScriptScriptBodyElement, subroutineName:String) : Boolean {
        return PsiTreeUtil.collectElementsOfType(containingScript, CaosScriptSubroutine::class.java)
                .any {
                    it.name == subroutineName
                }
    }
}