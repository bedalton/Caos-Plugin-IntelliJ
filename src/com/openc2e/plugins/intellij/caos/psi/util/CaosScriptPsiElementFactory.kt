package com.openc2e.plugins.intellij.caos.psi.util

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFile
import com.openc2e.plugins.intellij.caos.lang.CaosScriptLanguage
import com.openc2e.plugins.intellij.caos.psi.api.*

object CaosScriptPsiElementFactory {

    private val SUBROUTINE_NAME_REGEX = "[a-z_A-Z_0-9:$]{4}".toRegex()

    private fun createFileFromText(project: Project, text: String): CaosScriptFile {
        return PsiFileFactory.getInstance(project).createFileFromText("dummy.caosdef", CaosScriptLanguage.instance, text) as CaosScriptFile
    }

    fun createCommandTokenElement(project: Project, newNameString: String): CaosScriptIsCommandToken {
        val file= createFileFromText(project, "$newNameString 0");
        val command =  file.firstChild.firstChild as CaosScriptIsCommandToken
        return command
    }

    fun createSubroutineNameElement(project: Project, newNameString: String): CaosScriptSubroutineName? {
        if (newNameString.isEmpty() || !newNameString.matches(SUBROUTINE_NAME_REGEX))
            return null
        val file= createFileFromText(project, "gsub $newNameString");
        val command =  file.firstChild.firstChild?.firstChild?.firstChild?.firstChild as CaosScriptCGsub
        return command.subroutineName
    }

    val NAMED_VAR_REGEX = "[$]?[a-zA-Z_][a-zA-Z_0-9]*".toRegex()

    fun createNamedVar(project: Project, newNameString: String) : CaosScriptNamedVar? {
        if (!NAMED_VAR_REGEX.matches(newNameString))
            return null
        val newName = if (newNameString.startsWith("$"))
            newNameString
        else
            "$$newNameString"
        createFileFromText(project,"setv $newName 0")
        return null
    }

    val NAMED_CONST_REGEX = "[#]?[a-zA-Z_][a-zA-Z_0-9]*".toRegex()
    fun createNamedConst(project: Project, newNameString: String) : CaosScriptNamedConstant? {
        if (!NAMED_CONST_REGEX.matches(newNameString))
            return null
        val newName = if (newNameString.startsWith("#"))
            newNameString
        else
            "#$newNameString"
        val file = createFileFromText(project,"setv var0 $newName")
        return null
    }

    val CONSTANT_NAME_REGEX = "[a-zA-Z_][a-zA-Z_0-9]*".toRegex()
    fun createConstantName(project: Project, newNameString: String) : CaosScriptConstantName? {
        if (!CONSTANT_NAME_REGEX.matches(newNameString))
            return null
        val file = createFileFromText(project, "const $newNameString = 0")

        return null
    }

}