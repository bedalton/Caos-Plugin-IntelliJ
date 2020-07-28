package com.badahori.creatures.plugins.intellij.agenteering.caos.parser

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant.C3
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant.DS
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets.Companion.ScriptTerminators
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets.Companion.WHITE_SPACE_LIKE_WITH_COMMENT
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.variant
import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.resolve.FileContextUtil
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import gnu.trove.TObjectLongHashMap

@Suppress("UNUSED_PARAMETER", "unused")
object CaosScriptParserUtil : GeneratedParserUtilBase() {
    private val MODES_KEY = Key.create<TObjectLongHashMap<String>>("MODES_KEY")
    private val CAOS_VARIANT = Key.create<CaosVariant>("CAOS_VARIANT")
    private val expectsType: MutableList<Int> = mutableListOf()
    const val NUMBER = 0
    const val STRING = 1
    const val TOKEN = 4
    const val OTHER = -1
    const val ANIM = 5
    const val ANY = 3
    const val WHITE_SPACE_OPTIONAL = "whiteSpaceOptional"
    val needsNoWhitespace = TokenSet.create(
            CaosScriptTypes.CaosScript_BYTE_STRING,
            CaosScriptTypes.CaosScript_OPEN_BRACKET,
            CaosScriptTypes.CaosScript_INT,
            CaosScriptTypes.CaosScript_FLOAT,
            CaosScriptTypes.CaosScript_QUOTE_STRING_LITERAL,
            CaosScriptTypes.CaosScript_CHAR_CHAR,
            CaosScriptTypes.CaosScript_CHARACTER
    )
    private var blocks = 0
    private val blockEnds = listOf<IElementType>(
            CaosScriptTypes.CaosScript_K_ENDM, CaosScriptTypes.CaosScript_K_NEXT, CaosScriptTypes.CaosScript_K_ELSE, CaosScriptTypes.CaosScript_K_ENDI, CaosScriptTypes.CaosScript_K_ELIF, CaosScriptTypes.CaosScript_K_REPE, CaosScriptTypes.CaosScript_K_NSCN, CaosScriptTypes.CaosScript_K_UNTL, CaosScriptTypes.CaosScript_K_EVER, CaosScriptTypes.CaosScript_K_RETN, CaosScriptTypes.CaosScript_K_SUBR
    )

    private fun getParsingModes(builder_: PsiBuilder): TObjectLongHashMap<String>? {
        var flags = builder_.getUserData<TObjectLongHashMap<String>>(MODES_KEY)
        if (flags == null) {
            builder_.putUserData(MODES_KEY, TObjectLongHashMap<String>().also { flags = it })
        }
        return flags
    }

    /*@JvmStatic
    fun isVariant(builder_: PsiBuilder,
                  level: Int, variant: String?): Boolean {
        return CaosScriptProjectSettings.variant.code == variant
    }*/

    @JvmStatic
    fun isVariant(builder_: PsiBuilder,
                  level: Int,
                  variant: CaosVariant): Boolean {
        val fileVariant = psiFile(builder_)?.let { ModuleUtil.findModuleForPsiElement(it)  }
                ?.variant
                ?: return false//CaosScriptProjectSettings.variant
        return variant == fileVariant
    }

    @JvmStatic
    fun isOldVariant(builder_: PsiBuilder,
                  level: Int): Boolean {
        val fileVariant = psiFile(builder_)?.let { ModuleUtil.findModuleForPsiElement(it)  }
                ?.variant
                ?: return false//CaosScriptProjectSettings.variant
        return fileVariant.isOld
    }

    @JvmStatic
    fun isNewerVariant(builder_: PsiBuilder,
                     level: Int): Boolean {
        val fileVariant = psiFile(builder_)?.let { ModuleUtil.findModuleForPsiElement(it)  }
                ?.variant
                ?: return false//CaosScriptProjectSettings.variant
        return fileVariant.isNotOld
    }

    /**
     * Will not be CaosPsiFile
     */
    @JvmStatic
    fun psiFile(builder_: PsiBuilder) : PsiFile? {
        return builder_.getUserData(FileContextUtil.CONTAINING_FILE_KEY)
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
        if (blocks > 0)
            blocks--
        return true
    }

    @JvmStatic
    fun insideBlock(builder_: PsiBuilder?,
                    level: Int): Boolean {
        return blocks > 0
    }

    @JvmStatic
    fun pushExpectation(builder_: PsiBuilder?,
                        level: Int,
                        expectation: Int
    ): Boolean {
        if (ignoreExpects())
            return true
        if (expectsType.isEmpty())
            expectsType.add(expectation)
        else
            expectsType.add(0, expectation)
        return true
    }

    @JvmStatic
    fun popExpectation(builder_: PsiBuilder?,
                       level: Int): Boolean {
        if (ignoreExpects())
            return true
        if (expectsType.isEmpty())
            return true
        expectsType.remove(0)
        return true
    }

    @JvmStatic
    fun expects(builder_: PsiBuilder?,
                level: Int,
                expectation: Int
    ): Boolean {
        return expects(builder_, level, expectation, true)
    }

