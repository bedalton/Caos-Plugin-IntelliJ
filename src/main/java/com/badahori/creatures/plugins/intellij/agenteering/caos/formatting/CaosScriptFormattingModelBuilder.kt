package com.badahori.creatures.plugins.intellij.agenteering.caos.formatting

import com.intellij.application.options.CodeStyle
import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.formatter.DocumentBasedFormattingModel


class CaosScriptFormattingModelBuilder : FormattingModelBuilder {

    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val element = formattingContext.psiElement
        val settings = formattingContext.codeStyleSettings
        // element can be DartFile, DartEmbeddedContent, DartExpressionCodeFragment
        val rootNode: ASTNode = element.node
        val file = element.containingFile ?: element.originalElement?.containingFile
        val caosSettings = if (file != null) {
            CodeStyle.getCustomSettings(file, CaosScriptCodeStyleSettings::class.java)
        } else {
            null
        }
        val rootBlock = CaosScriptBlock(
            rootNode,
            Wrap.createWrap(WrapType.NONE, false),
            null,
            settings,
            caosSettings
        )
        val psiFile = element.containingFile ?: element.originalElement?.containingFile!!
        return DocumentBasedFormattingModel(rootBlock, element.project, settings, psiFile.fileType, psiFile)
    }

    override fun getRangeAffectingIndent(file: PsiFile?, offset: Int, elementAtOffset: ASTNode?): TextRange? {
        return null
    }
}