package com.badahori.creatures.plugins.intellij.agenteering.caos.hints

import com.bedalton.common.util.isNotNullOrBlank
import com.bedalton.common.util.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.ClassifierToAgentNameHelper
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptClassifier
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventScript
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.indices.CatalogueEntryElementIndex
import com.badahori.creatures.plugins.intellij.agenteering.utils.now
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

class CaosClassifierFolder: FoldingBuilderEx() {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {

        // Ensure that indices is loaded
        if (!shouldRun(root)) {
            return EMPTY
        }

        val group = FoldingGroup.newGroup("CaosScript_CLASSIFIER_FOLD")
        val classifiers = PsiTreeUtil.collectElementsOfType(root, CaosScriptClassifier::class.java)
        val regions = classifiers.mapNotNull {classifier ->
            ProgressIndicatorProvider.checkCanceled()
            getAndCache(classifier)?.let {
                Pair(classifier.node, it)
            }
        }
        return regions.map { (node, data) ->
            ProgressIndicatorProvider.checkCanceled()
            FoldingDescriptor(node, data.first, group, data.second)
        }.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String? {
        return getAndCache(node.psi)?.second
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        return true
    }


    /**
     * Finding agent name, and store it (and its text range) in userData for fast access later
     */
    private fun getAndCache(element: PsiElement): Pair<TextRange, String>? {
        val now = now
        element.getUserData(KEY)?.let {
            if (it.first < now) {
                return it.second
            }
        }
        val folded = getFoldedEx(element)
        element.putUserData(KEY, Pair(now + CACHE_FOR_IN_SECONDS, folded))
        return folded
    }


    /**
     * Gets the agent name and its text range
     */
    private fun getFoldedEx(element: PsiElement): Pair<TextRange, String>? {
        if (!shouldRun(element)) {
            return null
        }
        if (element !is CaosScriptClassifier) {
            return null
        }
        if (element.parent !is CaosScriptEventScript) {
            return null
        }

        // Get family ensuring it is an int
        val family = element.family
            .rvalue
            .intValue
            ?: return null

        // Get genus ensuring it is an int
        val genus = element.genus
            ?.rvalue
            ?.intValue
            ?: return null


        // Get species ensuring it is an int
        val species = element.species
            ?.rvalue
            ?.intValue
            ?: return null


        // Format the expected catalogue TAG
        val text = "Agent Help $family $genus $species"

        // Find agent name matches in the index
        var matches = CatalogueEntryElementIndex.Instance[text, element.project]
            .mapNotNull { it.itemsAsStrings.getOrNull(0) }
            .filter { it.isNotNullOrBlank()  && it.lowercase() !in ENUM_CALLS }
            .distinct()

        // If there are no matches, search in comments and stuff
        if (matches.isEmpty()) {
            // Get matches from the agent name index built from both CAOS comments,
            // and catalogue tags (including those with bad case or bad spacing)
            matches = ClassifierToAgentNameHelper.getAgentNames(element.project, family, genus, species)
                .distinct()
                .nullIfEmpty()
                ?: return null

        }

        return if (matches.size == 1) {
            Pair(element.textRange, matches[0])
        } else {
            Pair(element.textRange, matches.joinToString(" or "))
        }
    }

}

private const val CACHE_FOR_IN_SECONDS = 20_000
private val KEY  = Key<Pair<Long, Pair<TextRange, String>?>?>("bedalton.creatures.ClassifierFolder.RANGE_AND_TEXT")
private val ENUM_CALLS = listOf(
    "enum",
    "escn",
    "etch",
    "esee",
    "epas",
    "econ"
)
fun shouldRun(element: PsiElement?): Boolean {
    return shouldRun(element?.project, element)
}

@Suppress("MemberVisibilityCanBePrivate")
fun shouldRun(project: Project?, element: PsiElement?): Boolean {
    return project?.isDisposed == false && element?.isValid == true && !DumbService.isDumb(project)
}

private val EMPTY by lazy {
    emptyArray<FoldingDescriptor>()
}