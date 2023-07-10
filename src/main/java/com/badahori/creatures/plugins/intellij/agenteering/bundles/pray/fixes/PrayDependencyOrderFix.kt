package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.fixes

import com.bedalton.creatures.agents.pray.compiler.pray.bestDependencyCategoryForFile
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.PRAY
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayFileType
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayAgentBlock
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.stubs.PrayTagStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.AgentMessages
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class PrayDependencyOrderFix: LocalQuickFix, IntentionAction {
    override fun startInWriteAction(): Boolean {
        return true
    }

    override fun getText(): String {
        return AgentMessages.message("pray.tags.fixes.reflow-dependencies")
    }

    override fun getFamilyName(): String {
        return PRAY
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return file != null
                && file.fileType == PrayFileType
                && editor?.element?.getParentOfType(PrayAgentBlock::class.java) != null
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val block = editor?.element?.getParentOfType(PrayAgentBlock::class.java)
        if (block == null) {
            CaosNotifications.showError(
                project,
                text,
                "Dependency re-ordering failed. Could not ascertain parent agent block"
            )
            return
        }
        applyFix(project, block)
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val block = descriptor.psiElement?.getParentOfType(PrayAgentBlock::class.java)
        if (block == null) {
            CaosNotifications.showError(
                project,
                text,
                "Dependency re-ordering failed. Could not ascertain parent agent block"
            )
            return
        }
        applyFix(project, block)
    }


    private fun applyFix(project: Project, block: PrayAgentBlock) {
        val document = block.containingFile.document ?: block.containingFile?.originalFile?.document
        if (document == null) {
            CaosNotifications.showError(
                project,
                text,
                "Failed to locate document in memory for editing"
            )
            return
        }
        val out = StringBuilder(block.blockHeader.text)
        val tags = block.tagStructs
        val dependencies: List<PrayTagStruct.StringTag> = tags
            .filterIsInstance<PrayTagStruct.StringTag>()
            .filter { dependencyTagRegex.matches(it.tag) }
        val inlineDependencies: List<PrayTagStruct.InliningTag> = tags
            .filterIsInstance<PrayTagStruct.InliningTag>()
        if (inlineDependencies.isNotEmpty()) {
            CaosNotifications.showWarning(
                project,
                text,
                "You cannot pass file text as dependency. Line(s) ${inlineDependencies.joinToString { "${it.indexInFile}" }}")
            return
        }

        val dependencyCategoriesRaw: List<PrayTagStruct.IntTag> = tags
            .filterIsInstance<PrayTagStruct.IntTag>()

        val dependencyCategories = dependencyCategoriesRaw
            .filter { dependencyCategoryRegex.matches(it.tag) }
            .associateBy { dependencyCategoryRegex.matchEntire(it.tag)!!.groupValues[1].toInt() }

        val otherIntTags = tags.filter { it is PrayTagStruct.IntTag && !dependencyCategoryRegex.matches(it.tag) && !dependencyCountRegex.matches(it.tag)}
        val otherStringTags = tags.filter { (it is PrayTagStruct.StringTag || it is PrayTagStruct.InliningTag) && !dependencyTagRegex.matches(it.tag) }

        val reCategorize = dependencyCategories.size != dependencyCategoriesRaw.size
        if (reCategorize) {
            CaosNotifications.showWarning(
                project,
                text,
                "Duplicate dependency categories exist. Categories will be recalculated"
            )
        }
        for (tag in otherIntTags) {
            out.append("\n\"").append(tag.tag).append("\" ").append(tag.value)
        }
        for (tag in otherStringTags) {
            out.append("\n\"").append(tag.tag).append("\" \"").append(tag.value).append('"')
        }
        var index = 0
        var didError = false
        out.append("\n\"Dependency Count\" ").append(dependencies.size)
        for (tag in dependencies) {
            index++
            val oldIndex = dependencyTagRegex.matchEntire(tag.tag)?.groupValues?.get(1)?.toIntOrNull()
            val existingCategory = dependencyCategories[oldIndex]
            val category = if (reCategorize || existingCategory == null) {
                bestDependencyCategoryForFile(tag.value)
            } else {
                existingCategory.value
            }
            if (category == null) {
                didError = true
                CaosNotifications.showError(
                    project,
                    text,
                    "Failed to guess dependency category for file ${tag.value} on line #${tag.indexInFile}"
                )
                continue
            }
            if (didError) {
                continue
            }
            out.append("\n\"Dependency ").append(index).append("\" \"").append(tag.value).append('"')
            out.append("\n\"Dependency Category ").append(index).append("\" ").append(category)
        }

        if (didError) {
            CaosNotifications.showError(
                project,
                text,
                "Dependency re-ordering failed"
            )
            return
        }
        if (project.isDisposed) {
            return
        }
        EditorUtil.replaceText(document, block.textRange, out.append("\n").toString())
    }


    companion object {
        val dependencyTagRegex = "dependency\\s+(\\d+)".toRegex(RegexOption.IGNORE_CASE)
        val dependencyCategoryRegex = "dependency\\s+category\\s+(\\d+)".toRegex(RegexOption.IGNORE_CASE)
        val dependencyCountRegex = "dependency\\s+count".toRegex(RegexOption.IGNORE_CASE)
    }
}