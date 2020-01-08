package com.openc2e.plugins.intellij.caos.psi.util

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFile
import com.openc2e.plugins.intellij.caos.lang.CaosScriptLanguage
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCommandToken

object CaosScriptPsiElementFactory {

    private fun createFileFromText(project: Project, text: String): CaosScriptFile {
        return PsiFileFactory.getInstance(project).createFileFromText("dummy.caosdef", CaosScriptLanguage.instance, text) as CaosScriptFile
    }

    fun createCommandTokenElement(project: Project, newNameString: String): CaosScriptCommandToken {
        val file= createFileFromText(project, "$newNameString 0");
        val command =  file.firstChild.firstChild as CaosScriptCommandToken
        return command
    }

}