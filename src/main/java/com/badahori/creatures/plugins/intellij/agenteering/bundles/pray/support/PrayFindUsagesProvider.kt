package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support

import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lexer.PrayTypes.*
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayInlineFile
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayInlineText
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayPrayTag
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.lexer.PrayLexerAdapter
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.PrayTag

/**
 * Establishes a find usages provider
 */
class PrayFindUsagesProvider : FindUsagesProvider{

    override fun getWordsScanner(): WordsScanner? {
        return DefaultWordsScanner(
                PrayLexerAdapter(),
                TokenSet.create(Pray_STRING, Pray_SINGLE_QUO_STRING, Pray_DOUBLE_QUO_STRING, Pray_ID),
                TokenSet.create(Pray_BLOCK_COMMENT, Pray_LINE_COMMENT),
                TokenSet.create(Pray_INT, Pray_FLOAT)
        )
    }
    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        return element.text
    }

    override fun getDescriptiveName(element: PsiElement): String {
        return when (element) {
            is PrayInlineFile -> "inline ${element.inputFileNameString} -> ${element.outputFileNameString}"
            is PrayInlineText -> "inline@${element.inputFileName}"
            is PrayPrayTag -> "Tag: ${element.tagName} = ${element.tagTagValue.text}"
            else -> element.text
        }
    }

    override fun getType(element: PsiElement): String {
        return when (element) {
            is PrayInlineFile -> "Inline File"
            is PrayInlineText -> "Inline @ text"
            is PrayPrayTag -> "Tag"
            else -> "element"
        }
    }

    override fun getHelpId(element: PsiElement): String? {
        return null
    }

    override fun canFindUsagesFor(element: PsiElement): Boolean {
        return element is CaosScriptCompositeElement
    }
}