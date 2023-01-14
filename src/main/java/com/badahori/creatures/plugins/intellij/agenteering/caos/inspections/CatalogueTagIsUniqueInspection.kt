package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.bedalton.common.util.stripSurroundingQuotes
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.IgnoreCatalogueTag
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.IgnoreCatalogueTag.IgnoreScope
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptQuoteStringLiteral
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.token
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.indices.CatalogueEntryElementIndex
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueArray
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueItemName
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueTag
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueVisitor
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPreviousNonEmptySibling
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor


/**
 * Inspection to see if a Catalogue TAG exists
 * Looks for tag in catalogue files, ignored lists, or if its existence is checked in code
 */
class CatalogueTagIsUniqueInspection : LocalInspectionTool() {

    override fun isEnabledByDefault(): Boolean {
        return true
    }

    override fun getDisplayName(): String {
        return "Duplicate catalogue tag"
    }

    override fun getGroupDisplayName(): String {
        return "Catalogue"
    }

    override fun getShortName(): String {
        return "CatalogueTagIsUnique"
    }

    override fun getGroupPath(): Array<String> {
        return arrayOf(CAOSScript)
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CatalogueVisitor() {

            override fun visitItemName(o: CatalogueItemName) {
                super.visitItemName(o)
                validateTag(holder, o)
            }

        }
    }

    companion object {
        /**
         * Validates a catalogue TAG reference to see if it exists
         */
        private fun validateTag(holder: ProblemsHolder, element: PsiElement) {
            val tag = element.text.stripSurroundingQuotes()
            val project = element.project

            // Check for valid
            val raws = CatalogueEntryElementIndex.Instance[tag, project]

            val valid = raws.filter { it.isTag }.size <= 1 &&
                    raws.filter { it.isArray && (it as? CatalogueArray)?.isOverride == false }.size <= 1

            if (valid) {
                return
            }

            val message = CaosBundle.message("caos.inspection.duplicate-catalogue-tag.message", tag)
            holder.registerProblem(
                element,
                message
            )
        }
    }
}