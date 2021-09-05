package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.inspections

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Path
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Pray
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support.PrayTags
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.DeleteElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.AgentMessages
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Pray
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCaos2BlockComment
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCaos2Tag
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil

class Caos2PrayReservedTagUseInspection : LocalInspectionTool() {

    override fun getGroupPath(): Array<String> = CAOS2Path
    override fun getGroupDisplayName(): String = CAOS2Pray
    override fun getDisplayName(): String = AgentMessages.message("pray.inspections.tags.reserved-tag-use.display-name")

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitCaos2Tag(o: CaosScriptCaos2Tag) {
                super.visitCaos2Tag(o)
                validateCommentDirective(o, holder)
            }
        }
    }

    private fun validateCommentDirective(element: CaosScriptCaos2Tag, holder: ProblemsHolder) {
        val file = element.containingCaosFile
            ?: return
        if (!file.isCaos2Pray)
            return
        val tag = element.tagName.trim()
        if (tag.isBlank())
            return
        val usesRscrCommand = if (tag.toLowerCase() == "remove script") {
            PsiTreeUtil.collectElementsOfType(file, CaosScriptCaos2BlockComment::class.java)
                .any { blockComment ->
                    blockComment.caos2CommandList.any {
                        val commandName = it.commandName.toLowerCase()
                        commandName.toLowerCase() == "rscr" || PrayTags.normalize(commandName)
                            ?.toLowerCase() == "remove script"
                    } || blockComment.caos2TagList.any {
                        val tagName = it.tagName
                        tagName.toLowerCase() == "rscr" || PrayTags.normalize(tagName)?.toLowerCase() == "remove script"
                    }
                }
        } else {
            false
        }
        if (!PrayTags.isAutogenerated(tag, usesRscrCommand)) {
            return
        }
        val parent = element.getParentOfType(CaosScriptCaos2BlockComment::class.java)
        val value = element.valueAsString
        val fixes: Array<LocalQuickFix> = if (parent == null || value == null) {
            emptyArray()
        } else if (SCRIPT_TAG.matches(tag)) {
            arrayOf(
                CaosScriptReplaceElementFix(
                    parent,
                    "*# Link \"${value}\"",
                    "Use Link command instead"
                )
            )
        } else if (DEPENDENCY_TAG.matches(tag)) {
            arrayOf(
                CaosScriptReplaceElementFix(
                    parent,
                    "*# Depend \"${value}\"",
                    "Define dependency with Depend tag"
                ),
                CaosScriptReplaceElementFix(
                    parent,
                    "*# Inline \"${value}\"",
                    "Inline dependency with \"Inline\" tag"
                ),
                CaosScriptReplaceElementFix(
                    parent,
                    "*# Attach \"${value}\"",
                    "Attach dependency with \"Attach\" command"
                )
            )
        } else if (REMOVAL_TAG.matches(tag) && !usesRscrCommand) {
            arrayOf(
                CaosScriptReplaceElementFix(
                    parent,
                    "*# Rscr \"${value}\"",
                    "Use \"Rscr\" command"
                )
            )
        } else {
            arrayOf(DeleteElementFix("Remove restricted tag", parent))
        }

        // If tag is autogenerated, mark it as an error and offer to remove it
        holder.registerProblem(
            element,
            AgentMessages.message("pray.inspections.tags.reserved-tag-use.message", tag),
            *fixes
        )

    }

    companion object {
        private val SCRIPT_TAG = "Script\\s+\\d+".toRegex(RegexOption.IGNORE_CASE)
        private val REMOVAL_TAG = "Remove\\s+Script".toRegex(RegexOption.IGNORE_CASE)
        private val DEPENDENCY_TAG = "Dependency\\s+Script".toRegex(RegexOption.IGNORE_CASE)
    }


}