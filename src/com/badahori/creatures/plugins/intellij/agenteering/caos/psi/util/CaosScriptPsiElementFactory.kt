package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*

object CaosScriptPsiElementFactory {

    private val SUBROUTINE_NAME_REGEX = "[a-z_A-Z_0-9:$]{4}".toRegex()

    private fun createFileFromText(project: Project, text: String): CaosScriptFile {
        return PsiFileFactory.getInstance(project).createFileFromText("dummy.caosdef", CaosScriptLanguage.instance, text) as CaosScriptFile
    }

    fun createCommandTokenElement(project: Project, newNameString: String): CaosScriptIsCommandToken? {
        if (!SUBROUTINE_NAME_REGEX.matches(newNameString))
            return null
        val file = createFileFromText(project, "____X____DEF__ $newNameString")
        val factoryElement = file.firstChild as CaosScriptWordFactoryElement
        return factoryElement.aWord
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


    fun createNamedVar(project: Project, newNameStringIn: String): CaosScriptNamedVar? {
        if (!NAMED_VAR_REGEX.matches(newNameStringIn))
            return null
        val newNameString = if (!newNameStringIn.startsWith("$"))
            newNameStringIn
        else
            "$${newNameStringIn}"
        val script = createRValue(project, newNameString)
        return script.expression?.namedVar

    }

    private val NAMED_CONST_REGEX = "[#]?[a-zA-Z_][a-zA-Z_0-9]*".toRegex()

    fun createNamedConst(project: Project, newNameString: String): CaosScriptNamedConstant? {
        if (!NAMED_CONST_REGEX.matches(newNameString))
            return null
        val script = if (newNameString.startsWith("#"))
            newNameString
        else
            "#$newNameString"
        val file = createRValue(project, script)
        return file.expression?.namedConstant
    }

    private val TOKEN_NAME_REGEX = "[a-zA-Z_0-9]*".toRegex()
    fun createTokenElement(project: Project, newNameString: String): CaosScriptToken? {
        if (!TOKEN_NAME_REGEX.matches(newNameString))
            return null
        val script = "sndf $newNameString"
        val file = createFileFromText(project, script)
        return PsiTreeUtil.collectElementsOfType(file, CaosScriptToken::class.java).firstOrNull()
    }

    fun createStringRValue(project: Project, newNameString: String, start:Char, end:Char = start): CaosScriptRvalue {
        val script = "$start$newNameString$end"
        return createRValue(project,script)
    }

    fun createNumber(project:Project, number:Int) : CaosScriptExpression {
        return createRValue(project, "$number").expression!!
    }

    fun createFloat(project:Project, number:Int) : CaosScriptExpression {
        return createRValue(project, "$number").expression!!
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

    private fun createRValue(project:Project, expr:String) : CaosScriptRvalue {
        val script = "____X____EXPR__ $expr"
        return PsiTreeUtil.findChildOfType(createFileFromText(project, script).firstChild, CaosScriptRvalue::class.java)!!
    }

    fun newLine(project: Project): PsiElement {
        val file = createFileFromText(project, "enum 0 0 0\nnext")
        return PsiTreeUtil.collectElementsOfType(file, CaosScriptSpaceLikeOrNewline::class.java).first()
    }

    fun newLines(project: Project, numNewLines:Int): PsiElement {
        val lines = (0 until numNewLines).joinToString("") { "\n" }
        val file = createFileFromText(project, "enum 0 0 0${lines}next")
        return PsiTreeUtil.collectElementsOfType(file, CaosScriptSpaceLikeOrNewline::class.java).first()
    }

    fun comma(project: Project): PsiElement {
        return createFileFromText(project, "slim,slim")
                .firstChild
                .firstChild
                .firstChild
                .getChildOfType(CaosScriptSpaceLikeOrNewline::class.java)!!
    }

    fun spaceLikeOrNewlineSpace(project: Project): PsiElement {
        return createFileFromText(project, "slim slim")
                .firstChild
                .firstChild
                .firstChild
                .getChildOfType(CaosScriptSpaceLikeOrNewline::class.java)!!
    }

    fun createCodeBlock(project:Project, text:String) : CaosScriptCodeBlock {
        return createFileFromText(project, text).firstChild.firstChild.firstChild as CaosScriptCodeBlock
    }

}