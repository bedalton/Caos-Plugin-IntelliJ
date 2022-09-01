@file:Suppress("UnstableApiUsage")

package com.badahori.creatures.plugins.intellij.agenteering.common

import com.badahori.creatures.plugins.intellij.agenteering.utils.isNotFolded
import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.codeInsight.hints.Option
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.PsiElement

/**
 * Provider for generating inlay hints
 */
abstract class AbstractInlayHintsProvider : InlayParameterHintsProvider {

    /**
     * List of all possible generators for this subject or language
     */
    protected abstract val inlayHintGenerators: List<InlayHintGenerator>

    private val mInlayHintGenerators by lazy {
        inlayHintGenerators.sortedByDescending { it.priority }
    }

    /**
     * Gets the list of options for these inlay hints
     * These are used to activate/deactivate specific generators
     */
    override fun getSupportedOptions(): List<Option> = inlayHintGenerators.map { it.option }

    /**
     * Generate the inlay parameter hints
     */
    override fun getParameterHints(element: PsiElement): List<InlayInfo> {
        val resolved = resolve(element)
            ?: return mutableListOf()
        return resolved.provideHints(element)
    }

    /**
     * Determine if there is a blacklist supported for this provider
     * The blacklist prevents certain inlay hints from being generated
     */
    override fun isBlackListSupported(): Boolean = true

    /**
     * The inlay hint itself with any post-processing needed
     */
    override fun getInlayPresentation(inlayText: String): String = inlayText

    /**
     * Get the starting list of blacklist hint items
     * Used to prevent certain generated messages
     */
    override fun getDefaultBlackList(): Set<String> {
        return mutableSetOf()
    }

    /**
     * Gets information needed to generate a blacklist of items
     */
    override fun getHintInfo(element: PsiElement): HintInfo? {
        return resolve(element)?.getHintInfo(element)
    }

    // When resolving hint providers, ensure element is not folded first
    internal fun resolve(element: PsiElement): InlayHintGenerator? {
        ProgressIndicatorProvider.checkCanceled()
        return if (element.isNotFolded) {
            mInlayHintGenerators
                .firstOrNull { it.enabled && it.isApplicable(element) }
        } else {
            null
        }
    }

    companion object {
        // When resolving hint providers, ensure element is not folded first
        internal fun resolve(element: PsiElement, values: List<InlayHintGenerator>): InlayHintGenerator? {
            ProgressIndicatorProvider.checkCanceled()
            return if (element.isNotFolded) {
                values.firstOrNull { it.enabled && it.isApplicable(element) }
            } else {
                null
            }
        }

        internal fun hints(element: PsiElement, values: List<InlayHintGenerator>): HintInfo? {
            return resolve(element, values)?.getHintInfo(element)
        }
    }

}