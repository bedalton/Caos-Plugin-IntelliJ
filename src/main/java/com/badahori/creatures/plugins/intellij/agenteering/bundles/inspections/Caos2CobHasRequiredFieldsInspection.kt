package com.badahori.creatures.plugins.intellij.agenteering.bundles.inspections

import com.badahori.creatures.plugins.intellij.agenteering.att.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Cob
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor

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

        /**
         * Validates a COB comment directive, to ensure that it actually exists
         */
        private fun validateBlock(element:CaosScriptCaos2Block, holder: ProblemsHolder) {
            val variant = element.cobVariant
                ?: return
            val tagStrings = element.tags.keys
            val foundTags = tagStrings.mapNotNull { key -> CobTag.fromString(key) }
            val missingTags = mutableListOf<String>()
            for (tag in CobTag.getTags(variant).filter { it.required }) {
                if (tag !in foundTags)
                    missingTags.add(tag.keys.first())
            }
            if (missingTags.isEmpty())
                return

            holder.registerProblem(element.caos2BlockHeader,
                CaosBundle.message("cob.caos2cob.inspections.missing-required-properties", missingTags.joinToString(", "))
            )

        }

        private fun getFixesForSimilar(variant:CaosVariant?, element:PsiElement, tagName:String) : List<CaosScriptReplaceElementFix> {
            return CobTag.getTags(variant)
                .mapNotNull { aTag ->
                    aTag.keys.map { key ->
                        Pair(key, key.levenshteinDistance(tagName))
                    }.minBy { it.second }
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