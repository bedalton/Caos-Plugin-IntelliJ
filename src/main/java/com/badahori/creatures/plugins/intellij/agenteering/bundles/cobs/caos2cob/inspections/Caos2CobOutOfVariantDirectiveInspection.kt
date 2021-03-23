package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.inspections

import com.badahori.creatures.plugins.intellij.agenteering.att.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Cob
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

/**
 * Detects the usage of a CAOS2Cob property that does not apply to this variant
 */
class Caos2CobOutOfVariantDirectiveInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = "Out of variant CAOS2Cob directive"
    override fun getGroupDisplayName(): String = CaosBundle.message("cob.caos2cob.inspections.group")
    override fun getGroupPath(): Array<String> {
        return arrayOf(CaosBundle.message("caos.intentions.family"))
    }
    override fun getShortName(): String = "Caos2CobOutOfVariantDirective"

    /**
     * Builds visitor for visiting and validating PsiElements related to this inspection
     */
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitCobCommentDirective(element: CaosScriptCobCommentDirective) {
                super.visitCobCommentDirective(element)
                validateCobCommentDirective(element, holder)
            }

            override fun visitCaos2CommandName(o: CaosScriptCaos2CommandName) {
                super.visitCaos2CommandName(o)
                validateCobCommand(o, holder)
            }
        }
    }

    companion object {
        /**
         * Validates a COB comment directive to ensure that it is valid for this variant
         */
        fun validateCobCommentDirective(element:CaosScriptCobCommentDirective, holder: ProblemsHolder) {
            if (!element.containingCaosFile?.isCaos2Cob.orFalse())
                return
            val tagNameRaw = element.text
            val tagName = element.text.replace(WHITESPACE_OR_DASH, " ")
            val tag = CobTag.fromString(tagName)
                ?: return
            if (tag.variant == null)
                return
            val variant = element.variant
                ?: return
            // Variants must match to be valid
            if (tag.variant != variant) {
                holder.registerProblem(
                    element,
                    CaosBundle.message(
                        "cob.caos2cob.inspections.property-valid.variant-mismatch",
                        tagNameRaw,
                        tag.variant.code
                    )
                )
            }
        }
        /**
         * Validates a COB comment directive to ensure that it is valid for this variant
         */
        fun validateCobCommand(element:CaosScriptCaos2CommandName, holder: ProblemsHolder) {
            if (!element.containingCaosFile?.isCaos2Cob.orFalse())
                return
            val tagNameRaw = element.text
            val tagName = element.text.replace(WHITESPACE_OR_DASH, " ")
            val tag = CobCommand.fromString(tagName)
                ?: return
            if (tag.variant == null)
                return
            val variant = element.variant
                ?: return
            // Variants must match to be valid
            if (tag.variant != variant) {
                holder.registerProblem(
                    element,
                    CaosBundle.message(
                        "cob.caos2cob.inspections.command-valid.variant-mismatch",
                        tagNameRaw,
                        tag.variant.code
                    )
                )
            }
        }
    }
}