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
        val file = createFileFromText(project, "$newNameString 0")
        return file.firstChild.firstChild as CaosScriptIsCommandToken
    }

    fun createSubroutineNameElement(project: Project, newNameString: String): CaosScriptSubroutineName? {
        if (newNameString.isEmpty() || !newNameString.matches(SUBROUTINE_NAME_REGEX))
            return null
        val file = createFileFromText(project, "gsub $newNameString")
        val command = file.firstChild.firstChild?.firstChild?.firstChild?.firstChild as CaosScriptCGsub
        return command.subroutineName
    }

    private val NAMED_VAR_REGEX = "[$]?[a-zA-Z_][a-zA-Z_0-9]*".toRegex()

    fun createNamedVar(project: Project, newNameString: String): CaosScriptNamedVar? {
        if (!NAMED_VAR_REGEX.matches(newNameString))
            return null
        val newName = if (newNameString.startsWith("$"))
            newNameString
        else
            "$$newNameString"
        val file = createFileFromText(project, "setv $newName 0")
        var first = file.firstChild
        while (first != null && first !is CaosScriptCAssignment) {
            first = first.firstChild
        }
        if (first !is CaosScriptCAssignment)
            throw NullPointerException("Failed to find constant name in element type factory")
        return first.lvalue?.namedVar
    }

    private val NAMED_CONST_REGEX = "[#]?[a-zA-Z_][a-zA-Z_0-9]*".toRegex()

    fun createNamedConst(project: Project, newNameString: String): CaosScriptNamedConstant? {
        if (!NAMED_CONST_REGEX.matches(newNameString))
            return null
        val newName = if (newNameString.startsWith("#"))
            newNameString
        else
            "#$newNameString"
        val file = createFileFromText(project, "setv var0 $newName")
        var first = file.firstChild
        while (first != null && first !is CaosScriptCAssignment) {
            first = first.firstChild
        }
        if (first !is CaosScriptCAssignment)
            throw NullPointerException("Failed to find constant name in element type factory")
        val rvalue =
                first.rvalueDecimalOrInt
                        ?.let {
                            it.expectsDecimal?.rvalue ?: it.expectsInt?.rvalue
                        }
                        ?: first.expectsValue?.rvalue
                        ?: return null
        return rvalue.namedConstant
    }

    private val CONSTANT_NAME_REGEX = "[a-zA-Z_][a-zA-Z_0-9]*".toRegex()
    fun createConstantName(project: Project, newNameString: String): CaosScriptConstantName? {
        if (!CONSTANT_NAME_REGEX.matches(newNameString))
            return null
        val file = createFileFromText(project, "const $newNameString = 0")
        var first = file.firstChild
        while (first != null && first !is CaosScriptConstantAssignment) {
            first = first.firstChild
        }
        if (first !is CaosScriptConstantAssignment)
            throw NullPointerException("Failed to find constant name in element type factory")
        return first.constantName
    }

    private val TOKEN_NAME_REGEX = "[a-zA-Z_0-9]*".toRegex()
    fun createTokenElement(project: Project, newNameString: String): CaosScriptToken? {
        if (newNameString.isEmpty() || !newNameString.matches(TOKEN_NAME_REGEX))
            return null
        val file = createFileFromText(project, "tokn $newNameString")
        var first = file.firstChild
        while (first != null && first !is CaosScriptCommandCall) {
            first = first.firstChild
        }
        if (first !is CaosScriptCommandCall)
            throw NullPointerException("Failed to find string in element type factory")
        return first.expectsToken?.rvalue?.token
    }

    fun createStringElement(project: Project, newNameString: String): CaosScriptRvalue? {
        val file = createFileFromText(project, "sets var1 \"$newNameString\"")
        var first = file.firstChild
        while (first != null && first !is CaosScriptCAssignment) {
            first = first.firstChild
        }
        if (first !is CaosScriptCAssignment)
            throw NullPointerException("Failed to find string in element type factory")
        return first.expectsString?.rvalue
    }

}