package com.badahori.creatures.plugins.intellij.agenteering.caos.documentation

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptIsCommandToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVarToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getSelfOrParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.nullIfEmpty

class CaosScriptDocumentationProvider : AbstractDocumentationProvider() {
    override fun getDocumentationElementForLookupItem(psiManager: PsiManager, `object`: Any, element: PsiElement): PsiElement? {
        return null
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        return when (element) {
            is CaosScriptIsCommandToken -> getDescriptiveText(element)
            is CaosScriptVarToken -> getDescriptiveText(element)
            else -> null
        }
    }

    private fun getDescriptiveText(element: CaosScriptIsCommandToken): String {
        val declarationElement = element
                .reference
                .multiResolve(true)
                .firstOrNull()
                ?.element
                ?.getSelfOrParentOfType(com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement::class.java)
                ?: return CaosScriptPresentationUtil.getDescriptiveText(element)
        val fullCommand = declarationElement.fullCommand
        declarationElement.comment.nullIfEmpty()?.let {
            return "<b>$fullCommand</b>\n$it"
        }
        return fullCommand
    }

    /**
     * Callback for asking the doc provider for the complete documentation.
     *
     *
     * Underlying implementation may be time-consuming, that's why this method is expected not to be called from EDT.
     *
     *
     * One can use [DocumentationMarkup] to get proper content layout. Typical sample will look like this:
     * <pre>
     * DEFINITION_START + definition + DEFINITION_END +
     * CONTENT_START + main description + CONTENT_END +
     * SECTIONS_START +
     * SECTION_HEADER_START + section name +
     * SECTION_SEPARATOR + "
     *
     *" + section content + SECTION_END +
     * ... +
     * SECTIONS_END
    </pre> *
     *
     * @param element         the element for which the documentation is requested (for example, if the mouse is over
     * a method reference, this will be the method to which the reference is resolved).
     * @param originalElement the element under the mouse cursor
     * @return                target element's documentation, or `null` if provider is unable to generate documentation
     * for the given element
     */
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (originalElement !is CaosScriptVarToken && originalElement !is CaosScriptIsCommandToken)
            return null
        val baseElement = element
                ?: if (originalElement is CaosScriptIsCommandToken)
                    originalElement.reference.multiResolve(true).firstOrNull()?.element
                else if (originalElement is CaosScriptVarToken)
                    originalElement.reference.multiResolve(true).firstOrNull()?.element
                else
                    return null
        val command = baseElement?.getSelfOrParentOfType(com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement::class.java)
                ?: return when (originalElement) {
                    is CaosScriptIsCommandToken -> getDescriptiveText(originalElement)
                    is CaosScriptVarToken -> getDescriptiveText(originalElement)
                    else -> null
                }
        val fullCommand = command.fullCommand
        command.comment?.nullIfEmpty()?.let {
            return "DEFINITION_START $fullCommand DEFINITION_END\nCONTENT_START $it CONTENT_END"
        }
        return fullCommand
    }

    private fun getDescriptiveText(element: CaosScriptVarToken): String {
        return element
                .reference.multiResolve(true)
                .firstOrNull()
                ?.element
                ?.getSelfOrParentOfType(com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement::class.java)
                ?.comment
                ?: return "${element.varGroup} variable"
    }
}