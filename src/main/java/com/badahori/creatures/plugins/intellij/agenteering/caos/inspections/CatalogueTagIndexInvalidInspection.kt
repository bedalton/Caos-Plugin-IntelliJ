package com.badahori.creatures.plugins.intellij.agenteering.caos.inspections

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOSScript
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
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.getParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.settings
import com.badahori.creatures.plugins.intellij.agenteering.utils.startOffset
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
class CatalogueTagIndexInvalidInspection : LocalInspectionTool() {

    override fun isEnabledByDefault(): Boolean {
        return true
    }

    override fun getDisplayName(): String {
        return "Catalogue tag index invalid"
    }

    override fun getGroupDisplayName(): String {
        return CAOSScript
    }

    override fun getGroupKey(): String {
        return CAOSScript
    }

    override fun getShortName(): String {
        return "CaosCatalogueTagIndexIsValid"
    }

    override fun getGroupPath(): Array<String> {
        return arrayOf(CAOSScript)
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {

            override fun visitRvalue(o: CaosScriptRvalue) {
                super.visitRvalue(o)
                // Ensure command is a catalogue READ command
                if (o.commandStringUpper != "READ") {
                    return
                }

                // Ensure that this element is in a C3DS environment
                if (o.variant?.isC3DS != true) {
                    return
                }

                // Get all arguments
                val argumentsRaw = o.arguments

                // Filter arguments to RValue arguments only
                val arguments = argumentsRaw.filterIsInstance<CaosScriptRvalue>()

                // This means not all arguments where rvalues
                if (argumentsRaw.size != arguments.size) {
                    return
                }
                // Try and validate the catalogue read index
                validateTagIndex(holder, arguments)
            }

        }
    }

    companion object {
        private val REAN_TOKEN = token("rean")

        private val hasTags = listOf(
            token("read")
        )

        /**
         * Validates a catalogue TAG reference to see if it exists
         */
        private fun validateTagIndex(holder: ProblemsHolder, arguments: List<CaosScriptRvalue>) {
            if (arguments.size != 2 || !arguments[1].isValid || !arguments[1].isValid) {
                return
            }
            val intElement = arguments[1]

            // Try and get index as int from second arguments
            // If argument is not an int literal, return
            val index = intElement.intValue
                ?: return

            // Get project from element
            val project = intElement.project

            // Try and get catalogue tag from arguments
            // If not a quote string, return
            val tag = arguments[0].stringValue
                ?: return

            // Check for valid
            val valid = isValid(holder, project, intElement, tag, index)

            // Tag index was valid or was marked already so return
            if (valid) {
                return
            }

            // Get the maximum value that can be used for the Catalogue
            val maxItemCount = CatalogueEntryElementIndex.Instance[tag, project].maxOfOrNull { it.itemCount }
                ?: return

            // Get error message
            val message = CaosBundle.message(
                "caos.inspections.catalogue-index-is-valid.index-out-of-bounds", index, tag, maxItemCount - 1
            )
            val quickFixes = listOfNotNull(
                IgnoreCatalogueTag(tag, true, IgnoreScope.PROJECT),
                IgnoreCatalogueTag(tag, true, IgnoreScope.APPLICATION),
                intElement.module?.let { IgnoreCatalogueTag(tag, true, IgnoreScope.MODULE) }
            )
            holder.registerProblem(
                intElement,
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
            tag: String,
            index: Int
        ): Boolean {

            var valid = false

            if (index < 0) {
                holder.registerProblem(element, CaosBundle.message("caos.inspections.catalogue-index-is-valid.less-than-zero-error"))
                return true
            }

            // Check that tag exists naturally in Catalogue files
            val maxItemCountRaw = CatalogueEntryElementIndex.Instance[tag, project].maxOfOrNull { it.itemCount }
            var exists = maxItemCountRaw != null
            val maxItemCount = maxItemCountRaw ?: 0
            val lastIndex = if (maxItemCount > 0) maxItemCount - 1 else null


            // Check if tag is in ignored tags at APPLICATION level
            // Add quick-fix to remove it from list if in ignored list
            if (tag in CaosApplicationSettingsService.getInstance().ignoredCatalogueTags) {
                exists = true
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
                exists = true
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
                exists = true
                valid = true
                holder.registerProblem(
                    element,
                    "",
                    ProblemHighlightType.INFORMATION,
                    IgnoreCatalogueTag(tag, false, IgnoreScope.PROJECT)
                )
            }

            // Finally, check if Tag is checked in a DOIF statement
            if (exists && !valid && lastIndex != null && index in 0 .. lastIndex) {
                valid = true
            }

            // Return whether the Tag exists or is checked in DOIF
            return !exists || valid || isChecked(element, tag, index)
        }


        /**
         * Checks enclosing DOIF statements to see if the tag's item count is checked
         */
        private fun isChecked(element: PsiElement, tag: String, index: Int): Boolean {
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
                        if (eq.first.commandTokenElement?.toTokenOrNull() == REAN_TOKEN) {
                            val argText = (eq.first.arguments.getOrNull(0) as? CaosScriptRvalue)?.stringValue
                            if (argText != tag) {
                                continue
                            }
                        } else if (eq.second?.commandTokenElement?.toTokenOrNull() == REAN_TOKEN) {
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
                            EqOp.EQUAL -> intValue >= index
                            EqOp.GREATER_THAN -> intValue >= (index - 1)
                            else -> false
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