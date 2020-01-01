package com.openc2e.plugins.intellij.caos.psi.util

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefFile
import com.openc2e.plugins.intellij.caos.def.lang.CaosDefLanguage
import com.openc2e.plugins.intellij.caos.def.psi.api.*

object CaosScriptPsiElementFactory {

    private fun createFileFromText(project: Project, text: String): CaosDefFile {
        return PsiFileFactory.getInstance(project).createFileFromText("dummy.caosdef", CaosDefLanguage.instance, text) as CaosDefFile
    }

    private fun createComment(project: Project, text: String) : CaosDefDocComment {
        return (createFileFromText(project,"$text\nTEMP (null);").firstChild as CaosDefCommandDefElement).docComment!!
    }

    fun createCommandTokenElement(project: Project, newNameString: String): CaosDefCommandWord {
        val file= createFileFromText(project, "$newNameString 0");
        val command =  file.firstChild as CaosDefCommandDefElement
        return command.command.commandWordList[0]
    }

}