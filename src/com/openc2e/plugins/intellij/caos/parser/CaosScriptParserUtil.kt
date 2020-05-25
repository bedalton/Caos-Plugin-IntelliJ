package com.openc2e.plugins.intellij.caos.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.openapi.util.Key
import com.intellij.psi.tree.IElementType
import com.openc2e.plugins.intellij.caos.project.CaosScriptProjectSettings
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptTokenSets.Companion.ScriptTerminators
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptTokenSets.Companion.WHITE_SPACE_LIKE
import gnu.trove.TObjectLongHashMap

object CaosScriptParserUtil : GeneratedParserUtilBase() {
    private val MODES_KEY = Key.create<TObjectLongHashMap<String>>("MODES_KEY")
    private val CAOS_VARIANT = Key.create<String>("CAOS_VARIANT")
    private var blocks = 0

    private fun getParsingModes(builder_: PsiBuilder): TObjectLongHashMap<String>? {
        var flags = builder_.getUserData<TObjectLongHashMap<String>>(MODES_KEY)
        if (flags == null) {
            builder_.putUserData(MODES_KEY, TObjectLongHashMap<String>().also { flags = it })
        }
        return flags
    }

    @JvmStatic
    fun isVariant(builder_: PsiBuilder,
                  level: Int, variant: String?): Boolean {
        return CaosScriptProjectSettings.variant == variant
    }

    @JvmStatic
    fun enterMode(builder_: PsiBuilder,
                  level: Int, mode: String?): Boolean {
        val flags = getParsingModes(builder_)
        if (!flags!!.increment(mode)) {
            flags.put(mode, 1)
        }
        return true
    }

    @JvmStatic
    fun enterBlock(builder_: PsiBuilder?,
                   level: Int): Boolean {
        blocks++
        return true
    }

    @JvmStatic
    fun exitBlock(builder_: PsiBuilder?,
                  level: Int): Boolean {
        blocks--
        return true
    }

    @JvmStatic
    fun insideBlock(builder_: PsiBuilder?,
                    level: Int): Boolean {
        return blocks > 0
    }

    @JvmStatic
    fun exitMode(builder_: PsiBuilder,
                 level: Int, mode: String): Boolean {
        val flags = getParsingModes(builder_)
        val count = flags!![mode]
        if (count == 1L) {
            flags.remove(mode)
        } else if (count > 1) {
            flags.put(mode, count - 1)
        } else {
            builder_.error("Could not exit inactive '" + mode + "' mode at offset " + builder_.currentOffset)
        }
        return true
    }

    @JvmStatic
    fun inMode(builder_: PsiBuilder,
               level: Int, mode: String?): Boolean {
        return getParsingModes(builder_)!![mode] > 0
    }

    @JvmStatic
    fun notInMode(builder_: PsiBuilder,
                  level: Int, mode: String?): Boolean {
        return getParsingModes(builder_)!![mode] == 0L
    }

    @JvmStatic
    fun eofNext(builder_: PsiBuilder,
                level: Int): Boolean {
        return builder_.lookAhead(1) == null || builder_.eof()
    }

    @JvmStatic
    fun eol(builder_: PsiBuilder, level: Int): Boolean {
        val text = builder_.tokenText
        return text != null && text.contains("\n") || eof(builder_, level)
    }

    @JvmStatic
    fun eos(builder_: PsiBuilder, level: Int): Boolean {
        var type: IElementType?
        var offset = 0
        do {
            type = builder_.lookAhead(offset++)
        } while (type != null && WHITE_SPACE_LIKE.contains(type))
        return type == null || ScriptTerminators.contains(type) || eof(builder_, level)
    }
}