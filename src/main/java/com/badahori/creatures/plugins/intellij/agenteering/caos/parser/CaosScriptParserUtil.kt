package com.badahori.creatures.plugins.intellij.agenteering.caos.parser

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile.Companion.ExplicitVariantUserDataKey
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile.Companion.ImplicitVariantUserDataKey
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.cachedVariantExplicitOrImplicit
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.getCaos2VariantRaw
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.lexer.CaosScriptTypes.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommandType
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.isNumberType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.isStringType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets.ScriptTerminators
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptTokenSets.WHITE_SPACE_LIKE_WITH_COMMENT
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.ExplicitVariantFilePropertyPusher
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.ImplicitVariantFilePropertyPusher
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.settings
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.impl.source.resolve.FileContextUtil
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.indexing.IndexingDataKeys
import gnu.trove.TObjectLongHashMap

@Suppress("UNUSED_PARAMETER", "unused")
object CaosScriptParserUtil : GeneratedParserUtilBase() {
    private val MODES_KEY = Key.create<TObjectLongHashMap<String>>("MODES_KEY")
    private val EXPECTATIONS_KEY = Key.create<MutableList<Int>>("creatures.caos.parser.EXPECTATIONS_KEY")
    private val EXPECTED_R_L_VALUES_KEY = Key.create<Int>("creatures.caos.parser.EXPECTED_R_L_VALUES_KEY")
    private val ENDED_ARGS_KEY = Key.create<Int>("creatures.caos.parser.ENDED_ARGS_KEY")
    private val BLOCKS_KEY = Key.create<Int>("creatures.caos.parser.BLOCKS")
    private val CAOS_VARIANT = Key.create<CaosVariant>("CAOS_VARIANT")
    private val CAOS_VARIANT_HEADER = Key.create<String>("CAOS_VARIANT_HEADER")
    private val CAOS2_COB_VARIANT = Key.create<CaosVariant>("CAOS2_COB_VARIANT")
    private val CAOS_2_COB_REGEX = "^\\*\\*\\s*CAOS2COB\\s*(C1|C2)".toRegex(RegexOption.IGNORE_CASE)
    private val CAOS_VARIANT_REGEX = "^[*]{2}\\s*VARIANT\\s*([a-zA-Z0-9]{2}[+]?)".toRegex(RegexOption.IGNORE_CASE)
    private val lock = Object()
    const val NUMBER = 0
    const val STRING = 1
    const val OTHER = -1
    const val WHITE_SPACE_OPTIONAL = "whiteSpaceOptional"
    val needsNoWhitespace = TokenSet.create(
        CaosScript_BYTE_STRING,
        CaosScript_OPEN_BRACKET,
        CaosScript_INT,
        CaosScript_FLOAT,
        CaosScript_QUOTE_STRING_LITERAL,
        CaosScript_CHAR_CHAR,
        CaosScript_CHARACTER
    )

    private val commandStrings by lazy {
        CaosLibs[CaosCommandType.COMMAND].map {
            it.command
        }
    }

    private val blockEnds = listOf<IElementType>(
        CaosScript_K_ENDM,
        CaosScript_K_NEXT,
        CaosScript_K_ELSE,
        CaosScript_K_ENDI,
        CaosScript_K_ELIF,
        CaosScript_K_REPE,
        CaosScript_K_NSCN,
        CaosScript_K_UNTL,
        CaosScript_K_EVER,
        CaosScript_K_RETN,
        CaosScript_K_SUBR
    )

    private fun getParsingModes(builder_: PsiBuilder): TObjectLongHashMap<String>? {
        var flags = builder_.getUserData(MODES_KEY)
        if (flags == null) {
            builder_.putUserData(MODES_KEY, TObjectLongHashMap<String>().also { flags = it })
        }
        return flags
    }

    @JvmStatic
    fun isVariant(
        builder_: PsiBuilder,
        level: Int,
        vararg variants: CaosVariant
    ): Boolean {
        val fileVariant = fileVariant(builder_)
            ?: return false//CaosScriptProjectSettings.variant
        return fileVariant in variants
    }

    @JvmStatic
    fun isOldVariant(
        builder_: PsiBuilder,
        level: Int
    ): Boolean {
        val fileVariant = fileVariant(builder_)
            ?: return false//CaosScriptProjectSettings.variant
        return fileVariant.isOld
    }

    @JvmStatic
    fun isNewVariant(
        builder_: PsiBuilder,
        level: Int,
    ): Boolean {
        return fileVariant(builder_)?.isNotOld ?: false
    }

    @JvmStatic
    fun isNewVariant(
        builder_: PsiBuilder,
        level: Int,
        includeSeaMonkeys: Boolean
    ): Boolean {
        return fileVariant(builder_)?.let { variant ->
            variant.isNotOld && (includeSeaMonkeys || variant != CaosVariant.SM)
        } ?: false
    }

