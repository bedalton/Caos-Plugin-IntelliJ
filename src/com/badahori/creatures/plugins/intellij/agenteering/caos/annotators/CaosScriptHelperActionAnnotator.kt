package com.badahori.creatures.plugins.intellij.agenteering.caos.annotators

import com.badahori.creatures.plugins.intellij.agenteering.caos.completion.GenerateBitFlagIntegerIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefValuesListElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.isVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptCollapseNewLineIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptExpandCommasIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CollapseChar
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.next
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.toIntSafe
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.variant
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor

/**
 * Adds helper actions to command elements
 */
class CaosScriptHelperActionAnnotator : LocalInspectionTool() {

    override fun getGroupDisplayName(): String = "CaosScript"
    override fun getShortName(): String = "CollapseExpandLines"
    override fun getDisplayName(): String = "Collapse Expand Lines"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor() {
            override fun visitCommandCall(o: CaosScriptCommandCall) {

                var child: PsiElement? = o.firstChild
                if (child != null && child !is CaosScriptCommandLike) {
                    child = child.firstChild
                }
                (child as? CaosScriptCAssignment)?.let {
                    val variant = o.containingFile.module?.variant ?: CaosVariant.UNKNOWN
                    annotateAssignment(variant, it, holder)
                }
                //Anotatte
                addExpandCollapseLinesActions(o, holder)
            }

            override fun visitSpaceLikeOrNewline(o: CaosScriptSpaceLikeOrNewline) {
                expandCollapseOnSpaceOrNewline(o, holder)
            }
        }
    }


    private fun addExpandCollapseLinesActions(element: CaosScriptCommandLike, holder: ProblemsHolder) {
        val next = element.next

        // If there are only commas next, simply allow for expansion
        if (next != null && COMMAS_ONLY_REGEX.matches(next.text)) {
            holder.registerProblem(element, "", ProblemHighlightType.INFORMATION, CaosScriptExpandCommasIntentionAction)
            // if next is a newline element, allow collapse with commas or spaces
        } else if (next is CaosScriptSpaceLikeOrNewline && next.textContains('\n')) {
            holder.registerProblem(element, "", ProblemHighlightType.INFORMATION, CaosScriptCollapseNewLineIntentionAction(CollapseChar.COMMA), CaosScriptCollapseNewLineIntentionAction(CollapseChar.SPACE))
        } else {
            // Next does not include commas or newlines
            // Always allow expand lines
            val fixes = mutableListOf<LocalQuickFix>(CaosScriptExpandCommasIntentionAction)
            // If there are newlines in file, also allow collapsing of lines
            if (element.containingFile.text.contains("\n")) {
                fixes.add(CaosScriptCollapseNewLineIntentionAction(CollapseChar.COMMA))
                fixes.add(CaosScriptCollapseNewLineIntentionAction(CollapseChar.SPACE))
            }
            holder.registerProblem(element, "", ProblemHighlightType.INFORMATION, *fixes.toTypedArray())
        }
    }

    /**
     *
     */
    private fun expandCollapseOnSpaceOrNewline(element: CaosScriptSpaceLikeOrNewline, holder: ProblemsHolder) {
        val fixes = mutableListOf<LocalQuickFix>()
        if (element.text == "," || element.text == " ") {
            fixes.add(CaosScriptExpandCommasIntentionAction)
        }
        if (element.text.contains("\n")) {
            fixes.add(CaosScriptCollapseNewLineIntentionAction(CollapseChar.COMMA))
            fixes.add(CaosScriptCollapseNewLineIntentionAction(CollapseChar.SPACE))
        }
        holder.registerProblem(element, "", ProblemHighlightType.INFORMATION, *fixes.toTypedArray())
    }

    private fun annotateAssignment(variant: CaosVariant, assignment: CaosScriptCAssignment, holder: ProblemsHolder) {
        val commandString = assignment.lvalue?.commandStringUpper.nullIfEmpty()
                ?: return
        val project = assignment.project
        val addTo = assignment.lastChild
        val currentValue = if (addTo is CaosScriptExpectsValueOfType) {
            addTo.text.toIntSafe() ?: 0
        } else {
            0
        }
        val valuesList = CaosDefCommandElementsByNameIndex
                .Instance[commandString, project]
                .filter {
                    it.isVariant(variant)
                }
                .mapNotNull {
                    it.returnTypeStruct?.type?.valuesList
                }
                .firstOrNull()
                ?: return
        val valuesListWithBitFlags = CaosDefValuesListElementsByNameIndex
                .Instance[valuesList, project]
                .firstOrNull {
                    it.isBitflags && it.isVariant(variant)
                }
                ?: return
        holder.registerProblem(addTo, "", ProblemHighlightType.INFORMATION, GenerateBitFlagIntegerIntentionAction(addTo, valuesListWithBitFlags.typeName, valuesListWithBitFlags.keys, currentValue))
    }

    companion object {
        private val COMMAS_ONLY_REGEX = "[,]+".toRegex()
    }
}