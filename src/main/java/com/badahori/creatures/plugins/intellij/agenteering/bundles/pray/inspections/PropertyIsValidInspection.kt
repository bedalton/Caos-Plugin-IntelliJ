package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.inspections

import bedalton.creatures.util.FileNameUtil
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Path
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Pray
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support.PrayTags
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.DeleteElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.AgentMessages
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Pray
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor

class Caos2PrayPropertyIsValidInspection : LocalInspectionTool() {

    override fun getGroupDisplayName(): String = CAOS2Pray
    override fun getGroupPath(): Array<String> = CAOS2Path

    /**
     * Builds visitor for visiting and validating PsiElements related to this inspection
     */
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitCaos2TagName(element: CaosScriptCaos2TagName) {
                super.visitCaos2TagName(element)
                validateTagName(element, holder)
            }
        }
    }

    companion object {

        private const val FORCE_GENETICS_WILDCARD = false

        private val REMOVAL_SCRIPT_REGEX =
            "Removal\\s*Script|Remove[r]?\\s*Script|RSCR".toRegex(RegexOption.IGNORE_CASE)

        /**
         * Validates a COB comment directive, to ensure that it actually exists
         */
        private fun validateTagName(element: CaosScriptCaos2TagName, holder: ProblemsHolder) {
            if (!element.containingCaosFile?.isCaos2Pray.orFalse())
                return
            val tagNameRaw = element.text?.nullIfEmpty()
                ?: return

            val tagName = element.text?.replace(WHITESPACE_OR_DASH, " ")?.trim()?.nullIfEmpty()?.let { tag ->
                PrayTags.normalize(tag) ?: tag
            }

            if (tagName == null) {
                annotateEmptyTag(element, holder)
                return
            }

            if (tagName in listOf("Genetics File", "Father Genetic File", "Mother Genetic File")) {
                annotateGeneticsTag(element, tagName, holder)
                return
            }

            if (PrayTags.isOfficialTag(tagName)) {
                return
            }

            if (tagName.matches(REMOVAL_SCRIPT_REGEX)) {
                val parentTagElement = element.getParentOfType(CaosScriptCaos2Tag::class.java)
                val fix = parentTagElement?.let { tag ->
                    CaosScriptReplaceElementFix(
                        tag,
                        "Rscr \"${tag.valueAsString}\"",
                        AgentMessages.message("caos2properties.property-is-valid.replace-with-message",
                            tagNameRaw,
                            "Rscr"),
                        true
                    )
                }
                val error = AgentMessages.message("pray.inspections.tags.similar-tags.is-similar-message",
                    "command",
                    tagNameRaw)
                holder.registerProblem(element, error, fix)
            }

        }

        private fun annotateGeneticsTag(element: PsiElement, tagName: String, holder: ProblemsHolder) {
            val value = element.getParentOfType(CaosScriptCaos2Tag::class.java)
            val filePath = value?.valueAsString?.nullIfEmpty()
            if (filePath == null) {
                holder.registerProblem(
                    element,
                    AgentMessages.message(
                        "errors.caos2properties.property-invalid.missing-value",
                        tagName,
                        "genetics file value"
                    )
                )
                return
            }
            val blockCommands: List<Pair<String, MutableList<String>>> =
                element.getParentOfType(CaosScriptCaos2Block::class.java)
                    ?.commands
                    .orEmpty()
            var geneticFilePath = FileNameUtil.getLastPathComponent(filePath)
                .nullIfEmpty()
            if (geneticFilePath == null) {
                holder.registerProblem(
                    element,
                    AgentMessages.message(
                        "errors.caos2properties.property-invalid.missing-value",
                        tagName,
                        "genetics file value"
                    )
                )
                return
            }

            val geneticsFileRegex = geneticFilePath
                .replace("[*]+".toRegex(), "[^/]*")
                .let {
                    "$it(\\.gen)?"
                }
                .toRegex(RegexOption.IGNORE_CASE)


            val hasGeneticsFileMatch = hasGeneticFileMatch(blockCommands, geneticsFileRegex)
            if (hasGeneticsFileMatch) {
                return
            }
            val fixes = mutableListOf<LocalQuickFix>()

            // TODO: Check and fix case
//            val caseFix = getGeneticsFileCaseFix()


            if (!FORCE_GENETICS_WILDCARD && !geneticFilePath.endsWith('*')) {
                val newGeneticsFileRegex = "$geneticsFileRegex[^/]*".toRegex()
                if (hasGeneticFileMatch(blockCommands, geneticsFileRegex)) {
                    fixes.add(
                        CaosScriptReplaceElementFix(
                            element,
                            "$tagName = \"${filePath}*\""
                        )
                    )
                }
            }
            holder.registerProblem(
                value,
                AgentMessages.message(
                    "errors.caos2properties.property-invalid.no-genetics-files-matching",
                    filePath
                ),
                *fixes.toTypedArray()
            )
        }

        private fun hasGeneticFileMatch(
            blockCommands: List<Pair<String, MutableList<String>>>,
            fileExpression: Regex,
        ): Boolean {
            return blockCommands
                .any { (command, files) ->
                    // Get wildcard file names
                    val matches: List<String> = when (command.lowercase()) {
                        "link" -> emptyList()
                        "inline" -> {
                            val matches = fileExpression.matches(files.firstOrNull() ?: "")
                            if (matches) {
                                listOfNotNull(files.firstOrNull())
                            } else {
                                emptyList()
                            }
                        }
                        else -> files.filter(fileExpression::matches)
                    }
                    // Check that matches contains genetics file
                    matches.any { it.lowercase().endsWith(".gen") }
                }
        }

//        private fun getGeneticsFileCaseFix(
//            element: PsiElement,
//            blockCommands: List<Pair<String, MutableList<String>>>,
//            geneticFilePath: String
//        ) {
//            val splits = geneticFilePath
//                .split('*')
//            var counts = 0
//            for (part in splits) {
//
//            }
//            var geneticsFileRegexIgnoreCase = geneticFilePath
//                .replace("[*]+".toRegex(), "[^/]+")
//                .toRegex(RegexOption.IGNORE_CASE)
//        }

        private fun getFixesForSimilar(
            element: PsiElement,
            tagName: String,
            orb: Int,
        ): List<CaosScriptReplaceElementFix> {
            return PrayTags.allTags
                .map { aTag ->
                    Pair(aTag, aTag.levenshteinDistance(tagName))
                }.filter {
                    it.second < orb
                }.map {
                    CaosScriptReplaceElementFix(
                        element,
                        it.first,
                        AgentMessages.message("cob.caos2cob.fix.replace-cob-tag", it.first)
                    )
                }
        }

    }
}


private fun annotateEmptyTag(element: PsiElement, holder: ProblemsHolder) {
    val error = AgentMessages.message("errors.tags.tag-cannot-be-blank", "PRAY")
    val parent = element.getParentOfType(CaosScriptCaos2BlockComment::class.java)
    var fix: LocalQuickFix? = null
    if (parent != null) {
        val hasValue = parent.caos2TagList.any { it.caos2Value != null }
        fix = if (!hasValue) {
            DeleteElementFix(
                AgentMessages.message("pray.tags.fixes.delete-empty-tag"),
                parent
            )
        } else {
            DeleteElementFix(
                AgentMessages.message("pray.tags.fixes.delete-empty-tag-with-value"),
                parent
            )
        }
    }
    if (fix != null) {
        holder.registerProblem(element, error, fix)
    } else {
        holder.registerProblem(element, error)
    }
}