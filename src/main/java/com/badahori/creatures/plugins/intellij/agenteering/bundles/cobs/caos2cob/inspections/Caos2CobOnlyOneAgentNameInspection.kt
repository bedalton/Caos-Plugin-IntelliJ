package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.inspections

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Cob
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Path
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.DeleteElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.AgentMessages
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Cob
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.getParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.utils.startOffset
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil

class Caos2CobOnlyOneAgentNameInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = "Only one agent name"
    override fun getGroupDisplayName(): String = CAOS2Cob
    override fun getGroupPath() = CAOS2Path
    override fun getShortName(): String = "Caos2CobTooManyAgentNames"

    /**
     * Builds visitor for visiting and validating PsiElements related to this inspection
     */
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {

            override fun visitCaos2Block(element: CaosScriptCaos2Block) {
                super.visitCaos2Block(element)

                validateAgentNames(element, holder)
            }
        }
    }

    companion object {
        /**
         * Validates a COB agent names to ensure only one tag name is used.
         */
        private fun validateAgentNames(element:CaosScriptCaos2Block, holder: ProblemsHolder) {
            if (!element.containingCaosFile?.isCaos2Cob.orFalse())
                return
            val agentNameTuples = element.agentBlockNames
            if (agentNameTuples.size == 1)
                return
            val agentNameTags = agentNameTuples.map { it.first }.toSet()
            val tags = PsiTreeUtil.collectElementsOfType(element, CaosScriptCaos2Statement::class.java)
                .filter { item ->
                    if (item is CaosScriptCaos2Tag)
                        item.tagName in agentNameTags
                    else
                        (item as CaosScriptCaos2Command).commandName in agentNameTags
                }
                .map {
                    it.firstChild
                }
                .sortedBy {
                    it.startOffset
                }
                .drop(1)
            val error = AgentMessages.message("cob.caos2cob.inspections.only-one-agent.extraneous-agent-name")
            for(tag in tags) {
                val parentTag = tag.getParentOfType(CaosScriptCaos2Statement::class.java)
                if (parentTag != null)
                    holder.registerProblem(element, error, DeleteElementFix(AgentMessages.message("cob.caos2cob.inspections.only-one-agent.fix.delete-extraneous-tag", tag.text), parentTag))
                else
                    holder.registerProblem(element, error)
            }
        }
    }
}