    /*private fun fileVariant(builder_: PsiBuilder): CaosVariant? {
        val psiFile = psiFile(builder_)
            ?: return null
        // Get CAOS file, and if it has variant, return it
        (psiFile as? CaosScriptFile)?.getUserData(VariantUserDataKey).nullIfUnknown()?.let {
            return it
        }
        (psiFile.originalFile as? CaosScriptFile)?.getUserData(VariantUserDataKey).nullIfUnknown()?.let {
            return it
        }

        getCaos2VariantRaw(builder_.originalText).nullIfUnknown()?.let { variant ->
            psiFile.putUserData(VariantUserDataKey, variant)
            (psiFile as? CaosScriptFile)?.variant = variant
            (psiFile.originalFile as? CaosScriptFile)?.variant = variant
            return variant
        }

        (psiFile.virtualFile)?.cachedVariant.nullIfUnknown()?.let { variant ->
            return variant
        }
        (psiFile.originalFile.virtualFile)?.cachedVariant?.let { variant ->
            return variant
        }

        // PsiFile had not variant, find virtual file, and try to extract it
        val virtualFile = psiFile.virtualFile
            ?: psiFile.originalFile.virtualFile
            ?: psiFile.getUserData(IndexingDataKeys.VIRTUAL_FILE)
        (virtualFile as? CaosVirtualFile)?.let { caosVirtualFile ->
            return caosVirtualFile.variant
        }
        virtualFile?.cachedVariant.nullIfUnknown()?.let {
            return it
        }
        return (psiFile as? CaosScriptFile)?.variant ?: psiFile.module?.variant
    }*/

    @JvmStatic
    fun updateVariantFromCaos2Cob(builder_: PsiBuilder, level: Int): Boolean {
        getCaos2VariantRaw(builder_.originalText).nullIfUnknown()?.let { variant ->
            setVariant(builder_, variant, false)
        }
        return true
    }

    @JvmStatic
    fun updateVariantFromHeader(builder_: PsiBuilder, level: Int): Boolean {
        val text = builder_.originalText.trim()
        val tokenText = text.split("\n", limit = 2)[0].trim().removeSuffix(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED).nullIfEmpty()
        val cachedText = builder_.getUserData(CAOS_VARIANT_HEADER)
        if (tokenText == null || tokenText == cachedText)
            return true
        CAOS_VARIANT_REGEX.matchEntire(tokenText)?.groupValues?.getOrNull(1)?.let { variantCode ->
            builder_.putUserData(CAOS_VARIANT_HEADER, tokenText)
            val variant = CaosVariant.fromVal(variantCode)
            if (variant == CaosVariant.UNKNOWN)
                return false

            val psiFile = (psiFile(builder_) as? CaosScriptFile)
                ?: return false

            if (ApplicationManager.getApplication().isWriteAccessAllowed) {
                psiFile.setVariant(variant, true)

                if (psiFile.variant != variant) {
                    return false
                }
            }
            setVariant(builder_, variant, false)
        }
        return true
    }

    private fun setVariant(builder_: PsiBuilder, variant: CaosVariant, explicit: Boolean): Boolean {
        val psiFile = (psiFile(builder_) as? CaosScriptFile)
            ?: return false

        try {
            if (ApplicationManager.getApplication().isWriteAccessAllowed) {
                psiFile.setVariant(variant, explicit)
            }
            if (psiFile.variant != variant) {
                return false
            }
        } catch (e: Exception) {
            return false
        } catch (e: Error) {
            return false
        }

        psiFile.virtualFile?.let { virtualFile ->
            setVirtualFileVariant(virtualFile, variant, explicit)
        }

        psiFile.originalFile.virtualFile?.let { virtualFile ->
            setVirtualFileVariant(virtualFile, variant, explicit)
        }

        psiFile.putUserData(ImplicitVariantUserDataKey, variant)

        psiFile.getUserData(IndexingDataKeys.VIRTUAL_FILE)?.let { virtualFile ->
            setVirtualFileVariant(virtualFile, variant, explicit)
        }

        psiFile.getUserData(IndexingDataKeys.VIRTUAL_FILE)?.let { virtualFile ->
            setVirtualFileVariant(virtualFile, variant, explicit)
        }
        return true
    }

    private fun setVirtualFileVariant(virtualFile: VirtualFile, variant: CaosVariant, explicit: Boolean) {
        if (explicit)
            virtualFile.putUserData(ExplicitVariantUserDataKey, variant)
        else
            virtualFile.putUserData(ImplicitVariantUserDataKey, variant)
        if (virtualFile is CaosVirtualFile)
            virtualFile.setVariant(variant, explicit)
        else if (virtualFile is VirtualFileWithId) {
            if (explicit)
                ExplicitVariantFilePropertyPusher.writeToStorage(virtualFile, variant)
            else
               ImplicitVariantFilePropertyPusher.writeToStorage(virtualFile, variant)
        }
    }

