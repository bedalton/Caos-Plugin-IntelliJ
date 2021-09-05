package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.impl.source.resolve.FileContextUtil
import gnu.trove.TObjectLongHashMap

object PrayParserUtil : GeneratedParserUtilBase() {
    private val MODES_KEY = Key.create<TObjectLongHashMap<String>>("MODES_KEY")

    private fun getParsingModes(builder_: PsiBuilder): TObjectLongHashMap<String>? {
        var flags = builder_.getUserData(MODES_KEY)
        if (flags == null) {
            builder_.putUserData(MODES_KEY, TObjectLongHashMap<String>().also { flags = it })
        }
        return flags
    }

    /**
     * Will not be CaosPsiFile
     */
    @JvmStatic
    fun psiFile(builder_: PsiBuilder): PsiFile? {
        return builder_.getUserData(FileContextUtil.CONTAINING_FILE_KEY)
    }

    @JvmStatic
    fun enterMode(
        builder_: PsiBuilder,
        @Suppress("UNUSED_PARAMETER") level: Int, mode: String?
    ): Boolean {
        val flags = getParsingModes(builder_)
        if (!flags!!.increment(mode)) {
            flags.put(mode, 1)
        }
        return true
    }

    @JvmStatic
    fun exitMode(
        builder_: PsiBuilder,
        @Suppress("UNUSED_PARAMETER") level: Int, mode: String
    ): Boolean {
        val flags = getParsingModes(builder_)
        val count = flags!![mode]
        when {
            count == 1L -> flags.remove(mode)
            count > 1 -> flags.put(mode, count - 1)
        }
        return true
    }

    @JvmStatic
    fun inMode(
        builder_: PsiBuilder,
        @Suppress("UNUSED_PARAMETER") level: Int, mode: String?
    ): Boolean {
        return getParsingModes(builder_)!![mode] > 0
    }

    @JvmStatic
    fun notInMode(
        builder_: PsiBuilder,
        @Suppress("UNUSED_PARAMETER") level: Int, mode: String?
    ): Boolean {
        return getParsingModes(builder_)!![mode] == 0L
    }

    @JvmStatic
    fun eol(builder_: PsiBuilder, level: Int): Boolean {
        if (eof(builder_, level))
            return true
        var lookAhead = 0
        var next = builder_.lookAhead(lookAhead++)
            ?: return true
        val originalText = builder_.originalText
        var char: Char
        val textLength = originalText.length
        while (next == TokenType.WHITE_SPACE) {
            val startIndices = builder_.rawTokenTypeStart(lookAhead)
            val endIndex = builder_.rawTokenTypeStart(lookAhead)
            for (charIndex in startIndices until endIndex) {
                if (charIndex < 0)
                    return false
                if(charIndex == textLength)
                    return true
                char = originalText[charIndex]
                if (char == '\n')
                    return true
                if (char != ' ' && char != '\t')
                    return false
            }
            next = builder_.lookAhead(lookAhead++)
                ?: return true
        }
        return false
    }
}