    @JvmStatic
    fun expects(builder_: PsiBuilder?,
                level: Int,
                expectation: Int,
                default:Boolean
    ): Boolean {
        if (ignoreExpects())
            return default
        if (expectsType.isEmpty()) {
            //LOGGER.severe("Checking expectation '$expectation' when no expectations in stack")
            return default
        }
        return expectsType[0] == expectation
    }

    @JvmStatic
    fun expectsValue(builder_: PsiBuilder?,
                level: Int,
                 default:Boolean
    ): Boolean {
        if (ignoreExpects())
            return default
        return expectsType.isNotEmpty()
    }

    private fun ignoreExpects() : Boolean {
        return false && (CaosScriptProjectSettings.variant == C3 || CaosScriptProjectSettings.variant == DS)
    }

    @JvmStatic
    fun exitMode(builder_: PsiBuilder,
                 level: Int, mode: String): Boolean {
        val flags = getParsingModes(builder_)
        val count = flags!![mode]
        when {
            count == 1L -> flags.remove(mode)
            count > 1 -> flags.put(mode, count - 1)
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
    fun getVirtualFile(builder_: PsiBuilder) : VirtualFile? {
        return psiFile(builder_)?.virtualFile
    }

    @JvmStatic
    fun eofNext(builder_: PsiBuilder,
                level: Int): Boolean {
        return builder_.lookAhead(1) == null || builder_.eof()
    }

    @JvmStatic
    fun eol(builder_: PsiBuilder, level: Int): Boolean {
        val text = builder_.tokenText
        return (text != null && text.contains("\n")) || eof(builder_, level)
    }

    @JvmStatic
    fun eos(builder_: PsiBuilder, level: Int): Boolean {
        var type: IElementType?
        var offset = 0
        do {
            type = builder_.lookAhead(offset++)
        } while (type != null && WHITE_SPACE_LIKE_WITH_COMMENT.contains(type))
        return type == null || ScriptTerminators.contains(type) || eof(builder_, level)
    }

    @JvmStatic
    fun nextIsString(builder_: PsiBuilder, level: Int): Boolean {
        return when (builder_.lookAhead(1)) {
            in nextIsString -> true
            CaosScriptTypes.CaosScript_K_HIST -> return builder_.lookAhead(1) in histStrings
            CaosScriptTypes.CaosScript_K_PRT_COL -> builder_.lookAhead(1) == CaosScriptTypes.CaosScript_K_NAME
            CaosScriptTypes.CaosScript_K_PRAY -> builder_.lookAhead(1) in prayStrings
            else -> return false
        }
    }

    private val nextIsString: List<IElementType> = listOf(
            CaosScriptTypes.CaosScript_K_CATX,
            CaosScriptTypes.CaosScript_K_WILD,
            CaosScriptTypes.CaosScript_K_BKGD,
            CaosScriptTypes.CaosScript_K_TRAN,
            CaosScriptTypes.CaosScript_K_DBG_NUM,
            CaosScriptTypes.CaosScript_K_DBGA,
            CaosScriptTypes.CaosScript_K_FVWM,
            CaosScriptTypes.CaosScript_K_GTOS,
            CaosScriptTypes.CaosScript_K_BKDS,
            CaosScriptTypes.CaosScript_K_ERID,
            CaosScriptTypes.CaosScript_K_MLOC,
            CaosScriptTypes.CaosScript_K_RATE,
            CaosScriptTypes.CaosScript_K_RLOC,
            CaosScriptTypes.CaosScript_K_CAOS,
            CaosScriptTypes.CaosScript_K_SORC,
            CaosScriptTypes.CaosScript_K_MMSC,
            CaosScriptTypes.CaosScript_K_RMSC,
            CaosScriptTypes.CaosScript_K_RTIF,
            CaosScriptTypes.CaosScript_K_GAMN,
            CaosScriptTypes.CaosScript_K_READ,
            CaosScriptTypes.CaosScript_K_SUBS,
            CaosScriptTypes.CaosScript_K_VTOS,
            CaosScriptTypes.CaosScript_K_PSWD,
            CaosScriptTypes.CaosScript_K_WRLD
    )
    private val histStrings: List<IElementType> = listOf(
            CaosScriptTypes.CaosScript_K_FOTO,
            CaosScriptTypes.CaosScript_K_NAME,
            CaosScriptTypes.CaosScript_K_NEXT,
            CaosScriptTypes.CaosScript_K_PREV,
            CaosScriptTypes.CaosScript_K_UTXT,
            CaosScriptTypes.CaosScript_K_WNAM,
            CaosScriptTypes.CaosScript_K_WUID,
            CaosScriptTypes.CaosScript_K_MON1,
            CaosScriptTypes.CaosScript_K_MON2
    )
    private val prayStrings: List<IElementType> = listOf(
            CaosScriptTypes.CaosScript_K_PREV,
            CaosScriptTypes.CaosScript_K_NEXT,
            CaosScriptTypes.CaosScript_K_AGTS
    )
}