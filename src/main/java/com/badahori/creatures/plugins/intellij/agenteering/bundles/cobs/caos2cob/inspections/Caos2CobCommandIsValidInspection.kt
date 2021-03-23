package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.inspections

import com.badahori.creatures.plugins.intellij.agenteering.att.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Cob
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor

class Caos2CobCommandIsValidInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = "Invalid CAOS2Cob command"
    override fun getGroupDisplayName(): String = CaosBundle.message("cob.caos2cob.inspections.group")
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

        val COB_NAME_COMMAND_REGEX = "(C1|C2)-?Name".toRegex(RegexOption.IGNORE_CASE)

        /**
         * Validates a COB comment directive, to ensure that it actually exists
         */
        private fun validateCobCommandName(element:CaosScriptCaos2CommandName, holder: ProblemsHolder) {
            if (!element.containingCaosFile?.isCaos2Cob.orFalse())
                return
            val tagNameRaw = element.text
            if (tagNameRaw.matches(COB_NAME_COMMAND_REGEX))
                return
            val tagName = element.text.replace(WHITESPACE_OR_DASH, " ").nullIfEmpty()
                ?: return
            val command = CobCommand.fromString(tagName)
            val variant = element.variant
            if (command != null) {
                return
            }
            val similar = getFixesForSimilar(variant, element, tagName)
                .toTypedArray()
            val error = "'$tagNameRaw' is not a recognized COB command"
            holder.registerProblem(element, error, *similar)
        }

        private fun getFixesForSimilar(variant:CaosVariant?, element:PsiElement, tagName:String) : List<CaosScriptReplaceElementFix> {
            return CobCommand.getCommands(variant)
                .map { aTag ->
                    aTag.keyString.let { key ->
                        Pair(key, key.levenshteinDistance(tagName))
                    }
                }.filter {
                    it.second < 7
                }.map {
                    CaosScriptReplaceElementFix(
                        element,
                        it.first,
                        CaosBundle.message("cob.caos2cob.fix.replace-cob-command", it.first)
                    )
                }
        }
    }
}