    /**
     * Will not be CaosPsiFile
     */
    @JvmStatic
    fun psiFile(builder_: PsiBuilder): PsiFile? {
        return builder_.getUserData(FileContextUtil.CONTAINING_FILE_KEY)
    }

    @JvmStatic
    fun fileVariant(builder_: PsiBuilder): CaosVariant? {
        val psiFile = psiFile(builder_)
            ?: return builder_.project.settings.defaultVariant
        (psiFile as? CaosScriptFile)?.variant?.let { variant ->
            return variant
        }
        return (psiFile.virtualFile?.cachedVariantExplicitOrImplicit
            ?: psiFile.originalFile.virtualFile?.cachedVariantExplicitOrImplicit
            ?: psiFile.getUserData(IndexingDataKeys.VIRTUAL_FILE)?.cachedVariantExplicitOrImplicit
            ?: psiFile.module?.variant
            ?: psiFile.project.settings.defaultVariant)
            .nullIfUnknown()
            ?: CaosVariant.DS
    }

    @JvmStatic
    fun enterMode(
        builder_: PsiBuilder,
        level: Int, mode: String?
    ): Boolean {
        val flags = getParsingModes(builder_)
        if (!flags!!.increment(mode)) {
            flags.put(mode, 1)
        }
        return true
    }

    @JvmStatic
    fun enterBlock(
        builder_: PsiBuilder,
        level: Int
    ): Boolean {
        builder_.putUserData(BLOCKS_KEY, builder_.getUserData(BLOCKS_KEY)?.let { it + 1 }.orElse(1))
        return true
    }

    @JvmStatic
    fun exitBlock(
        builder_: PsiBuilder,
        level: Int
    ): Boolean {
        builder_.putUserData(BLOCKS_KEY, builder_.getUserData(BLOCKS_KEY)?.let { if (it > 0) it - 1 else 0 }.orElse(0))
        return true
    }

    @JvmStatic
    fun insideBlock(
        builder_: PsiBuilder,
        level: Int
    ): Boolean {
        return builder_.getUserData(BLOCKS_KEY).orElse(0) > 0
    }

    private fun ignoreExpects(): Boolean {
        return false// && (CaosScriptProjectSettings.variant == C3 || CaosScriptProjectSettings.variant == DS)
    }

    @JvmStatic
    fun exitMode(
        builder_: PsiBuilder,
        level: Int, mode: String
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
        level: Int, mode: String?
    ): Boolean {
        return getParsingModes(builder_)!![mode] > 0
    }

    @JvmStatic
    fun notInMode(
        builder_: PsiBuilder,
        level: Int, mode: String?
    ): Boolean {
        return getParsingModes(builder_)!![mode] == 0L
    }

    @JvmStatic
    fun getVirtualFile(builder_: PsiBuilder): VirtualFile? {
        return psiFile(builder_)?.virtualFile
    }

