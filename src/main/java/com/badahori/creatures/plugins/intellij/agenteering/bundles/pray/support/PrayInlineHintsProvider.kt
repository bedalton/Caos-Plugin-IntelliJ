@file:Suppress("UnstableApiUsage")

package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support

import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support.PrayDependencyCategories.dependencyCategoryName
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayLanguage
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayTagTagValue
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.PrayTag
import com.badahori.creatures.plugins.intellij.agenteering.utils.endOffset
import com.badahori.creatures.plugins.intellij.agenteering.utils.isNotFolded
import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.codeInsight.hints.Option
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.PsiElement



class PrayInlayHintsProvider : InlayParameterHintsProvider {
    override fun getSupportedOptions(): List<Option> =
        HintsProvider.values.map { it.option }

    override fun getParameterHints(element: PsiElement): List<InlayInfo> {
        // Ensure we should still be providing hints
        ProgressIndicatorProvider.checkCanceled()

        // Resolve provider based on element
        val resolved = HintsProvider.resolve(element)
            ?: return mutableListOf()
        return resolved.provideHints(element)
    }

    override fun isBlackListSupported(): Boolean = false

    override fun getInlayPresentation(inlayText: String): String = inlayText

    override fun getDefaultBlackList(): Set<String> {
        return mutableSetOf()
    }

    override fun getHintInfo(element: PsiElement): HintInfo? {
        return HintsProvider.resolve(element)?.getHintInfo(element)
    }

}


private interface HintsProvider {

    fun isApplicable(element: PsiElement): Boolean
    fun provideHints(element: PsiElement): List<InlayInfo>
    fun getHintInfo(element: PsiElement): HintInfo?
    val enabled: Boolean
    val option: Option
    val priority: Int

    companion object {
        // When resolving hint providers, ensure element is not folded first
        fun resolve(element: PsiElement): HintsProvider? = if (element.isNotFolded)
            values.sortedByDescending { it.priority }
                .find { it.enabled && it.isApplicable(element) }
        else
            null

        val values: Array<HintsProvider> =
            arrayOf(*InlayHints.values())
    }
}

private enum class InlayHints(
    description: String,
    override val enabled: Boolean,
    override val priority: Int = 0
) : HintsProvider {
    /**
     * Provides hint for return values of expression, mostly for comparisons
     *
     */
    SHOW_DEPENDENCY_CATEGORY_NAME("Show dependency category name", true, 100) {

        private val categoryInvalid = "INVALID!"

        override fun isApplicable(element: PsiElement): Boolean {
            return (element as? PrayTagTagValue)?.isNumberValue == true
        }

        /**
         * Ensure that element is tag value for a "Dependency Category #" tag
         */
        private fun isDependencyCategory(element: PsiElement): Boolean {
            // Ensure this is a pray tag value
            if (element !is PrayTagTagValue)
                return false

            // Get parent pray tag so we can get their tag name
            val parent = element.parent as PrayTag

            // Ensure that tag name is indeed for a dependency category
            return PrayTags.DEPENDENCY_CATEGORY_TAG_FUZZY.matches(parent.tagName)
        }

        override fun provideHints(element: PsiElement): List<InlayInfo> {

            // Ensure is a tag value element, and that its corresponding tag name is Dependency Category
            if (!isDependencyCategory(element))
                return emptyList()

            // Ensure that this value is an integer
            val category = element.text.toIntOrNull()
                ?: return emptyList()

            val directoryName = dependencyCategoryName(category, true)
                ?: categoryInvalid
            return listOf(InlayInfo("(${directoryName})", element.endOffset))
        }

        /**
         * Gets the hint info for use in black-listing
         */
        override fun getHintInfo(element: PsiElement): HintInfo? {
            if ((element as? PrayTagTagValue)?.isNumberValue != true) {
                return null
            }
            return HintInfo.MethodInfo("Dependency Category ${element.text}", listOf(), PrayLanguage)
        }

    };

    override val option: Option = Option("SHOW_${this.name}", description, enabled)
}