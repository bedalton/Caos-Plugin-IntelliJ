package com.badahori.creatures.plugins.intellij.agenteering.caos.highlighting

import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support.PrayTags
import com.badahori.creatures.plugins.intellij.agenteering.caos.annotators.colorize
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

class CaosScriptHighlighterAnnotator : Annotator, DumbAware {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when {
            element is CaosScriptIsRvalueKeywordToken && element.firstChild?.tokenType != CaosScriptTypes.CaosScript_TOKEN -> colorize(
                element,
                holder,
                CaosScriptSyntaxHighlighter.RVALUE_TOKEN
            )
            element is CaosScriptIsLvalueKeywordToken -> {
                if (element is CaosScriptVarToken)
                    return
                colorize(element, holder, CaosScriptSyntaxHighlighter.LVALUE_TOKEN)
            }
            element is CaosScriptIsCommandKeywordToken -> colorizeCommand(element, holder)
            element is CaosScriptAnimationString -> annotateByteString(element, holder)
            element is CaosScriptByteString -> annotateByteString(element, holder)
            element is CaosScriptSubroutineName || element.parent is CaosScriptSubroutineName -> colorize(
                element,
                holder,
                CaosScriptSyntaxHighlighter.SUBROUTINE_NAME
            )
            element is CaosScriptTokenRvalue -> colorize(element, holder, CaosScriptSyntaxHighlighter.TOKEN)

            // Annotate/colorize '@directive' or '**Directives'
            element is CaosScriptAtDirectiveComment -> colorize(element, holder, CaosScriptSyntaxHighlighter.COMMENT)

//             Colorizes all fallthrough word tokens
            element.tokenType == CaosScriptTypes.CaosScript_WORD -> when {
                element.hasParentOfType(CaosScriptErrorCommand::class.java) -> colorize(
                    element,
                    holder,
                    CaosScriptSyntaxHighlighter.ERROR_COMMAND_TOKEN
                )
                element.parent is CaosScriptCGsub -> colorize(
                    element,
                    holder,
                    CaosScriptSyntaxHighlighter.SUBROUTINE_NAME
                )
                else -> colorize(element, holder, CaosScriptSyntaxHighlighter.TOKEN)
            }
            element is CaosScriptCaos2Value -> if (element.quoteStringLiteral != null && element.c1String != null) {
                colorize(element, holder, CaosScriptSyntaxHighlighter.STRING)
            }
            element is CaosScriptCaos2TagName -> highlightCaos2PrayTag(element, holder)
        }
    }

    private fun colorizeCommand(element: PsiElement, holder: AnnotationHolder) {
        val commandToken = (element as? CaosScriptIsCommandToken)
            ?: ((element as? CaosScriptCommandLike)?.commandTokenElement)
            ?: return

        val tokens = commandToken.commandTokenElements
        if (tokens.size == 1) {
            if (tokens[0].elementType in CaosScriptTokenSets.KEYWORDS) {
                holder.colorize(tokens[0], CaosScriptSyntaxHighlighter.KEYWORDS)
                return
            }
            holder.colorize(tokens[0], CaosScriptSyntaxHighlighter.COMMAND_TOKEN)
            return
        }
        holder.colorize(tokens[0], CaosScriptSyntaxHighlighter.PREFIX_TOKEN)
        holder.colorize(tokens[1], CaosScriptSyntaxHighlighter.COMMAND_TOKEN)
        if (tokens.size > 2) {
            holder.colorize(tokens[2], CaosScriptSyntaxHighlighter.SUFFIX_TOKEN)
        }
    }

    /**
     * Helper function to add color and style to a given element
     */
    private fun colorize(
        psiElement: PsiElement,
        annotationHolder: AnnotationHolder,
        attribute: TextAttributesKey,
        recursive: Boolean = true
    ) {
        annotationHolder.colorize(psiElement, attribute, recursive)
    }

    private fun isAnimationByteString(element: PsiElement): Boolean {
        val argumentParent = element.getParentOfType(CaosScriptArgument::class.java)
            ?: return false
        val index = argumentParent.index
        val commandParent = element.getParentOfType(CaosScriptCommandElement::class.java)
            ?: return false
        return commandParent.commandDefinition?.let { command ->
            val parameter = command.parameters.getOrNull(index)
            parameter?.type == CaosExpressionValueType.ANIMATION
        } ?: false

    }

    private fun annotateByteString(element: PsiElement, holder: AnnotationHolder) {
        val attribute = if (element.parent?.parent is CaosScriptEqualityExpressionPrime) {
            CaosScriptSyntaxHighlighter.STRING
        } else if (element.variant?.isOld == true && element.text.contains(WHITESPACE)) {
            CaosScriptSyntaxHighlighter.STRING
        } else if (element is CaosScriptAnimationString || isAnimationByteString(element)) {
            CaosScriptSyntaxHighlighter.ANIMATION
        } else {
            CaosScriptSyntaxHighlighter.BYTE_STRING
        }
        colorize(
            element,
            holder,
            attribute,
            true
        )
    }

    private fun highlightCaos2PrayTag(element: CaosScriptCaos2TagName, holder: AnnotationHolder) {
        if (element.variant?.isOld == true)
            return
        val tag = element.quoteStringLiteral?.stringValue ?: element.c1String?.stringValue ?: element.text
        if (PrayTags.isOfficialTag(tag)) {
            holder.colorize(element, CaosScriptSyntaxHighlighter.OFFICIAL_PRAY_TAG)
        } else {
            holder.colorize(element, CaosScriptSyntaxHighlighter.PRAY_TAG)
        }
    }

}