    @JvmStatic
    fun eofNext(
        builder_: PsiBuilder,
        level: Int
    ): Boolean {
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
        return builder_.getUserData(EXPECTATIONS_KEY)?.firstOrNull()?.let { expectedType ->
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
        return builder_.getUserData(EXPECTATIONS_KEY)?.firstOrNull()?.let { expectedType ->
            expectedType == NUMBER || expectedType == OTHER
        } ?: true
    }

    private fun expectedTypeString(expectedType: Int?): String {
        return when (expectedType) {
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
        commandType: CaosCommandType
    ): Boolean {
        // If push is not needed, (i.e. not in variant CV), bail out
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
                // Try to find a command match with this, plus prev.prev
                CaosLibs[CaosVariant.CV][commandType]["$prevPrevToken $prevToken"]?.let { command ->
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
        return fileVariant(builder_) == CaosVariant.CV
    }

    @JvmStatic
    fun eol(builder_: PsiBuilder, level: Int): Boolean {
        var lookAhead = 0
        var next = builder_.lookAhead(lookAhead++)
        val originalText = builder_.originalText
        var char: Char = 0.toChar()
        val textLength = originalText.length
        var couldBeSubroutine = false
        while (next != null && next == TokenType.WHITE_SPACE) {
            val startIndices = builder_.rawTokenTypeStart(lookAhead)
            val endIndex = builder_.rawTokenTypeStart(lookAhead)
            for (charIndex in startIndices until endIndex) {
                if (charIndex < 0 || charIndex == textLength)
                    break
                char = originalText[charIndex]
                if (charIndex == startIndices && (char == 's' || char == 'g')) {
                    couldBeSubroutine = next == CaosScript_K_SUBR || next == CaosScript_K_GSUB
                }
                if (char == '\n' || char == ',')
                    break
                if (char != ' ' && char != '\t')
                    break
            }
            next = builder_.lookAhead(++lookAhead)
        }

        if (next == CaosScript_GHOST_QUOTE) {
            return true
        }

        if (next == TokenType.BAD_CHARACTER)
            return true

        if (next == CaosScript_ERROR_WORD) {
            return !couldBeSubroutine
        }

        if (char == '\n' || char == ',' || eof(builder_, level)) {
            builder_.putUserData(ENDED_ARGS_KEY, 0)
            builder_.putUserData(EXPECTED_R_L_VALUES_KEY, 0)
            return true
        }
        return next in ALL_COMMAND_STARTS
    }

    @JvmStatic
    fun endOfArgs(builder_: PsiBuilder, level: Int): Boolean {

        return true
    }


    @JvmStatic
    fun nextIsCommand(builder_: PsiBuilder, level: Int): Boolean {
        return false
    }

    @JvmStatic
    fun commandNext(builder_: PsiBuilder, level: Int): Boolean {
        val nextToken = nextToken(builder_)?.uppercase()
            ?: return false
        if (nextToken in commandStrings)
            return true
        val startsWith = commandStrings.filter { it.startsWith(nextToken) }
        if (startsWith.isEmpty())
            return false
        val nextNextToken = nextToken(builder_, 5)
            ?.uppercase()
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
            CaosScript_K_HIST -> return builder_.lookAhead(1) in histStrings
            CaosScript_K_PRT_COL -> builder_.lookAhead(1) == CaosScript_K_NAME
            CaosScript_K_PRAY -> builder_.lookAhead(1) in prayStrings
            else -> return false
        }
    }

    private val COMMAND_ENDINGS = listOf(
        CaosScript_K_DOIF,
        CaosScript_K_ELIF,
        CaosScript_K_ELSE,
        CaosScript_K_ENDM,
        CaosScript_K_ENUM,
        CaosScript_K_NEXT,
        CaosScript_K_EPAS,
        CaosScript_K_ESEE,
        CaosScript_K_ECON,
        CaosScript_K_SCRP,
        CaosScript_K_ENDM,
        CaosScript_K_ISCR,
        CaosScript_K_RSCR,
        CaosScript_K_NEW_COL,
        CaosScript_K_SLIM,
        CaosScript_K_ADDV,
        CaosScript_K_SUBV,
        CaosScript_K_MULV,
        CaosScript_K_DIVV,
        CaosScript_K_MODV,
        CaosScript_K_NEGV,
        CaosScript_K_ANDV,
        CaosScript_K_RNDV,
        CaosScript_K_SETV,
        CaosScript_K_BBLE,
        CaosScript_K_STOP,
        CaosScript_K_ENDM,
        CaosScript_K_SUBR,
        CaosScript_K_GSUB,
        CaosScript_K_RETN,
        CaosScript_K_CRETN,
        CaosScript_K_REPS,
        CaosScript_K_REPE,
        CaosScript_K_LOOP,
        CaosScript_K_UNTL,
        CaosScript_K_ENUM,
        CaosScript_K_ESEE,
        CaosScript_K_ETCH,
        CaosScript_K_ESCN,
        CaosScript_K_NSCN,
        CaosScript_K_RTAR,
        CaosScript_K_STAR,
        CaosScript_K_INST,
        CaosScript_K_SLOW,
        CaosScript_K_EVER,
        CaosScript_K_DOIF,
        CaosScript_K_ELSE,
        CaosScript_K_ENDI,
        CaosScript_K_WAIT,
        CaosScript_K_ANIM,
        CaosScript_K_OVER,
        CaosScript_K_PRLD,
        CaosScript_K_MVTO,
        CaosScript_K_MCRT,
        CaosScript_K_MVBY,
        CaosScript_K_MESG,
        CaosScript_K_STM_NUM,
        CaosScript_K_STIM,
        CaosScript_K_DELR,
        CaosScript_K_DELN,
        CaosScript_K_TECO,
        CaosScript_K_ASEA,
        CaosScript_K_SPOT,
        CaosScript_K_KNOB,
        CaosScript_K_KMSG,
        CaosScript_K_CABN,
        CaosScript_K_DPS2,
        CaosScript_K_DPAS,
        CaosScript_K_GPAS,
        CaosScript_K_SPAS,
        CaosScript_K_TELE,
        CaosScript_K_BBD_COL,
        CaosScript_K_BBTX,
        CaosScript_K_BBT2,
        CaosScript_K_BBFD,
        CaosScript_K_CBRG,
        CaosScript_K_CBRX,
        CaosScript_K_SYS_COL,
        CaosScript_K_RCLR,
        CaosScript_K_SNDE,
        CaosScript_K_SNDQ,
        CaosScript_K_SNDC,
        CaosScript_K_SNDL,
        CaosScript_K_PLDS,
        CaosScript_K_STPC,
        CaosScript_K_FADE,
        CaosScript_K_INJR,
        CaosScript_K_FIRE,
        CaosScript_K_TRIG,
        CaosScript_K_APPR,
        CaosScript_K_WALK,
        CaosScript_K_POIN,
        CaosScript_K_AIM_COL,
        CaosScript_K_SAY_NUM,
        CaosScript_K_SAY_DOL,
        CaosScript_K_SAYN,
        CaosScript_K_IMPT,
        CaosScript_K_DONE,
        CaosScript_K_LTCY,
        CaosScript_K_DROP,
        CaosScript_K_MATE,
        CaosScript_K_SNEZ,
        CaosScript_K_DDE_COL,
        CaosScript_K_DBUG,
        CaosScript_K_DBGV,
        CaosScript_K_DBGM,
        CaosScript_K_SCRX,
        CaosScript_K_LOCK,
        CaosScript_K_UNLK,
        CaosScript_K_EVNT,
        CaosScript_K_RMEV,
        CaosScript_K_ISCR,
        CaosScript_K_RSCR,
        CaosScript_K_ORRV,
        CaosScript_K_TOOL,
        CaosScript_K_SNDF,
        CaosScript_K_SNDV,
        CaosScript_K_ALPH,
        CaosScript_K_ANMS,
        CaosScript_K_BMPS,
        CaosScript_K_FRAT,
        CaosScript_K_GAIT,
        CaosScript_K_IMGE,
        CaosScript_K_NOHH,
        CaosScript_K_SCLE,
        CaosScript_K_TTAR,
        CaosScript_K_BRMI,
        CaosScript_K_CMRA,
        CaosScript_K_CMRP,
        CaosScript_K_CMRT,
        CaosScript_K_FRSH,
        CaosScript_K_MIRR,
        CaosScript_K_PRNT,
        CaosScript_K_SCAM,
        CaosScript_K_SCRL,
        CaosScript_K_SNAP,
        CaosScript_K_TNTO,
        CaosScript_K_ZOOM,
        CaosScript_K_FCUS,
        CaosScript_K_FRMT,
        CaosScript_K_GRPL,
        CaosScript_K_GRPV,
        CaosScript_K_AGES,
        CaosScript_K_BORN,
        CaosScript_K_DYED,
        CaosScript_K_FORF,
        CaosScript_K_HAIR,
        CaosScript_K_LIKE,
        CaosScript_K_MVFT,
        CaosScript_K_NEWC,
        CaosScript_K_NUDE,
        CaosScript_K_RSET,
        CaosScript_K_SPNL,
        CaosScript_K_STRE,
        CaosScript_K_SWAP,
        CaosScript_K_TNTC,
        CaosScript_K_VOCB,
        CaosScript_K_APRO,
        CaosScript_K_HELP,
        CaosScript_K_MANN,
        CaosScript_K_MEMX,
        CaosScript_K_OUTS,
        CaosScript_K_OUTV,
        CaosScript_K_OUTX,
        CaosScript_K_ELIF,
        CaosScript_K_GOTO,
        CaosScript_K_MOUS,
        CaosScript_K_ADDB,
        CaosScript_K_ALTR,
        CaosScript_K_CACL,
        CaosScript_K_DELM,
        CaosScript_K_DMAP,
        CaosScript_K_DOCA,
        CaosScript_K_EMIT,
        CaosScript_K_MAPD,
        CaosScript_K_MAPK,
        CaosScript_K_FLTO,
        CaosScript_K_FREL,
        CaosScript_K_MVSF,
        CaosScript_K_VELO,
        CaosScript_K_ECON,
        CaosScript_K_STPT,
        CaosScript_K_MCLR,
        CaosScript_K_MIDI,
        CaosScript_K_SEZZ,
        CaosScript_K_STRK,
        CaosScript_K_VOIC,
        CaosScript_K_ABSV,
        CaosScript_K_ADDS,
        CaosScript_K_DELG,
        CaosScript_K_REAF,
        CaosScript_K_SETA,
        CaosScript_K_SETS,
        CaosScript_K_CABW,
        CaosScript_K_EPAS,
        CaosScript_K_RPAS,
        CaosScript_K_DELW,
        CaosScript_K_LOAD,
        CaosScript_K_QUIT,
        CaosScript_K_RGAM,
        CaosScript_K_SAVE,
        CaosScript_K_TNTW,
        CaosScript_K_WTNT,
        CaosScript_K_CALL,
        CaosScript_K_CATO,
        CaosScript_K_CORE,
        CaosScript_K_DCOR,
        CaosScript_K_DSEE,
        CaosScript_K_TINO,
        CaosScript_K_UCLN,
        CaosScript_K_ADIN,
        CaosScript_K_BRN_COL,
        CaosScript_K_DOIN,
        CaosScript_K_PAT_COL,
        CaosScript_K_BOOT,
        CaosScript_K_ORDR,
        CaosScript_K_PLMD,
        CaosScript_K_PLMU,
        CaosScript_K_STEP,
        CaosScript_K_SWAY,
        CaosScript_K_URGE,
        CaosScript_K_BANG,
        CaosScript_K_DBG_COL,
        CaosScript_K_WEBB,
        CaosScript_K_GENE,
        CaosScript_K_CALC,
        CaosScript_K_ROTN,
        CaosScript_K_GIDS,
        CaosScript_K_JECT,
        CaosScript_K_DELE,
        CaosScript_K_NAMN,
        CaosScript_K_NOTV,
        CaosScript_K_SCRP,
        CaosScript_K_F__K,
        CaosScript_K_APP_COL,
        CaosScript_K_SSFC,
        CaosScript_K_BLCK,
        CaosScript_K_OUTL,
        CaosScript_K_SHAD,
        CaosScript_K_STRC,
        CaosScript_EQ_OP_OLD_,
        CaosScript_EQ_OP_NEW_,
        CaosScript_K_AND,
        CaosScript_K_OR
    )

    private val ALL_COMMAND_STARTS = listOf(
        CaosScript_K_NEW_COL,
        CaosScript_K_TARG,
        CaosScript_K_EDIT,
        CaosScript_K_BHVR,
        CaosScript_K_KILL,
        CaosScript_K_TICK,
        CaosScript_K_SLIM,
        CaosScript_K_ADDV,
        CaosScript_K_SUBV,
        CaosScript_K_MULV,
        CaosScript_K_DIVV,
        CaosScript_K_MODV,
        CaosScript_K_NEGV,
        CaosScript_K_ANDV,
        CaosScript_K_RNDV,
        CaosScript_K_SETV,
        CaosScript_K_BBLE,
        CaosScript_K_STOP,
        CaosScript_K_ENDM,
        CaosScript_K_SUBR,
        CaosScript_K_GSUB,
        CaosScript_K_RETN,
        CaosScript_K_CRETN,
        CaosScript_K_REPS,
        CaosScript_K_REPE,
        CaosScript_K_LOOP,
        CaosScript_K_UNTL,
        CaosScript_K_ENUM,
        CaosScript_K_ESEE,
        CaosScript_K_ETCH,
        CaosScript_K_NEXT,
        CaosScript_K_ESCN,
        CaosScript_K_NSCN,
        CaosScript_K_RTAR,
        CaosScript_K_STAR,
        CaosScript_K_INST,
        CaosScript_K_SLOW,
        CaosScript_K_EVER,
        CaosScript_K_DOIF,
        CaosScript_K_ELSE,
        CaosScript_K_ENDI,
        CaosScript_K_WAIT,
        CaosScript_K_ANIM,
        CaosScript_K_OVER,
        CaosScript_K_POSE,
        CaosScript_K_PRLD,
        CaosScript_K_BASE,
        CaosScript_K_MVTO,
        CaosScript_K_MCRT,
        CaosScript_K_MVBY,
        CaosScript_K_MESG,
        CaosScript_K_STM_NUM,
        CaosScript_K_STIM,
        CaosScript_K_ROOM,
        CaosScript_K_DELR,
        CaosScript_K_DELN,
        CaosScript_K_TECO,
        CaosScript_K_ASEA,
        CaosScript_K_SPOT,
        CaosScript_K_KNOB,
        CaosScript_K_KMSG,
        CaosScript_K_PART,
        CaosScript_K_CABN,
        CaosScript_K_DPS2,
        CaosScript_K_DPAS,
        CaosScript_K_GPAS,
        CaosScript_K_SPAS,
        CaosScript_K_TELE,
        CaosScript_K_BBD_COL,
        CaosScript_K_BBTX,
        CaosScript_K_BBT2,
        CaosScript_K_BBFD,
        CaosScript_K_CBRG,
        CaosScript_K_CBRX,
        CaosScript_K_RAIN,
        CaosScript_K_SYS_COL,
        CaosScript_K_RMSC,
        CaosScript_K_RCLR,
        CaosScript_K_SNDE,
        CaosScript_K_SNDQ,
        CaosScript_K_SNDC,
        CaosScript_K_SNDL,
        CaosScript_K_PLDS,
        CaosScript_K_STPC,
        CaosScript_K_FADE,
        CaosScript_K_CHEM,
        CaosScript_K_ASLP,
        CaosScript_K_INJR,
        CaosScript_K_FIRE,
        CaosScript_K_TRIG,
        CaosScript_K_APPR,
        CaosScript_K_WALK,
        CaosScript_K_TOUC,
        CaosScript_K_POIN,
        CaosScript_K_AIM_COL,
        CaosScript_K_SAY_NUM,
        CaosScript_K_SAY_DOL,
        CaosScript_K_SAYN,
        CaosScript_K_IMPT,
        CaosScript_K_DONE,
        CaosScript_K_LTCY,
        CaosScript_K_DREA,
        CaosScript_K_DROP,
        CaosScript_K_MATE,
        CaosScript_K_SNEZ,
        CaosScript_K_DDE_COL,
        CaosScript_K_DBUG,
        CaosScript_K_DBGV,
        CaosScript_K_DBGM,
        CaosScript_K_SCRX,
        CaosScript_K_LOCK,
        CaosScript_K_UNLK,
        CaosScript_K_EVNT,
        CaosScript_K_RMEV,
        CaosScript_K_VRSN,
        CaosScript_K_ISCR,
        CaosScript_K_RSCR,
        CaosScript_K_ORRV,
        CaosScript_K_TOOL,
        CaosScript_K_SNDF,
        CaosScript_K_SNDV,
        CaosScript_K_ALPH,
        CaosScript_K_ANMS,
        CaosScript_K_ATTR,
        CaosScript_K_BMPS,
        CaosScript_K_FRAT,
        CaosScript_K_GAIT,
        CaosScript_K_GALL,
        CaosScript_K_HAND,
        CaosScript_K_IMGE,
        CaosScript_K_MIRA,
        CaosScript_K_NOHH,
        CaosScript_K_PAUS,
        CaosScript_K_PLNE,
        CaosScript_K_PUHL,
        CaosScript_K_PUPT,
        CaosScript_K_RNGE,
        CaosScript_K_SCLE,
        CaosScript_K_SHOW,
        CaosScript_K_TINT,
        CaosScript_K_TTAR,
        CaosScript_K_BKGD,
        CaosScript_K_BRMI,
        CaosScript_K_CMRA,
        CaosScript_K_CMRP,
        CaosScript_K_CMRT,
        CaosScript_K_FRSH,
        CaosScript_K_LINE,
        CaosScript_K_META,
        CaosScript_K_MIRR,
        CaosScript_K_PRNT,
        CaosScript_K_SCAM,
        CaosScript_K_SCRL,
        CaosScript_K_SNAP,
        CaosScript_K_TNTO,
        CaosScript_K_TRCK,
        CaosScript_K_WDOW,
        CaosScript_K_ZOOM,
        CaosScript_K_FCUS,
        CaosScript_K_FRMT,
        CaosScript_K_GRPL,
        CaosScript_K_GRPV,
        CaosScript_K_PAGE,
        CaosScript_K_PTXT,
        CaosScript_K_AGES,
        CaosScript_K_BODY,
        CaosScript_K_BORN,
        CaosScript_K_DEAD,
        CaosScript_K_DIRN,
        CaosScript_K_DRIV,
        CaosScript_K_DYED,
        CaosScript_K_EXPR,
        CaosScript_K_FACE,
        CaosScript_K_FORF,
        CaosScript_K_HAIR,
        CaosScript_K_LIKE,
        CaosScript_K_LOCI,
        CaosScript_K_MVFT,
        CaosScript_K_NEWC,
        CaosScript_K_NORN,
        CaosScript_K_NUDE,
        CaosScript_K_RSET,
        CaosScript_K_SPNL,
        CaosScript_K_STRE,
        CaosScript_K_SWAP,
        CaosScript_K_TNTC,
        CaosScript_K_UNCS,
        CaosScript_K_VOCB,
        CaosScript_K_WEAR,
        CaosScript_K_ZOMB,
        CaosScript_K_APRO,
        CaosScript_K_HELP,
        CaosScript_K_MANN,
        CaosScript_K_MEMX,
        CaosScript_K_OUTS,
        CaosScript_K_OUTV,
        CaosScript_K_OUTX,
        CaosScript_K_ELIF,
        CaosScript_K_GOTO,
        CaosScript_K_CLAC,
        CaosScript_K_CLIK,
        CaosScript_K_IMSK,
        CaosScript_K_MOUS,
        CaosScript_K_PURE,
        CaosScript_K_TRAN,
        CaosScript_K_ADDB,
        CaosScript_K_ALTR,
        CaosScript_K_CACL,
        CaosScript_K_DELM,
        CaosScript_K_DMAP,
        CaosScript_K_DOCA,
        CaosScript_K_DOOR,
        CaosScript_K_EMIT,
        CaosScript_K_LINK,
        CaosScript_K_MAPD,
        CaosScript_K_MAPK,
        CaosScript_K_PERM,
        CaosScript_K_PROP,
        CaosScript_K_RATE,
        CaosScript_K_RTYP,
        CaosScript_K_ACCG,
        CaosScript_K_AERO,
        CaosScript_K_ELAS,
        CaosScript_K_FLTO,
        CaosScript_K_FREL,
        CaosScript_K_FRIC,
        CaosScript_K_MVSF,
        CaosScript_K_VELO,
        CaosScript_K_ECON,
        CaosScript_K_STPT,
        CaosScript_K_MCLR,
        CaosScript_K_MIDI,
        CaosScript_K_MMSC,
        CaosScript_K_SEZZ,
        CaosScript_K_STRK,
        CaosScript_K_VOIC,
        CaosScript_K_VOIS,
        CaosScript_K_VOLM,
        CaosScript_K_WPAU,
        CaosScript_K_ABSV,
        CaosScript_K_ADDS,
        CaosScript_K_CHAR,
        CaosScript_K_DELG,
        CaosScript_K_REAF,
        CaosScript_K_SETA,
        CaosScript_K_SETS,
        CaosScript_K_CABP,
        CaosScript_K_CABV,
        CaosScript_K_CABW,
        CaosScript_K_EPAS,
        CaosScript_K_RPAS,
        CaosScript_K_DELW,
        CaosScript_K_LOAD,
        CaosScript_K_PSWD,
        CaosScript_K_QUIT,
        CaosScript_K_RGAM,
        CaosScript_K_SAVE,
        CaosScript_K_TNTW,
        CaosScript_K_WRLD,
        CaosScript_K_WTNT,
        CaosScript_K_CALL,
        CaosScript_K_CATO,
        CaosScript_K_CORE,
        CaosScript_K_DCOR,
        CaosScript_K_DSEE,
        CaosScript_K_TINO,
        CaosScript_K_UCLN,
        CaosScript_K_ADIN,
        CaosScript_K_BRN_COL,
        CaosScript_K_DOIN,
        CaosScript_K__CD_,
        CaosScript_K_PAT_COL,
        CaosScript_K_BOOT,
        CaosScript_K_CALG,
        CaosScript_K_MIND,
        CaosScript_K_MOTR,
        CaosScript_K_ORDR,
        CaosScript_K_PLMD,
        CaosScript_K_PLMU,
        CaosScript_K_SOUL,
        CaosScript_K_STEP,
        CaosScript_K_SWAY,
        CaosScript_K_URGE,
        CaosScript_K_BANG,
        CaosScript_K_DBG_COL,
        CaosScript_K_FILE,
        CaosScript_K_WEBB,
        CaosScript_K_GENE,
        CaosScript_K_HIST,
        CaosScript_K_CALC,
        CaosScript_K_ADMP,
        CaosScript_K_AVEL,
        CaosScript_K_FDMP,
        CaosScript_K_FVEL,
        CaosScript_K_ROTN,
        CaosScript_K_SDMP,
        CaosScript_K_SPIN,
        CaosScript_K_SVEL,
        CaosScript_K_VARC,
        CaosScript_K_NET_COL,
        CaosScript_K_PRT_COL,
        CaosScript_K_PRAY,
        CaosScript_K_GIDS,
        CaosScript_K_JECT,
        CaosScript_K_BUZZ,
        CaosScript_K_DELE,
        CaosScript_K_NAMN,
        CaosScript_K_NOTV,
        CaosScript_K_SCRP,
        CaosScript_K_F__K,
        CaosScript_K_EXEC,
        CaosScript_K_APP_COL,
        CaosScript_K_SSFC,
        CaosScript_K_BLCK,
        CaosScript_K_FLIP,
        CaosScript_K_OUTL,
        CaosScript_K_ROTA,
        CaosScript_K_SHAD,
        CaosScript_K_STRC,
        CaosScript_K_OR,
        CaosScript_K_AND,
        CaosScript_EQ_OP_NEW_,
        CaosScript_EQ_OP_OLD_
    )

    private val nextIsString: List<IElementType> = listOf(
        CaosScript_K_CATX,
        CaosScript_K_WILD,
        CaosScript_K_BKGD,
        CaosScript_K_TRAN,
        CaosScript_K_DBG_NUM,
        CaosScript_K_DBGA,
        CaosScript_K_FVWM,
        CaosScript_K_GTOS,
        CaosScript_K_BKDS,
        CaosScript_K_ERID,
        CaosScript_K_MLOC,
        CaosScript_K_RATE,
        CaosScript_K_RLOC,
        CaosScript_K_CAOS,
        CaosScript_K_SORC,
        CaosScript_K_MMSC,
        CaosScript_K_RMSC,
        CaosScript_K_RTIF,
        CaosScript_K_GAMN,
        CaosScript_K_READ,
        CaosScript_K_SUBS,
        CaosScript_K_VTOS,
        CaosScript_K_PSWD,
        CaosScript_K_WRLD
    )
    private val histStrings: List<IElementType> = listOf(
        CaosScript_K_FOTO,
        CaosScript_K_NAME,
        CaosScript_K_NEXT,
        CaosScript_K_PREV,
        CaosScript_K_UTXT,
        CaosScript_K_WNAM,
        CaosScript_K_WUID,
        CaosScript_K_MON1,
        CaosScript_K_MON2
    )
    private val prayStrings: List<IElementType> = listOf(
        CaosScript_K_PREV,
        CaosScript_K_NEXT,
        CaosScript_K_AGTS
    )
}