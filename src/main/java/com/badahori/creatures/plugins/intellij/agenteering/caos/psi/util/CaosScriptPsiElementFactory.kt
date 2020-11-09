package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*

object CaosScriptPsiElementFactory {

    private val SUBROUTINE_NAME_REGEX = "[a-zA-Z0-9_:$#!]{4}".toRegex()

    fun createFileFromText(project: Project, text: String, fileName: String = "dummy.cos"): CaosScriptFile {
        return (PsiFileFactory.getInstance(project).createFileFromText(fileName, CaosScriptLanguage, text) as CaosScriptFile)
    }

    fun createCommandTokenElement(project: Project, newNameString: String): CaosScriptIsCommandToken? {
        if (!SUBROUTINE_NAME_REGEX.matches(newNameString))
            return null
        val factoryElement = createAndGet(project, "____X____DEF__ $newNameString", CaosScriptWordFactoryElement::class.java)
        return factoryElement?.aWord
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

    private val TOKEN_NAME_REGEX = "[a-zA-Z_0-9]*".toRegex()
    fun createTokenElement(project: Project, newNameString: String): CaosScriptToken? {
        if (!TOKEN_NAME_REGEX.matches(newNameString))
            return null
        @Suppress("SpellCheckingInspection") val script = "sndf $newNameString"
        val file = createFileFromText(project, script)
        return PsiTreeUtil.collectElementsOfType(file, CaosScriptToken::class.java).firstOrNull()
    }

    fun createStringRValue(project: Project, newNameString: String, start: Char, end: Char = start): CaosScriptRvalue {
        val script = "$start$newNameString$end"
        return createRValue(project, script)
    }

    fun createNumber(project: Project, number: Int) : CaosScriptRvalue {
        return createRValue(project, "$number")
    }

    fun createNumber(project: Project, number: Long) : CaosScriptRvalue {
        return createRValue(project, "$number")
    }

    fun createFloat(project: Project, number: Float) : CaosScriptRvalue {
        return createRValue(project, "$number")
    }

    private fun getCommandCall(project: Project, script: String) : CaosScriptCommandCall? {
        return createAndGet(project, script, CaosScriptCommandCall::class.java)
    }

    private fun createRValue(project: Project, expr: String) : CaosScriptRvalue {
        val script = "____X____EXPR__ $expr"
        return createAndGet(project, script, CaosScriptRvalue::class.java)!!
    }

    fun newLine(project: Project): PsiElement {
        val file = createFileFromText(project, "inst\nendm")
        return PsiTreeUtil.collectElementsOfType(file, CaosScriptSpaceLikeOrNewline::class.java).first().apply{
            assert (text == "\n") { "Newline factory method returned non-newline space. Text: '$text'" }
        }
    }

    fun newLines(project: Project, numNewLines: Int): PsiElement {
        val lines = (0 until numNewLines).joinToString("") { "\n" }
        val file = createFileFromText(project, "inst${lines}endm")
        return PsiTreeUtil.collectElementsOfType(file, CaosScriptSpaceLikeOrNewline::class.java).first().apply{
            assert (text == lines) { "Newline factory method returned non-newline space. Actual: '$text'" }
        }
    }

    fun comma(project: Project): PsiElement {
        return createAndGet(project, "slim,slim", CaosScriptSpaceLikeOrNewline::class.java)!!
    }

    fun spaceLikeOrNewlineSpace(project: Project): PsiElement {
        return createAndGet(project, "slim slim", CaosScriptSpaceLikeOrNewline::class.java)!!
    }

    fun <PsiT:PsiElement> createAndGet(project:Project, script:String, type:Class<PsiT>) : PsiT? {
        return PsiTreeUtil.collectElementsOfType(createFileFromText(project, script), type).first()
    }

    fun createCodeBlock(project: Project, text: String) : CaosScriptCodeBlock {
        return createAndGet(project, text, CaosScriptCodeBlock::class.java)!!
    }

}