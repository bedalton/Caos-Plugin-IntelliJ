package com.badahori.creatures.plugins.intellij.agenteering.caos.lang

import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.DisableSpellcheckForCommandFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosApplicationSettingsService
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.startOffset
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.spellchecker.inspections.PlainTextSplitter
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy
import com.intellij.spellchecker.tokenizer.TokenConsumer
import com.intellij.spellchecker.tokenizer.Tokenizer
import korlibs.io.util.unescape

class CaosSpellcheckingStrategy : SpellcheckingStrategy() {

    private val EMPTY_FIXES = emptyArray<LocalQuickFix>()

    override fun getTokenizer(element: PsiElement?): Tokenizer<*> {
        return when (element) {
            is CaosScriptComment, is PsiComment -> TEXT_TOKENIZER
            is CaosScriptRvalue -> CaosScriptRValueTokenizer
            else -> {
                EMPTY_TOKENIZER
            }
        }
    }

    override fun getRegularFixes(
        element: PsiElement?,
        textRange: TextRange,
        useRename: Boolean,
        typo: String?,
    ): Array<LocalQuickFix> {
        return super.getRegularFixes(element, textRange, useRename, typo) + when (element) {
            is CaosScriptComment, is PsiComment -> EMPTY_FIXES
            is CaosScriptRvalue -> getRValueFixes(element, typo)
            else -> EMPTY_FIXES
        }
    }

    private fun getRValueFixes(element: PsiElement, type: String?): Array<LocalQuickFix> {
        val parentCommand = (element.parent as? CaosScriptCommandElement)
            ?.commandStringUpper
            ?: return EMPTY_FIXES
        if (parentCommand !in CaosScriptRValueTokenizer.commandsToCheck) {
            return EMPTY_FIXES
        }

        val disable = parentCommand !in CaosApplicationSettingsService.getInstance().noSpellcheckCommands
        return arrayOf(DisableSpellcheckForCommandFix(parentCommand, disable))
    }
}

internal object CaosScriptRValueTokenizer : Tokenizer<CaosScriptRvalue>() {

    private val mySplitter = PlainTextSplitter.getInstance()

    override fun tokenize(element: CaosScriptRvalue, consumer: TokenConsumer) {

        val string = element.quoteStringLiteral
            ?: element.c1String
            ?: return

        val parentCommand = element.parent as? CaosScriptCommandElement
            ?: return

        val command = parentCommand
            .commandStringUpper
            ?: return

        val isLvalue = parentCommand.parent is CaosScriptLvalue
        val ignored = CaosApplicationSettingsService.getInstance()
            .noSpellcheckCommands
        if (command !in commandsToCheck.filter { it !in ignored }) {
            return
        }


        val index = element.index

        if (checkCommands.any { it.first == command && it.second == index && it.third == isLvalue }) {
            LOGGER.info("Should spellcheck ${element.text}")
            return consumer.consumeToken(string, mySplitter)
        }
    }

    private val checkCommands = listOf(
        Triple("DBG: OUTS", 0, false),
        Triple("OUTS", 0, false),
        Triple("OUTX", 0, false),
        Triple("EAME", 0, true),
        Triple("GAME", 0, true),
        Triple("NAME", 0, true),
        Triple("ADDS", 1, false),
        Triple("SETS", 1, false),
        Triple("LOWA", 1, false),
        Triple("ORDR SHOU", 0, false),
        Triple("ORDR SIGN", 0, false),
        Triple("ORDR TACT", 0, false),
        Triple("ORDR WRIT", 1, false),
        Triple("PTXT", 0, false),
        Triple("SEZZ", 0, false),
        Triple("SINS", 0, false),
        Triple("SINS", 2, false),
        Triple("SUBS", 0, false),
        Triple("UPPA", 0, false),
    )

    internal val commandsToCheck = checkCommands.map { it.first }.toSet()
}


private val breakChars = "[\\p{L}'\\\\]".toRegex()

private fun getSpellcheckRanges(element: PsiElement): List<Pair<String, TextRange>> {
    val text = element.text
    val startOffset = element.startOffset;
    var start = (text.takeWhile { !breakChars.matches("$it") }).length
    var end = start
    val out = mutableListOf<Pair<String, TextRange>>()
    for ((i, char) in text.withIndex()) {
        if (breakChars.matches("" + char)) {
            end = i
        } else if (start < end) {
            val wordText = text.substring(start, i).unescape()
                .trim('\\', '\'', ' ', '\n', '\t', '\b', '\r')
            val range = TextRange(startOffset + start, startOffset + start + end)
            out.add(Pair(wordText, range))
            start = end
        } else {
            start = i
            end = i
        }
    }
    return out
}