package com.badahori.creatures.plugins.intellij.agenteering.bundles.inspections

import com.badahori.creatures.plugins.intellij.agenteering.att.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Cob
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCobCommentDirective
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CobTag
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor

class Caos2CobPropertyIsValidInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = "Invalid CAOS2Cob property"
    override fun getGroupDisplayName(): String = CaosBundle.message("cob.caos2cob.inspections.group")
    override fun getGroupPath(): Array<String> {
        return arrayOf(CaosBundle.message("caos.intentions.family"))
    }
    override fun getShortName(): String = "Caos2CobInvalidProperty"

    /**
     * Builds visitor for visiting and validating PsiElements related to this inspection
     */
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitCobCommentDirective(element: CaosScriptCobCommentDirective) {
                super.visitCobCommentDirective(element)
                validateCobCommentDirective(element, holder)
            }
        }
    }

    companion object {

        /**
         * Validates a COB comment directive, to ensure that it actually exists
         */
        private fun validateCobCommentDirective(element:CaosScriptCobCommentDirective, holder: ProblemsHolder) {
            if (!element.containingCaosFile?.isCaos2Cob.orFalse())
                return
            val tagNameRaw = element.text
            val tagName = element.text.replace(WHITESPACE_OR_DASH, " ")
            val tag = CobTag.fromString(tagName)
            val variant = element.variant
            if (tag != null) {
                return
            }
            val similar = getFixesForSimilar(variant, element, tagName)
                .toTypedArray()
            val error = "'$tagNameRaw' is not a recognized COB property"
            holder.registerProblem(element, error, *similar)
        }

        private fun getFixesForSimilar(variant:CaosVariant?, element:PsiElement, tagName:String) : List<CaosScriptReplaceElementFix> {
            return CobTag.getTags(variant)
                .mapNotNull { aTag ->
                    aTag.keys.map { key ->
                        Pair(key, key.levenshteinDistance(tagName))
                    }.minBy { it.second }
                }.filter {
                    it.second < 5
                }.map {
                    CaosScriptReplaceElementFix(
                        element,
                        it.first,
                        CaosBundle.message("cob.caos2cob.fix.replace-cob-tag", it.first)
                    )
                }
        }
    }
}