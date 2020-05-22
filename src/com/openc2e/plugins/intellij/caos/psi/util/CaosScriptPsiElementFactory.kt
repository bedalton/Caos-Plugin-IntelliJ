package com.openc2e.plugins.intellij.caos.psi.util

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFile
import com.openc2e.plugins.intellij.caos.lang.CaosScriptLanguage
import com.openc2e.plugins.intellij.caos.psi.api.*
import java.lang.RuntimeException

object CaosScriptPsiElementFactory {

    private val SUBROUTINE_NAME_REGEX = "[a-z_A-Z_0-9:$]{4}".toRegex()

    private fun createFileFromText(project: Project, text: String): CaosScriptFile {
        return PsiFileFactory.getInstance(project).createFileFromText("dummy.caosdef", CaosScriptLanguage.instance, text) as CaosScriptFile
    }

    fun createCommandTokenElement(project: Project, newNameString: String): CaosScriptIsCommandToken {
        val file = createFileFromText(project, "$newNameString 0")
        return file.firstChild.firstChild as CaosScriptIsCommandToken
    }

    fun createSubroutineNameElement(project: Project, newNameString: String): CaosScriptSubroutineName? {
        if (newNameString.isEmpty() || !newNameString.matches(SUBROUTINE_NAME_REGEX))
            return null
        val commandCall = getCommandCall(project, "gsub $newNameString")
        commandCall?.cGsub?.subroutineName?.let {
            return it
        }
        throw NullPointerException("Failed to find gsub in command")
    }

    private val NAMED_VAR_REGEX = "[$]?[a-zA-Z_][a-zA-Z_0-9]*".toRegex()

    fun createNamedVar(project: Project, newNameString: String): CaosScriptNamedVar? {
        if (!NAMED_VAR_REGEX.matches(newNameString))
            return null
        val script = if (newNameString.startsWith("$"))
            "* $newNameString = var0"
        else
            "* $$newNameString = var0"
        val file = createFileFromText(project, script)
        val comment = file.firstChild?.firstChild as? CaosScriptComment
                ?: throw RuntimeException("Failed to find expected comment in caos element factory script")
        return comment.namedVarAssignment?.namedVar

    }

    private val NAMED_CONST_REGEX = "[#]?[a-zA-Z_][a-zA-Z_0-9]*".toRegex()

    fun createNamedConst(project: Project, newNameString: String): CaosScriptNamedConstant? {
        if (!NAMED_CONST_REGEX.matches(newNameString))
            return null
        val script = if (newNameString.startsWith("#"))
            "* $newNameString = 10"
        else
            "* #$newNameString = 10"
        val file = createFileFromText(project, script)
        val comment = file.firstChild?.firstChild as? CaosScriptComment
                ?: throw RuntimeException("Failed to find expected comment in caos element factory script")
        return comment.constantAssignment?.namedConstant
    }

    private val TOKEN_NAME_REGEX = "[a-zA-Z_0-9]*".toRegex()
    fun createTokenElement(project: Project, newNameString: String): CaosScriptToken? {
        val script = "setv var0 tokn $newNameString"
        val commandCall = getCommandCall(project, script)
                ?: return null
        return commandCall.expectsDecimal?.rvalue?.token
    }

    fun createStringElement(project: Project, newNameString: String): CaosScriptRvalue? {
        val script = "sets var1 \"$newNameString\""
        val commandCall = getCommandCall(project, script)
                ?: return null
        return (commandCall.expectsQuoteString?.rvalue ?: commandCall.expectsC1String?.rvalue)
    }

    private fun getCommandCall(project:Project, script:String, throws:Boolean = true) : CaosScriptCommandCall? {
        val file = createFileFromText(project, script)
        var first = file.firstChild
        while (first != null && first !is CaosScriptCommandCall) {
            first = first.firstChild
        }
        if (first is CaosScriptCommandCall)
           return first
        if (throws)
            throw NullPointerException("Failed to find command call in element type factory for script: '${script}'")
        return null
    }

}