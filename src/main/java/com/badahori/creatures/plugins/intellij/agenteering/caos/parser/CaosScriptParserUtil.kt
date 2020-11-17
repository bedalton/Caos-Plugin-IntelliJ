package com.badahori.creatures.plugins.intellij.agenteering.caos.parser

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.cachedVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommandType
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.isNumberType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.isStringType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets.Companion.ScriptTerminators
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets.Companion.WHITE_SPACE_LIKE_WITH_COMMENT
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.resolve.FileContextUtil
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.indexing.IndexingDataKeys
import gnu.trove.TObjectLongHashMap

@Suppress("UNUSED_PARAMETER", "unused")
object CaosScriptParserUtil : GeneratedParserUtilBase() {
    private val MODES_KEY = Key.create<TObjectLongHashMap<String>>("MODES_KEY")
    private val EXPECTATIONS_KEY = Key.create<MutableList<Int>>("com.badahori.caos.parser.EXPECTATIONS_KEY")
    private val BLOCKS_KEY = Key.create<Int>("com.badahori.caos.parser.BLOCKS")
    private val CAOS_VARIANT = Key.create<CaosVariant>("CAOS_VARIANT")
    private val lock = Object()
    const val NUMBER = 0
    const val STRING = 1
    const val OTHER = -1
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

    private val commandStrings by lazy {
        CaosLibs[CaosCommandType.COMMAND].map {
            it.command
        }
    }

    private val blockEnds = listOf<IElementType>(
            CaosScriptTypes.CaosScript_K_ENDM, CaosScriptTypes.CaosScript_K_NEXT, CaosScriptTypes.CaosScript_K_ELSE, CaosScriptTypes.CaosScript_K_ENDI, CaosScriptTypes.CaosScript_K_ELIF, CaosScriptTypes.CaosScript_K_REPE, CaosScriptTypes.CaosScript_K_NSCN, CaosScriptTypes.CaosScript_K_UNTL, CaosScriptTypes.CaosScript_K_EVER, CaosScriptTypes.CaosScript_K_RETN, CaosScriptTypes.CaosScript_K_SUBR
    )

    private fun getParsingModes(builder_: PsiBuilder): TObjectLongHashMap<String>? {
        var flags = builder_.getUserData(MODES_KEY)
        if (flags == null) {
            builder_.putUserData(MODES_KEY, TObjectLongHashMap<String>().also { flags = it })
        }
        return flags
    }

    @JvmStatic
    fun isVariant(builder_: PsiBuilder,
                  level: Int,
                  vararg variants: CaosVariant): Boolean {
        val fileVariant = fileVariant(builder_)
                ?: return false//CaosScriptProjectSettings.variant
        return fileVariant in variants
    }

    @JvmStatic
    fun isOldVariant(builder_: PsiBuilder,
                     level: Int): Boolean {
        val fileVariant = variant(builder_)
                ?: return false//CaosScriptProjectSettings.variant
        return fileVariant.isOld
    }

    @JvmStatic
    fun isNewVariant(
            builder_: PsiBuilder,
            level: Int,
            includeSeaMonkeys: Boolean
    ): Boolean {
        return fileVariant(builder_)?.let {variant ->
            variant.isNotOld && (includeSeaMonkeys || variant != CaosVariant.SM)
        } ?: false
    }

    private fun fileVariant(builder_: PsiBuilder): CaosVariant? {
        val psiFile = psiFile(builder_)
                ?: return null
        // Get CAOS file, and if it has variant, return it
        (psiFile as? CaosScriptFile ?: psiFile.originalFile as? CaosScriptFile)
                ?.variant
                ?.let { variant ->
                    return variant
                }

        (psiFile.virtualFile)?.let { virtualFile ->
            VariantFilePropertyPusher.readFromStorage(virtualFile)?.let { variant ->
                (psiFile as? CaosScriptFile)?.variant = variant
                return variant
            }
        }
        (psiFile.originalFile.virtualFile)?.let { virtualFile ->
            VariantFilePropertyPusher.readFromStorage(virtualFile)?.let { variant ->
                (psiFile as? CaosScriptFile)?.variant = variant
                return variant
            }
        }

        // PsiFile had not variant, find virtual file, and try to extract it
        val virtualFile = psiFile.virtualFile
                ?: psiFile.originalFile.virtualFile
                ?: psiFile.getUserData(IndexingDataKeys.VIRTUAL_FILE)
        (virtualFile as? CaosVirtualFile)?.let { caosVirtualFile ->
            return caosVirtualFile.variant
        }
        return psiFile.module?.variant
    }

