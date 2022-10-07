package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.inspections

import bedalton.creatures.agents.pray.compiler.pray.bestDependencyCategoryForFile
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Path
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.PRAY
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayAgentBlock
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayPrayTag
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayVisitor
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support.PrayDependencyCategories
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support.PrayTags
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.AgentMessages
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import bedalton.creatures.util.nullIfEmpty
import com.intellij.openapi.project.DumbAware

class PrayDependencyCategoryMatchesFileType: LocalInspectionTool(), DumbAware {

    override fun getGroupDisplayName(): String = PRAY
    override fun getGroupPath(): Array<String> = CAOS2Path
    override fun getShortName(): String = "PRAYDependencyCategoryMatchesFileType"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PrayVisitor() {
            override fun visitPrayTag(o: PrayPrayTag) {
                super.visitPrayTag(o)
                validateTag(o, holder)
            }
        }
    }

    private fun validateTag(element: PrayPrayTag, problemsHolder: ProblemsHolder) {
        val tag = element.tagTagName

        val tagName = tag.stringValue
            ?.trim()
            ?.nullIfEmpty()
            ?: return

        val dependencyIndex = PrayTags.DEPENDENCY_CATEGORY_TAG_FUZZY
            .matchEntire(tagName)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.toIntOrNull()
            ?: return

        val actualCategory = element.valueAsInt
            ?: return

        val dependencyKey = "Dependency\\s+$dependencyIndex".toRegex(RegexOption.IGNORE_CASE)
        val dependency = element.getParentOfType(PrayAgentBlock::class.java)
            ?.tagStructs
            ?.firstOrNull {
                dependencyKey.matches(it.tag)
            }
            ?: return

        val filename = (dependency.value as? String)
            ?.trim()
            ?.nullIfEmpty()
            ?: return

        val bestCategory = bestDependencyCategoryForFile(filename)
            ?: return
        if (bestCategory == actualCategory)
            return
        // Image can be 2 (images) or 5 (Overlays)
        if (bestCategory == 2 && actualCategory == 5)
            return
        val bestCategoryName = PrayDependencyCategories.dependencyCategoryName(bestCategory, false)
            ?: "INVALID!"
        val error = AgentMessages.message(
            "fixes.dependency-category.not-ideal-category",
            bestCategory,
            PrayDependencyCategories.dependencyCategoryName(bestCategory, false) ?: "INVALID",
            actualCategory,
            PrayDependencyCategories.dependencyCategoryName(actualCategory, true) ?: "INVALID"
        )
        val fix = CaosScriptReplaceElementFix(
            element.tagTagValue,
            "$bestCategory",
            AgentMessages.message(
                "fixes.dependency-category.not-ideal-category.replace-with",
                bestCategory,
                bestCategoryName
            )
        )
        problemsHolder.registerProblem(
            element.tagTagValue,
            error,
            fix
        )
    }
}

private class AssociateFileTypeWithCategory(
    private val fileName: String,
    private val categoryId: Int
): LocalQuickFix {
    override fun getFamilyName(): String = PRAY

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {

    }


}