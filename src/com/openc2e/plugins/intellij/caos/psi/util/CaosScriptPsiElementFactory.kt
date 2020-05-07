package com.openc2e.plugins.intellij.caos.psi.util

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFile
import com.openc2e.plugins.intellij.caos.lang.CaosScriptLanguage
import com.openc2e.plugins.intellij.caos.psi.api.*

object CaosScriptPsiElementFactory {

    private val SUBROUTINE_NAME_REGEX = "[a-z_A-Z]{4}".toRegex()

    private fun createFileFromText(project: Project, text: String): CaosScriptFile {
        return PsiFileFactory.getInstance(project).createFileFromText("dummy.caosdef", CaosScriptLanguage.instance, text) as CaosScriptFile
    }

    fun createCommandTokenElement(project: Project, newNameString: String): CaosScriptIsCommandToken {
        val file= createFileFromText(project, "$newNameString 0");
        val command =  file.firstChild.firstChild as CaosScriptIsCommandToken
        return command
    }

    fun createSubroutineNameElement(project: Project, newNameString: String): CaosScriptSubroutineName? {
        if (newNameString.isNotEmpty() || !newNameString.matches(SUBROUTINE_NAME_REGEX))
            return null
        val file= createFileFromText(project, "gsub $newNameString");
        val command =  file.firstChild.firstChild?.firstChild?.firstChild?.firstChild as CaosScriptCGsub
        return command.subroutineName
    }

}