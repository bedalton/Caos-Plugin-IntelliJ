package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.inspections


import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Cob
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.DeleteElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.AgentMessages
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Cob
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCaos2BlockComment
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCaos2CommandName
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CobCommand
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.utils.WHITESPACE
import com.badahori.creatures.plugins.intellij.agenteering.utils.levenshteinDistance
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.bedalton.common.util.toArrayOf
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor

class Caos2CobCommandIsValidInspection : LocalInspectionTool(), DumbAware {

    override fun getDisplayName(): String = "Invalid CAOS2Cob command"
    override fun getGroupDisplayName(): String = CAOS2Cob
    override fun getGroupPath(): Array<String> {
        return arrayOf(CaosBundle.message("caos.intentions.family"))
    }
    override fun getShortName(): String = "Caos2CobInvalidCommand"

    /**
     * Builds visitor for visiting and validating PsiElements related to this inspection
     */
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitCaos2CommandName(element: CaosScriptCaos2CommandName) {
                super.visitCaos2CommandName(element)
                validateCobCommandName(element, holder)
            }
        }
    }

    companion object {

        private val AGENT_NAME_COMMAND_REGEX = "(C1|C2)-?Name".toRegex(RegexOption.IGNORE_CASE)

        /**
         * Validates a COB comment directive, to ensure that it actually exists
         */
        private fun validateCobCommandName(element:CaosScriptCaos2CommandName, holder: ProblemsHolder) {
            if (!element.containingCaosFile?.isCaos2Cob.orFalse())
                return
            val tagNameRaw = element.text
            if (tagNameRaw.matches(AGENT_NAME_COMMAND_REGEX))
                return
            val tagName = element.text.replace(WHITESPACE, " ").nullIfEmpty()
                ?: return
            val command = CobCommand.fromString(tagName)
            val variant = element.variant
            if (command != null) {
                if (variant == null || command.variant == null || command.variant == variant)
                    return
                val error = AgentMessages.message("errors.caos2properties.out-of-variant-property", tagNameRaw, command.variant)
                val parent = element.getParentOfType(CaosScriptCaos2BlockComment::class.java)
                val fix = if (parent != null) {
                    DeleteElementFix(
                        AgentMessages.message("errors.caos2properties.out-of-variant-property.delete-element"),
                        parent
                    ).toArrayOf()
                } else emptyArray()
                holder.registerProblem(element, error, *fix)
                return
            }

            val similar = getFixesForSimilar(variant, element, tagName)
                .toTypedArray()
            val error = AgentMessages.message("errors..caos2properties.property-invalid.property-not-recognized", tagNameRaw, CAOS2Cob )
            holder.registerProblem(element, error, *similar)
        }

        private fun getFixesForSimilar(variant:CaosVariant?, element:PsiElement, tagName:String) : List<CaosScriptReplaceElementFix> {
            return CobCommand.getCommands(variant)
                .map { aTag ->
                    aTag.keyStrings.minByOrNull { it.levenshteinDistance(tagName) }!!.let { key ->
                        Pair(key, key.levenshteinDistance(tagName))
                    }
                }.filter {
                    it.second < 7
                }.map {
                    CaosScriptReplaceElementFix(
                        element,
                        it.first,
                        AgentMessages.message("caos2commands.fixes.replace-command", it.first)
                    )
                }
        }
    }
}