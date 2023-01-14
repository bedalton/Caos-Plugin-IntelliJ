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
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosApplicationSettingsService
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.settings
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.token
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.indices.CatalogueEntryElementIndex
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueArray
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPreviousNonEmptySibling
import com.badahori.creatures.plugins.intellij.agenteering.utils.settings
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor


/**
 * Inspection to see if a Catalogue TAG exists
 * Looks for tag in catalogue files, ignored lists, or if its existence is checked in code
 */
class CaosCatalogueTagIsUniqueInspection : LocalInspectionTool() {

    override fun isEnabledByDefault(): Boolean {
        return true
    }

    override fun getDisplayName(): String {
        return "Referencing non-unique catalogue tag"
    }

    override fun getGroupDisplayName(): String {
        return CAOSScript
    }

    override fun getGroupKey(): String {
        return CAOSScript
    }

    override fun getShortName(): String {
        return "CaosCatalogueTagIsUnique"
    }

    override fun getGroupPath(): Array<String> {
        return arrayOf(CAOSScript)
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {

            override fun visitQuoteStringLiteral(o: CaosScriptQuoteStringLiteral) {
                if (o.variant?.isC3DS != true) {
                    return
                }

                val previous = o.getPreviousNonEmptySibling(false)
                    ?: return

                if (token(previous) !in hasTags) {
                    return
                }
                validateTag(holder, o)
                super.visitQuoteStringLiteral(o)
            }

        }
    }

    companion object {

        private val hasTags = listOf(
            token("read")
        )

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

            if (valid || ignored(holder, project, element, tag)) {
                return
            }


            val message = CaosBundle.message("caos.inspection.duplicate-catalogue-tag.message", tag)

            val quickFixes = listOfNotNull(
                (if (tag !in project.settings.ignoredCatalogueTags) IgnoreCatalogueTag(tag, true, IgnoreScope.PROJECT) else null),
                (if (tag !in CaosApplicationSettingsService.getInstance().ignoredCatalogueTags) IgnoreCatalogueTag(tag, true, IgnoreScope.APPLICATION) else null),
                element.module?.let { if (tag !in it.settings.ignoredCatalogueTags) IgnoreCatalogueTag(tag, true, IgnoreScope.MODULE) else null }
            )
            holder.registerProblem(
                element,
                message,
                *quickFixes.toTypedArray()
            )
        }
        fun token(psiElement: PsiElement): Int? {
            return if (psiElement.textLength != 4) {
                null
            } else {
                token(psiElement.text)
            }
        }

        private fun ignored(holder: ProblemsHolder, project: Project, element: PsiElement, tag: String): Boolean {

            var valid = false
            // Check if tag is in ignored tags at APPLICATION level
            // Add quick-fix to remove it from list if in ignored list
            if (tag in CaosApplicationSettingsService.getInstance().ignoredCatalogueTags) {
                valid = true
                holder.registerProblem(
                    element,
                    "",
                    ProblemHighlightType.INFORMATION,
                    IgnoreCatalogueTag(tag, false, IgnoreScope.APPLICATION)
                )
            }

            // Check if tag is in ignored tags at PROJECT level
            // Add quick-fix to remove it from list if in ignored list
            if (tag in project.settings.ignoredCatalogueTags) {
                valid = true
                holder.registerProblem(
                    element,
                    "",
                    ProblemHighlightType.INFORMATION,
                    IgnoreCatalogueTag(tag, false, IgnoreScope.PROJECT)
                )
            }

            // Check if tag is in ignored tags at MODULE level
            // Add quick-fix to remove it from list if in ignored list
            if (tag in element.module?.settings?.ignoredCatalogueTags.orEmpty()) {
                valid = true
                holder.registerProblem(
                    element,
                    "",
                    ProblemHighlightType.INFORMATION,
                    IgnoreCatalogueTag(tag, false, IgnoreScope.PROJECT)
                )
            }
            return valid
        }
    }
}