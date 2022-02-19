package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.inspections

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Cob
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Path
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptInsertAfterFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptInsertBeforeFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.AgentMessages
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.IS_SUPPLEMENT_KEY
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isSupplement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCaos2Block
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CobCommand
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CobTag
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import kotlin.math.min

class Caos2CobHasRequiredFieldsInspection : LocalInspectionTool() {

    override fun getDisplayName(): String =
        AgentMessages.message("inspections.caos-to-compiler.required-fields.display-name")

    override fun getGroupDisplayName(): String = CAOS2Cob
    override fun getGroupPath(): Array<String> = CAOS2Path

    override fun getShortName(): String = "Caos2CobMissingRequiredProperties"

    /**
     * Builds visitor for visiting and validating PsiElements related to this inspection
     */
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitCaos2Block(element: CaosScriptCaos2Block) {
                super.visitCaos2Block(element)
                validateBlock(element, holder)
            }
        }
    }

    companion object {

        private val PRAY_AGENT_BLOCK_VARIANTS = listOf("CV", "C3", "DS", "SM")

        /**
         * Validates a COB comment directive, to ensure that it actually exists
         */
        private fun validateBlock(element: CaosScriptCaos2Block, holder: ProblemsHolder) {
            if (!element.isCaos2Cob) {
                return
            }
            if (element.containingCaosFile?.isSupplement == true) {
                return
            }
            val variant = element.cobVariant
            val tagStrings = element.tags.keys
            val cobFile = if (element.commands.any { CobCommand.fromString(it.first, variant) == CobCommand.COBFILE })
                listOf(CobTag.COB_NAME)
            else
                emptyList()

            val foundTags = tagStrings.mapNotNull { key -> CobTag.fromString(key) } + cobFile
            val missingTags = mutableListOf<CobTag>()
            for (tag in CobTag.getTags(variant).filter { it.required }) {
                if (tag !in foundTags)
                    missingTags.add(tag)
            }


            // Find text range for which to show missing variant error
            val errorTextRange = element.caos2BlockHeader?.textRange
                ?: element.firstChild?.let {
                    TextRange(0, min(2, it.textLength))
                }

            if (missingTags.contains(CobTag.AGENT_NAME) && element.agentBlockNames.any { it.first == "C1" || it.first == "C2" }) {
                missingTags.remove(CobTag.AGENT_NAME)
            } else if (CobTag.AGENT_NAME in foundTags && element.agentBlockNames.none { it.first == "C1" || it.first == "C2" }) {
                val fixes = element.caos2BlockHeader?.let { header ->
                    arrayOf(
                        CaosScriptReplaceElementFix(header,
                            "**CAOS2Cob C1",
                            AgentMessages.message("error.missing-game-variant.fix-text", CAOS2Cob, "C1")),
                        CaosScriptReplaceElementFix(header,
                            "**CAOS2Cob C2",
                            AgentMessages.message("error.missing-game-variant.fix-text", CAOS2Cob, "C2"))
                    )
                } ?: arrayOf(
                    CaosScriptInsertBeforeFix(
                        AgentMessages.message("error.missing-game-variant.fix-text", CAOS2Cob, "C1"),
                        "**CAOS2Cob C1",
                        element.children.first(),
                        '\n'
                    ),
                    CaosScriptInsertBeforeFix(
                        AgentMessages.message("error.missing-game-variant.fix-text", CAOS2Cob, "C2"),
                        "**CAOS2Cob C2",
                        element.children.first(),
                        '\n'
                    )
                )
                // Show missing variant error with fixes
                holder.registerProblem(
                    element,
                    errorTextRange,
                    AgentMessages.message("error.missing-game-variant", CAOS2Cob),
                    *fixes
                )
            }
            if (missingTags.isEmpty())
                return

            val missingTagsString = missingTags.joinToString(", ") { it.keys.first() }
            holder
                .registerProblem(element,
                    errorTextRange,
                    AgentMessages.message("error.caos-to-compiler.required-fields.missing-required-properties",
                        CAOS2Cob,
                        missingTagsString),
                    CaosScriptInsertAfterFix("Mark as a linked file", "\n**Is Linked", element) {
                        element.containingCaosFile?.putUserData(IS_SUPPLEMENT_KEY, null)
                    }
                )
        }
    }
}