    /**
     * Will not be CaosPsiFile
     */
    @JvmStatic
    fun psiFile(builder_: PsiBuilder): PsiFile? {
        return builder_.getUserData(FileContextUtil.CONTAINING_FILE_KEY)
    }

    @JvmStatic
    fun variant(builder_: PsiBuilder): CaosVariant? {
        val psiFile = psiFile(builder_)
        if (psiFile == null) {
            LOGGER.severe("CaosParser is parsing non-caos-script file")
            return null
        }
        (psiFile as? CaosScriptFile)?.variant?.let { variant ->
            return variant
        }
        val variant = psiFile.virtualFile?.cachedVariant
                ?: psiFile.originalFile.virtualFile?.cachedVariant
                ?: psiFile.getUserData(IndexingDataKeys.VIRTUAL_FILE)?.cachedVariant
                ?: psiFile.module?.variant
        if (variant == CaosVariant.UNKNOWN)
            return null
        return variant
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
    fun enterBlock(builder_: PsiBuilder,
                   level: Int): Boolean {
        builder_.putUserData(BLOCKS_KEY, builder_.getUserData(BLOCKS_KEY)?.let { it + 1 }.orElse(1))
        return true
    }

    @JvmStatic
    fun exitBlock(builder_: PsiBuilder,
                  level: Int): Boolean {
        builder_.putUserData(BLOCKS_KEY, builder_.getUserData(BLOCKS_KEY)?.let { if (it > 0) it - 1 else 0 }.orElse(0))
        return true
    }

    @JvmStatic
    fun insideBlock(builder_: PsiBuilder,
                    level: Int): Boolean {
        return builder_.getUserData(BLOCKS_KEY).orElse(0) > 0
    }

    private fun ignoreExpects(): Boolean {
        return false// && (CaosScriptProjectSettings.variant == C3 || CaosScriptProjectSettings.variant == DS)
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
    fun getVirtualFile(builder_: PsiBuilder): VirtualFile? {
        return psiFile(builder_)?.virtualFile
    }

    @JvmStatic
    fun eofNext(builder_: PsiBuilder,
                level: Int): Boolean {
        return builder_.lookAhead(1) == null || builder_.eof()
    }

    @JvmStatic
    fun pushParameter(
            builder_: PsiBuilder,
            level: Int,
            vararg parameters: Int
    ): Boolean {
        if (parameters.isEmpty())
            return true
        if (!pushPop(builder_))
            return true
        val expectations = builder_.getUserData(EXPECTATIONS_KEY)
                ?: mutableListOf()
        if (expectations.isEmpty())
            expectations.addAll(parameters.toList())
        else
            expectations.addAll(0, parameters.toList())
        builder_.putUserData(EXPECTATIONS_KEY, expectations)
        return true
    }

    @JvmStatic
    fun popParameter(
            builder_: PsiBuilder,
            level: Int
    ): Boolean {
        if (!pushPop(builder_))
            return true
        val expectations = builder_.getUserData(EXPECTATIONS_KEY)
                // Nullify on empty to simplify return without popping
                ?.nullIfEmpty()
                ?: return true
        expectations.removeAt(0)
        builder_.putUserData(EXPECTATIONS_KEY, expectations)
        return true
    }

    @JvmStatic
    fun needsString(
            builder_: PsiBuilder,
            level: Int
    ): Boolean {
        if (!pushPop(builder_)) {
            return true
        }
        return builder_.getUserData(EXPECTATIONS_KEY)?.firstOrNull()?.let {expectedType ->
            expectedType == STRING || expectedType == OTHER
        } ?: true
    }

    @JvmStatic
    fun needsInt(
            builder_: PsiBuilder,
            level: Int
    ): Boolean {
        if (!pushPop(builder_)) {
            return true
        }
        return builder_.getUserData(EXPECTATIONS_KEY)?.firstOrNull()?.let {expectedType ->
            expectedType == NUMBER || expectedType == OTHER
        } ?: true
    }

    private fun expectedTypeString(expectedType:Int?) : String  {
        return when(expectedType) {
            NUMBER -> "NUMBER"
            STRING -> "STRING"
            OTHER -> "OTHER"
            else -> "NULL"
        }

    }

    @JvmStatic
    fun tagParameters(
            builder_: PsiBuilder,
            level: Int,
            commandType:CaosCommandType
    ) : Boolean {
        // If push is not needed, (ie. not in variant CV), bail out
        if (!pushPop(builder_))
            return true
        // Try to get command text from previous token
        val text = builder_.latestDoneMarker?.let {
            val originalText = builder_.originalText
            originalText.substring(it.startOffset, it.endOffset).nullIfEmpty()
        }
        if (text == null) {
            // get the token before this one
            val prevPrevToken = prevToken(builder_, 1)
            val prevToken = prevToken(builder_)
            if (prevToken == null) {
                // No command could be inferred, so clear stack of expectations
                builder_.putUserData(EXPECTATIONS_KEY, mutableListOf())
                return true
            }
            if (prevPrevToken != null) {
                // Try to find a command match with this, plus prev prev
                CaosLibs[CaosVariant.CV][commandType]["$prevPrevToken $prevToken"]?.let {command ->
                    val parameters = command.parameters.map {parameter ->
                        when {
                            parameter.type.isNumberType -> NUMBER
                            parameter.type.isStringType -> STRING
                            else -> OTHER
                        }
                    }
                    val expectations = builder_.getUserData(EXPECTATIONS_KEY)
                            ?.nullIfEmpty()
                            ?.apply {
                                addAll(0, parameters)
                            }
                            ?: parameters.toMutableList()
                    builder_.putUserData(EXPECTATIONS_KEY, expectations)
                    return true
                }
            }
            // If previous two tokens do not match a command together
            // Try your luck with just one
            CaosLibs[CaosVariant.CV][commandType][prevToken]?.let { command ->
                val parameters = command.parameters.map { parameter ->
                    when {
                        parameter.type.isNumberType -> NUMBER
                        parameter.type.isStringType -> STRING
                        else -> OTHER
                    }
                }
                val expectations = builder_.getUserData(EXPECTATIONS_KEY)
                        ?.nullIfEmpty()
                        ?.apply {
                            addAll(0, parameters)
                        }
                        ?: parameters.toMutableList()
                builder_.putUserData(EXPECTATIONS_KEY, expectations)
            }
        }
        return true
    }

    @JvmStatic
    private fun pushPop(builder_: PsiBuilder): Boolean {
        return variant(builder_) == CaosVariant.CV
    }

    @JvmStatic
    fun eol(builder_: PsiBuilder, level: Int): Boolean {
        val text = builder_.tokenText
        return (text != null && text.contains("\n")) || eof(builder_, level)
    }

    @JvmStatic
    fun commandNext(builder_: PsiBuilder, level: Int): Boolean {
        val nextToken = nextToken(builder_)?.toUpperCase()
                ?: return false
        if (nextToken in commandStrings)
            return true
        val startsWith = commandStrings.filter { it.startsWith(nextToken) }
        if (startsWith.isEmpty())
            return false
        val nextNextToken = nextToken(builder_, 5)
                ?.toUpperCase()
                ?.let { nextNextToken ->
                    "$nextToken $nextNextToken"
                }
                ?: return false
        return nextNextToken in commandStrings
    }

    @JvmStatic
    fun isNext(
            builder_: PsiBuilder,
            level: Int,
            key: String
    ): Boolean {
        return nextToken(builder_) like key
    }

    @JvmStatic
    fun isNext(
            builder_: PsiBuilder,
            level: Int,
            vararg keys: String
    ): Boolean {
        return nextToken(builder_)?.let { nextToken ->
            keys.any { it like nextToken }
        } ?: false
    }

    private fun nextToken(builder_: PsiBuilder, offset: Int = 0): String? {
        val originalText = builder_.originalText
        var i = builder_.currentOffset + offset
        while (originalText.getOrNull(i)?.let { it == ' ' }.orFalse())
            i++
        if (i + 4 >= originalText.length) {
            return null
        }
        return originalText.substring(i until i + 4)
    }

    private fun prevToken(builder_: PsiBuilder, tokensOffset: Int = 0): String? {
        val originalText = builder_.originalText
        var i = builder_.currentOffset
        for (tokenIndex in 0..tokensOffset) {
            while (originalText.getOrNull(i)?.let { it == ' ' }.orFalse())
                i--
            if (i - 4 >= 0) {
                return null
            }
        }
        return originalText.substring(i - 4 until i)
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