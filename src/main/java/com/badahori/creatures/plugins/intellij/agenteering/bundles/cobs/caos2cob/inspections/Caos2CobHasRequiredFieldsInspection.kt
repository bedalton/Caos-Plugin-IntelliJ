package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.inspections

import com.badahori.creatures.plugins.intellij.agenteering.att.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptInsertBeforeFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import kotlin.math.min

class Caos2CobHasRequiredFieldsInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = "Missing required CAOS2Cob properties"
    override fun getGroupDisplayName(): String = CaosBundle.message("cob.caos2cob.inspections.group")
    override fun getGroupPath(): Array<String> {
        return arrayOf(CaosBundle.message("caos.intentions.family"))
    }
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
        private fun validateBlock(element:CaosScriptCaos2Block, holder: ProblemsHolder) {
            if (!element.isCaos2Cob) {
                if (element.agentBlockNames.none { it.first in PRAY_AGENT_BLOCK_VARIANTS}) {
                    val error = CaosBundle.message("cob.caos2cob.inspections.missing-required-properties.missing-agent-name-command")
                    holder.registerProblem(element, error)
                }
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

            val textRange = element.caos2BlockHeader?.textRange
                ?: element.firstChild?.let {
                    TextRange(0, min(2, it.textLength))
                }

            if (missingTags.contains(CobTag.AGENT_NAME) && element.agentBlockNames.any { it.first == "C1" || it.first == "C2" }) {
                missingTags.remove(CobTag.AGENT_NAME)
            } else if (CobTag.AGENT_NAME in foundTags && element.agentBlockNames.none { it.first == "C1" || it.first == "C2" }) {
                val fixes = element.caos2BlockHeader?.let {header ->
                    val text = header.text.trim()
                    arrayOf(
                        CaosScriptReplaceElementFix(header, "$text C1", "Set variant to C1"),
                        CaosScriptReplaceElementFix(header, "$text C2", "Set variant to C2")
                    )
                } ?: arrayOf(
                    CaosScriptInsertBeforeFix(
                        "Set variant to C1",
                        "**CAOS2Cob C1",
                        element.children.first(),
                        '\n'
                    ),
                    CaosScriptInsertBeforeFix(
                        "Set variant to C2",
                        "**CAOS2Cob C2",
                        element.children.first(),
                        '\n'
                    )
                )
                holder.registerProblem(
                    element,
                    textRange,
                    CaosBundle.message("cob.caos2cob.inspections.missing-game-variant"),
                    *fixes
                )
            }
            if (missingTags.isEmpty())
                return

            val missingTagsString = missingTags.joinToString(", ") { it.keys.first() }
            holder.registerProblem(element,
                textRange,
                CaosBundle.message("cob.caos2cob.inspections.missing-required-properties", missingTagsString)
            )
        }
    }
}