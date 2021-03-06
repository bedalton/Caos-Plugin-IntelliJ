package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.document
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.BlockSupportImpl
import com.intellij.psi.util.PsiTreeUtil

object CaosScriptPsiElementFactory {

    private val C1E_SUBROUTINE_NAME_REGEX = "[a-zA-Z0-9_:$#!*]{4}".toRegex()
    private val C2E_SUBROUTINE_NAME_REGEX = "[a-zA-Z][a-zA-Z0-9_:\$#!*]+".toRegex()
    fun createFileFromText(project: Project, text: String, fileName: String = "dummy.cos"): CaosScriptFile {
        return (PsiFileFactory.getInstance(project).createFileFromText(fileName, CaosScriptLanguage, text) as CaosScriptFile)
    }

    fun createCommandTokenElement(project: Project, newNameString: String): CaosScriptIsCommandToken? {
        if (!C1E_SUBROUTINE_NAME_REGEX.matches(newNameString))
            return null
        val factoryElement = createAndGet(project, "____X____DEF__ $newNameString", CaosScriptWordFactoryElement::class.java)
        return factoryElement?.aWord
    }

    fun createSubroutineNameElement(project: Project, variant:CaosVariant, newNameString: String): CaosScriptSubroutineName? {
        val subroutineNameRegex = if (variant.isOld) C1E_SUBROUTINE_NAME_REGEX else C2E_SUBROUTINE_NAME_REGEX
        if (newNameString.isEmpty() || !newNameString.matches(subroutineNameRegex)) {
            return null
        }
        val commandCall = getCommandCall(project, "gsub $newNameString", variant)
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

    private fun getCommandCall(project: Project, script: String, variant:CaosVariant) : CaosScriptCommandCall? {
        return createAndGet(project, script, CaosScriptCommandCall::class.java, variant)
    }

    private fun createRValue(project: Project, expr: String) : CaosScriptRvalue {
        val script = "____X____EXPR__ $expr"
        return createAndGet(project, script, CaosScriptRvalue::class.java)!!
    }

    fun newLine(project: Project): PsiElement {
        val file = createFileFromText(project, "inst\nendm")
        return PsiTreeUtil.collectElementsOfType(file, PsiWhiteSpace::class.java).first().apply{
            assert(text == "\n") { "Newline factory method returned non-newline space. Text: '$text'" }
        }
    }

    fun newLines(project: Project, numNewLines: Int): PsiElement {
        val lines = (0 until numNewLines).joinToString("") { "\n" }
        val file = createFileFromText(project, "inst${lines}endm")
        return PsiTreeUtil.collectElementsOfType(file, PsiWhiteSpace::class.java).first().apply{
            assert(text == lines) { "Newline factory method returned non-newline space. Actual: '$text'" }
        }
    }

    fun comma(project: Project): PsiElement {
        return createAndGet(project, "slim,slim", PsiWhiteSpace::class.java)!!
    }

    fun spaceLikeOrNewlineSpace(project: Project): PsiElement {
        return createAndGet(project, "slim slim", PsiWhiteSpace::class.java)!!
    }

    fun <PsiT : PsiElement> createAndGet(project: Project, script: String, type: Class<PsiT>, variant: CaosVariant = CaosVariant.C1) : PsiT? {
        val file = createFileFromText(project, script)
        val pointer = SmartPointerManager.createPointer(file)
        (file as? CaosScriptFile)?.apply {
            file.variant = variant // Used to ensure a string is created not bytestring on '['
        }
        runWriteAction {
            val range = file.textRange
            val recalledFile = pointer.element!!
            BlockSupportImpl.getInstance(project).reparseRange(recalledFile, range.startOffset, range.endOffset, recalledFile.text)
        }
        return PsiTreeUtil.collectElementsOfType(pointer.element, type).first()
    }

    fun createCodeBlock(project: Project, text: String) : CaosScriptCodeBlock {
        return createAndGet(project, text, CaosScriptCodeBlock::class.java)!!
    }

    fun createScriptElement(project: Project, tag: String): CaosScriptScriptElement? {
        return createAndGet(project, "$tag\nendm", CaosScriptScriptElement::class.java)
    }

}