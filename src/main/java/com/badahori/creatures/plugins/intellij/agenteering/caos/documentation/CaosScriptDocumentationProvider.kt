package com.badahori.creatures.plugins.intellij.agenteering.caos.documentation

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommand
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCommandDefElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCommandElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptIsCommandToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVarToken
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager

class CaosScriptDocumentationProvider : AbstractDocumentationProvider() {

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        return getDescriptionInfo(element, originalElement)?.let {(fullCommand, comment) ->
            return "<b>$fullCommand</b>${comment.nullIfEmpty()?.let {"\n$it"} ?: ""}"
        }
    }

    private fun getDescriptionInfo(element: PsiElement?, originalElement: PsiElement?) : Pair<String, String?>? {
        return when {
            element is CaosScriptVarToken -> getDescriptiveText(element)
            originalElement is CaosScriptVarToken -> getDescriptiveText(originalElement)
            element is CaosScriptIsCommandToken -> getDescriptiveText(element)
            originalElement is CaosScriptIsCommandToken -> getDescriptiveText(originalElement)
            else -> null
        }
    }

    private fun getDescriptiveText(element: CaosScriptIsCommandToken): Pair<String, String?> {
        val fullCommand = CaosScriptPresentationUtil.getDescriptiveText(element)
        val comment = when (val parent = element.parent) {
            is CaosScriptCommandElement -> parent.commandDefinition?.description
            is CaosDefCommand -> (parent.parent as? CaosDefCommandDefElement)?.comment
            else -> null
        }.nullIfEmpty() ?: return Pair(fullCommand, null)
        return Pair(fullCommand, comment)
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
        return getDescriptionInfo(element, originalElement)?.let {(fullCommand, commentIn) ->
            val commentFormatted = commentIn?.let { "\n" + DocumentationMarkup.CONTENT_START + it + DocumentationMarkup.CONTENT_END } ?: ""
            return DocumentationMarkup.DEFINITION_START + fullCommand + DocumentationMarkup.DEFINITION_END + commentFormatted
        }
    }

    private fun getDescriptiveText(element: CaosScriptVarToken): Pair<String,String?> {
        val group = element.varGroup
        val command = CaosLibs.commands(group.value)
                .firstOrNull()
                ?: return Pair("${element.varGroup} variable", null)
        return Pair(command.fullCommandHeader, command.description)
    }

    override fun getDocumentationElementForLink(psiManager: PsiManager?, linkIn: String?, context: PsiElement?): PsiElement? {
        val project = psiManager?.project
                ?: context?.project
                ?: return null
        val variant = (context?.containingFile as? CaosScriptFile)?.variant
                ?: return null
        val link = linkIn?.replace("psi_element://", "")?.trim('[', ']')
                ?: return null
        return CaosDefCommandElementsByNameIndex.Instance[link, project]
                .filter {
                    it.isVariant(variant)
                }
                .minBy {
                    when {
                        it.isCommand -> 0
                        it.isRvalue -> 1
                        else -> 2
                    }
                }
    }
}