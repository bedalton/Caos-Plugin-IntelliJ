package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.inspections

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Path
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Pray
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.PRAY
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayTagTagName
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayVisitor
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support.PrayTags
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.AgentMessages
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Pray
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCaos2TagName
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import bedalton.creatures.util.stripSurroundingQuotes

class Caos2PrayTagCaseInspection : LocalInspectionTool() {

    override fun getGroupPath(): Array<String> = CAOS2Path
    override fun getGroupDisplayName(): String = CAOS2Pray
    override fun getDisplayName(): String = AgentMessages.message("pray.inspections.tags.incorrect-case.display-name")
    override fun getShortName(): String = AgentMessages.message("pray.caos2pray.inspections.tags.incorrect-case.short-name")

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitCaos2TagName(o: CaosScriptCaos2TagName) {
                super.visitCaos2TagName(o)
                if (o.containingCaosFile?.isCaos2Pray != true)
                    return
                check(o, holder)
            }
        }
    }
}


class PrayTagCaseInspection : LocalInspectionTool() {

    override fun getGroupPath(): Array<String> = CAOS2Path
    override fun getGroupDisplayName(): String = PRAY
    override fun getDisplayName(): String = AgentMessages.message("pray.inspections.tags.incorrect-case.display-name")
    override fun getShortName(): String = AgentMessages.message("pray.inspections.tags.incorrect-case.short-name")

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PrayVisitor() {
            override fun visitTagTagName(o: PrayTagTagName) {
                super.visitTagTagName(o)
                check(o, holder)
            }
        }
    }
}


private fun check(element: PsiElement, holder: ProblemsHolder) {
    val tag = element.text.trim().stripSurroundingQuotes()
    if (tag.isBlank())
        return

    val correctedCase = PrayTags.getCorrectedCase(tag)
        ?: return
    val fixText = if (element.text.startsWith('"') || element.text.startsWith('\'')) {
        "\"$correctedCase\""
    } else {
        correctedCase
    }
    holder.registerProblem(
        element,
        AgentMessages.message("pray.inspections.tags.incorrect-case.error", correctedCase),
        CaosScriptReplaceElementFix(element, fixText)
    )
}