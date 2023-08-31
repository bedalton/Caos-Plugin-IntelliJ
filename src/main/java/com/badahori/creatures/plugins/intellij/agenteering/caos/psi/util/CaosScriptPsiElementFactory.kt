package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util

import com.badahori.creatures.plugins.intellij.agenteering.caos.exceptions.CaosInvalidTokenLengthException
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.endOffset
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.text.BlockSupport
import com.intellij.psi.util.PsiTreeUtil

object CaosScriptPsiElementFactory {

    internal val C1E_SUBROUTINE_NAME_REGEX = "[a-zA-Z0-9_:$#!*]{4}".toRegex()
    internal val C2E_SUBROUTINE_NAME_REGEX = "[a-zA-Z][a-zA-Z0-9_:\$#!*]+".toRegex()
    fun createFileFromText(project: Project, text: String, variant: CaosVariant = CaosVariant.DS, fileName: String = "dummy.cos"): CaosScriptFile {
        return (PsiFileFactory.getInstance(project).createFileFromText(fileName, CaosScriptLanguage, text) as CaosScriptFile).apply {
            setVariant(variant, true)
        }
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
        val file = createFileFromText(project, script,  CaosVariant.C1, "dummy.cos")
        return PsiTreeUtil.collectElementsOfType(file, CaosScriptToken::class.java).firstOrNull()
    }

    fun createToknCommandWithToken(project: Project, token: String): CaosScriptRvalue {
        if (token.length != 4) {
            throw CaosInvalidTokenLengthException(token.toCharArray(), "Cannot create TOKN statement with invalid token length")
        }
        val script = "tokn $token"
        return createRValue(project, script, CaosVariant.C1)
    }

    fun createStringRValue(project: Project, newNameString: String, start: Char, end: Char = start): CaosScriptRvalue {
        val script = "$start$newNameString$end"
        val variant = if (start == '"') {
            CaosVariant.DS
        } else {
            CaosVariant.C1
        }
        return createRValue(project, script, variant)
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

    private fun createRValue(project: Project, expr: String, variant: CaosVariant = CaosVariant.DS) : CaosScriptRvalue {
        val script = "____X____EXPR__ $expr"
        return createAndGet(project, script, CaosScriptRvalue::class.java, variant)!!
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
            file.setVariant(variant, true) // Used to ensure a string is created not byte-string on '['
        }
        return runWriteAction {
            BlockSupport.getInstance(project).reparseRange(file, 0, file.endOffset, file.text)
            if (pointer.element?.variant != variant) {
                throw Exception("Variant not properly set before fetch created rvalue")
            }
            PsiTreeUtil.collectElementsOfType(pointer.element, type).first()
        }
    }

    fun createCodeBlock(project: Project, text: String) : CaosScriptCodeBlock {
        return createAndGet(project, text, CaosScriptCodeBlock::class.java)!!
    }

    fun createScriptElement(project: Project, tag: String): CaosScriptScriptElement? {
        return createAndGet(project, "$tag\nendm", CaosScriptScriptElement::class.java)
    }

    fun createCAOS2PrayTag(project: Project, correctedCase: String, startsWithQuote: Boolean): CaosScriptCaos2TagName? {
        val tag = if (startsWithQuote)
            "\"$correctedCase\""
        else
            correctedCase
        val script = "*# $tag = \"\"\n"
        return createAndGet(project, script, CaosScriptCaos2TagName::class.java, variant = CaosVariant.DS)
    }

}