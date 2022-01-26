package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptBinaryLiteral
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCharacter
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRvalue
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptPsiElementFactory
import com.badahori.creatures.plugins.intellij.agenteering.utils.getSelfOrParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.binaryToInteger
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor

class CaosScriptInvalidNumericExpressionInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = CaosBundle.message("caos.inspection.invalid-numeric-expression.display-name")
    override fun getGroupDisplayName(): String = CAOSScript
    override fun getShortName(): String = CaosBundle.message("caos.inspection.invalid-numeric-expression.short-name")

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitBinaryLiteral(o: CaosScriptBinaryLiteral) {
                super.visitBinaryLiteral(o)
                val variant = o.variant ?: return
                if (variant.isNotOld)
                    return
                holder.registerProblem(o, CaosBundle.message("caos.inspection.invalid-numeric-expression.invalid-binary-number"), Fix)
            }

            override fun visitCharacter(o: CaosScriptCharacter) {
                super.visitCharacter(o)
                val variant = o.variant ?: return
                if (variant.isNotOld)
                    return
                holder.registerProblem(o, CaosBundle.message("caos.inspection.invalid-numeric-expression.invalid-char-literal"), Fix)
            }
        }
    }

}

object Fix : LocalQuickFix {
    override fun getFamilyName(): String = CaosBundle.message("caos.intentions.family")
    override fun getName(): String = "Replace with integer value"
    override fun startInWriteAction(): Boolean = true
    override fun applyFix(project: Project, problemDescriptor: ProblemDescriptor) {
        val element = problemDescriptor.psiElement
        val text = element.text
        if (text.length < 2)
            return
        val value = if (element is CaosScriptBinaryLiteral)
            binaryToInteger(element.text.substring(1))
        else
            text.toCharArray()[1].code.toLong()
        val newElement = CaosScriptPsiElementFactory.createNumber(element.project, value)
        element.getSelfOrParentOfType(CaosScriptRvalue::class.java)?.replace(newElement)
    }

}