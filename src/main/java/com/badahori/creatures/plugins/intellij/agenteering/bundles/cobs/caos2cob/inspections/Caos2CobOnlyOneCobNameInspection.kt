package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.inspections

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Cob
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Path
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.DeleteElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.AgentMessages
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Cob
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.utils.startOffset
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

class Caos2CobOnlyOneCobNameInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = "Only one cob name"
    override fun getGroupDisplayName(): String = CAOS2Cob
    override fun getGroupPath(): Array<String> = CAOS2Path
    override fun getShortName(): String = "Caos2CobTooManyCobNames"

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
            val commands:List<CaosScriptCompositeElement> = element.caos2BlockCommentList.flatMap { blockCommentList -> blockCommentList.caos2CommandList.filter { CobCommand.fromString(it.commandName) == CobCommand.COBFILE }.flatMap { it.caos2ValueList }.filterNotNull() }
            val tags:List<CaosScriptCompositeElement> = element.caos2BlockCommentList.flatMap { blockCommentList -> blockCommentList.caos2TagList.filter { CobTag.fromString(it.tagName) == CobTag.COB_NAME }.map { it.caos2Value } }.filterNotNull()
            if (commands.size + tags.size <= 1) {
                return
            }
            val valuesRaw = (commands + tags)
                .sortedBy { it.startOffset }
            val first = valuesRaw.first()
            val error = AgentMessages.message("cob.caos2cob.inspections.only-one-cob-file-name.extraneous-cob-name")
            for(tag in valuesRaw.drop(0)) {
                val elementToDelete = tag.getParentOfType(CaosScriptCaos2Statement::class.java) ?: tag.getParentOfType(CaosScriptCaos2Command::class.java)?.let {
                    if (first in it.caos2ValueList) {
                        tag
                    } else
                        it
                }
                if (elementToDelete != null)
                    holder.registerProblem(element, error, DeleteElementFix(AgentMessages.message("cob.caos2cob.inspections.only-one-cob-file-name.fix.delete-extraneous-cob-name", tag.text), elementToDelete))
                else
                    holder.registerProblem(element, error)
            }
        }
    }
}