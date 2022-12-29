package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import bedalton.creatures.common.util.stripSurroundingQuotes
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.IgnoreCatalogueTag
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.IgnoreCatalogueTag.IgnoreScope
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.EqOp
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosApplicationSettingsService
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.settings
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.toTokenOrNull
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.token
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.indices.CatalogueEntryElementIndex
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
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
class CatalogueTagExistsInspection : LocalInspectionTool() {

    override fun isEnabledByDefault(): Boolean {
        return true
    }

    override fun getDisplayName(): String {
        return "Catalogue tag does not exist"
    }

    override fun getGroupDisplayName(): String {
        return CAOSScript
    }

    override fun getGroupKey(): String {
        return CAOSScript
    }

    override fun getShortName(): String {
        return "CaosCatalogueTagIsValid"
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
        private const val similarityThreshold = 6

        private val REAQ_TOKEN = token("reaq")

        private val hasTags = listOf(
            token("read"),
            token("rean")
        )

        /**
         * Validates a catalogue TAG reference to see if it exists
         */
        private fun validateTag(holder: ProblemsHolder, element: PsiElement) {
            val tag = element.text.stripSurroundingQuotes()
            val project = element.project
            val tags = CatalogueEntryElementIndex.Instance.getAllKeys(project)

            // Check for valid
            val valid = isValid(holder, project, element, tags, tag)

            if (valid) {
                return
            }

            val nearby = tags.map { Pair(it, it.levenshteinDistance(tag)) }
                .sortedBy { it.second }
                .filter { it.second < similarityThreshold }
            val closest = nearby.subList(0, minOf(nearby.size, 3))
            val message = CaosBundle.message("caos.inspection.catalogue-exists.message", tag)
            val quickFixes = closest.map { (tag, _) ->
                CaosScriptReplaceElementFix(
                    element,
                    "\"$tag\"",
                    CaosBundle.message("caos.inspection.catalogue-exists.fix", tag)
                )
            } + listOfNotNull(
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

        /**
         * Checks if a given tag exists in a catalogue or in an ignored tag list
         */
        private fun isValid(
            holder: ProblemsHolder,
            project: Project,
            element: PsiElement,
            tags: Collection<String>,
            tag: String
        ): Boolean {

            var valid = false

            // Check that tag exists naturally in Catalogue files
            if (tags.any { it == tag }) {
                valid = true
            }

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

            // Finally, check if Tag is checked in a DOIF statement
            if (!valid && isChecked(element, tag)) {
                valid = true
            }

            // Return whether the Tag exists or is checked in DOIF
            return valid
        }

        private fun isChecked(element: PsiElement, tag: String): Boolean {
            val parentScript = element.getParentOfType(CaosScriptScriptElement::class.java)
                ?: return false
            val startIndex = parentScript.startOffset
            var parentBlock = element.getParentOfType(CaosScriptHasCodeBlock::class.java)
                ?: return false
            val endIndex = element.startOffset

            while (parentBlock.startOffset in startIndex until endIndex) {
                val eqStatement = when (parentBlock) {
                    is CaosScriptDoifStatementStatement -> {
                        parentBlock.equalityExpression
                    }

                    is CaosScriptElseIfStatement -> {
                        parentBlock.equalityExpression
                    }

                    else -> null
                }
                val eqStatements = if (eqStatement != null) {
                    listOfNotNull(
                        eqStatement.equalityExpressionPrime,
                        *eqStatement.equalityExpressionPlusList.mapNotNull {
                            it.equalityExpressionPrime
                        }.toTypedArray()
                    )
                } else {
                    emptyList()
                }

                if (eqStatements.isNotEmpty()) {
                    for (eq in eqStatements) {
                        if (eq.first.commandTokenElement?.toTokenOrNull() == REAQ_TOKEN) {
                            val argText = (eq.first.arguments.getOrNull(0) as? CaosScriptRvalue)?.stringValue
                            if (argText != tag) {
                                continue
                            }
                        } else if (eq.second?.commandTokenElement?.toTokenOrNull() == REAQ_TOKEN) {
                            val argText = (eq.second?.arguments?.getOrNull(0) as? CaosScriptRvalue)?.stringValue
                            if (argText != tag) {
                                continue
                            }
                        } else {
                            continue
                        }

                        val eqOp = eq.eqOp?.let { EqOp.fromValue(it.text) }
                            ?: continue

                        val intValue = eq.first.intValue
                            ?: eq.second?.intValue
                            ?: return true

                        return when (eqOp) {
                            EqOp.EQUAL -> intValue == 1
                            EqOp.GREATER_THAN -> intValue == 0
                            EqOp.NOT_EQUAL -> intValue == 0
                            else -> true
                        }
                    }
                }

                parentBlock = parentBlock.getParentOfType(CaosScriptHasCodeBlock::class.java)
                    ?: return false
            }
            return false
        }

        fun token(psiElement: PsiElement): Int? {
            return if (psiElement.textLength != 4) {
                null
            } else {
                token(psiElement.text)
            }
        }
    }
}