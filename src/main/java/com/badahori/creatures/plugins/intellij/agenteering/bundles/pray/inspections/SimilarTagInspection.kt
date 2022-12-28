package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.inspections

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Path
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.PRAY
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayAgentBlock
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayTagTagName
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayVisitor
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support.PrayTags
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.AgentMessages
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Pray
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCaos2TagName
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.getParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import bedalton.creatures.common.util.stripSurroundingQuotes
import com.intellij.openapi.project.DumbAware
import kotlin.math.floor
import kotlin.math.min

class Caos2PraySimilarTagInspection : LocalInspectionTool(), DumbAware {

    override fun getGroupPath(): Array<String> = CAOS2Path
    override fun getGroupDisplayName(): String = PRAY
    override fun getDisplayName(): String = AgentMessages.message("pray.inspections.tags.similar-tags.display-name")

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitCaos2TagName(o: CaosScriptCaos2TagName) {
                super.visitCaos2TagName(o)
                if (o.containingCaosFile?.isCaos2Pray != true)
                    return
                validate(o, holder)
            }
        }
    }
}

class PrayTagFixSimilarInspection : LocalInspectionTool(), DumbAware {

    override fun getGroupPath(): Array<String> = arrayOf(CaosBundle.message("caos.intentions.family"))
    override fun getGroupDisplayName(): String = AgentMessages.message("pray.group")
    override fun getDisplayName(): String = AgentMessages.message("pray.inspections.tags.similar-tags.display-name")

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PrayVisitor() {
            override fun visitTagTagName(o: PrayTagTagName) {
                super.visitTagTagName(o)
                validate(o, holder)
            }
        }
    }
}


private fun validate(element: PsiElement, holder: ProblemsHolder) {
    val tag = element.text.trim().stripSurroundingQuotes()
    if (tag.isBlank())
        return


    // If tag is official return
    // If is badly cased official, return as it is handled elsewhere
    if (PrayTags.isOfficialTag(tag) || PrayTags.getCorrectedCase(tag) != null)
        return

    val thisTag = element.text.stripSurroundingQuotes()
    val isEggs = element.getParentOfType(PrayAgentBlock::class.java)?.blockTagString == "EGGS"
    val similar = PrayTags.getSimilarTags(thisTag, isEggs, min(floor(thisTag.length / 5.0).toInt(), 4))
        .nullIfEmpty()
        ?: return

    if (thisTag in similar) {
        return
    }
    val fixes = getSimilarTagFixes(element, similar)

    holder.registerProblem(
        element,
        AgentMessages.message("pray.inspections.tags.similar-tags.is-similar-message", "tag", similar.first() ),
        *fixes
    )
}

private fun getSimilarTagFixes(element: PsiElement, similar: List<String>): Array<CaosScriptReplaceElementFix> {
    val textRaw = element.text
    val isQuoted = textRaw.startsWith('"') || textRaw.startsWith('\'')
    return similar
        .map { newTag ->
            CaosScriptReplaceElementFix(
                element,
                if (isQuoted) "\"$newTag\"" else newTag,
                AgentMessages.message("cob.caos2cob.fix.replace-cob-tag", newTag)
            )
        }
        .toTypedArray()
}