package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.intellij.application.options.CodeStyle
import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.formatting.Wrap
import com.intellij.formatting.WrapType
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.psi.formatter.DocumentBasedFormattingModel


class CaosScriptFormattingModelBuilder : FormattingModelBuilder {

    override fun createModel(element: PsiElement, settings: CodeStyleSettings): FormattingModel {
        // element can be DartFile, DartEmbeddedContent, DartExpressionCodeFragment
        val rootNode: ASTNode = element.node
        val file = element.containingFile ?: element.originalElement?.containingFile
        val caosSettings = if (file != null) {
            CodeStyle.getCustomSettings(file, CaosScriptCodeStyleSettings::class.java)
        } else {
            null
        }
        val rootBlock = CaosScriptBlock(
            node = rootNode,
            wrap = Wrap.createWrap(WrapType.NONE, false),
            alignment = null,
            settings = settings,
            caosSettings = caosSettings
        )
        val psiFile = element.containingFile ?: element.originalElement?.containingFile!!
        return DocumentBasedFormattingModel(rootBlock, element.project, settings, psiFile.fileType, psiFile)
    }

    override fun getRangeAffectingIndent(file: PsiFile?, offset: Int, elementAtOffset: ASTNode?): TextRange? {
        return null
    }
}