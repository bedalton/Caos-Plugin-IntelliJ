package com.badahori.creatures.plugins.intellij.agenteering.caos.lang


import bedalton.creatures.pray.cli.PrayCompilerCliOptions
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Cob
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Pray
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PRAY_COMPILER_SETTINGS_KEY
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PraySettingsPropertyPusher
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.stubs.PrayTagStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptExpandCommasIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptFileStub
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiTreeUtil.collectElementsOfType
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class CaosScriptFile constructor(viewProvider: FileViewProvider, private val myFile: VirtualFile) :
    PsiFileBase(viewProvider, CaosScriptLanguage), HasVariant, DumbAware {


    private val didFormatInitial = AtomicBoolean(false)
    override val variant: CaosVariant?
        get() {
            return explicitVariant.nullIfUnknown()
                ?: getUserData(ExplicitVariantUserDataKey).nullIfUnknown()
                ?: ExplicitVariantFilePropertyPusher.readFromStorage(myFile).nullIfUnknown()
                ?: implicitVariant.nullIfUnknown()
                ?: getUserData(ImplicitVariantUserDataKey).nullIfUnknown()
                ?: ImplicitVariantFilePropertyPusher.readFromStorage(myFile).nullIfUnknown()
                ?: myFile.cachedVariantExplicitOrImplicit.nullIfUnknown()
                ?: (this.originalFile as? CaosScriptFile)
                    ?.myFile
                    ?.cachedVariantExplicitOrImplicit
                    .nullIfUnknown()
                ?: (module ?: originalFile.module)?.variant.nullIfUnknown()
                ?: project.settings.defaultVariant.nullIfUnknown()
        }

    override fun setVariant(variant: CaosVariant?, explicit: Boolean) {
        setVariantBase(virtualFile, newVariant = variant, explicit)
        setVariantBase(myFile, variant, explicit)
        if (explicit) {
            explicitVariant = variant
            if (variant != this.variant)
                LOGGER.severe("Failed to set variant on PSI file. Expected: ${variant?.code}; Found: ${this.variant?.code}")
            ExplicitVariantFilePropertyPusher.readFromStorage(myFile).let {
                if (it != variant)
                    LOGGER.severe("Failed to set variant on virtual file. Expected: ${variant?.code}; Found: ${it?.code}")
            }
        } else {
            implicitVariant = variant
            ImplicitVariantFilePropertyPusher.readFromStorage(myFile).let {
                if (it != variant)
                    LOGGER.severe("Failed to set variant on virtual file. Expected: ${variant?.code}; Found: ${it?.code}")
            }
        }

        if (ApplicationManager.getApplication().isDispatchThread && this.isValid) {
            runWriteAction {
                if (virtualFile is CaosVirtualFile) {
                    invokeLater {
                        runWriteAction {
                            quickFormat()
                        }
                    }
                }
                DaemonCodeAnalyzer.getInstance(project).restart(this)
            }
        }
    }


    private var explicitVariant: CaosVariant? = null

    private var implicitVariant: CaosVariant? = null

    internal var lastInjector: GameInterfaceName?
        get() {
            val variant = variant
            return getUserData(INJECTOR_INTERFACE_USER_DATA_KEY)
                ?: InjectorInterfacePropertyPusher
                    .readFromStorage(project, myFile, variant)
                ?: InjectorInterfacePropertyPusher
                    .readFromStorage(project, virtualFile ?: myFile, variant)
                ?: module?.settings?.lastGameInterface(project)
                ?: project.settings.lastInterface(variant)
        }
        set(gameInterface) {
            putUserData(INJECTOR_INTERFACE_USER_DATA_KEY, gameInterface)
            InjectorInterfacePropertyPusher.writeToStorage(virtualFile ?: myFile, gameInterface)
            InjectorInterfacePropertyPusher.writeToStorage(myFile, gameInterface)
            if (gameInterface != null) {
                module?.settings?.lastGameInterface(gameInterface)
                variant.nullIfUnknown()?.let { variant ->
                    project.settings.lastInterface(variant, gameInterface)
                }
            }
        }

    private var mCliOptions: PrayCompilerCliOptions? = null
    var compilerSettings: PrayCompilerCliOptions?
        get() = mCliOptions ?: getUserData(PRAY_COMPILER_SETTINGS_KEY)
        set(value) {
            mCliOptions = value
            putUserData(PRAY_COMPILER_SETTINGS_KEY, value)
            PraySettingsPropertyPusher.writeToStorage(virtualFile ?: myFile, value)
            (virtualFile ?: myFile).putUserData(PRAY_COMPILER_SETTINGS_KEY, value)
        }

    val hasCaos2Tags: Boolean
        get() {
        val time = now
        this.getUserData(HAS_CAOS2_KEY)?.let { (expiry, hasCaos2) ->
            if (expiry > time) {
                return hasCaos2
            }
        }
        val hasCaos2 = calculateHasCaos2Tags()
        val expiry = time + rand(cacheMin, cacheMax) // cache for seconds
        this.putUserData(HAS_CAOS2_KEY, Pair(expiry, hasCaos2))
        return hasCaos2
//            return calculateHasCaos2Tags()
        }

    val prayTags: List<PrayTagStruct<*>>
        get() {
            if (!this.isValid || !this.isContentsLoaded)
                return emptyList()
            return stub?.prayTags
                ?: collectElementsOfType(
                    this,
                    CaosScriptCaos2Tag::class.java
                )
                    .mapNotNull map@{ tag ->
                        val tagName = tag.tagName
                        tag.valueAsInt?.let { number ->
                            PrayTagStruct.IntTag(
                                tagName,
                                number,
                                tag.startOffset
                            )
                        } ?: tag.valueAsString?.let { string ->
                            PrayTagStruct.StringTag(
                                tagName,
                                string,
                                tag.startOffset
                            )
                        }
                    }
        }

    override fun getFileType(): FileType {
        return CaosScriptFileType.INSTANCE
    }

    override fun getStub(): CaosScriptFileStub? {
        return super.getStub() as? CaosScriptFileStub
    }

    fun <PsiT : PsiElement> getChildOfType(childClass: Class<PsiT>): PsiT? =
        PsiTreeUtil.getChildOfType(this, childClass)


    fun <PsiT : PsiElement> getChildrenOfType(childClass: Class<PsiT>): List<PsiT> =
        PsiTreeUtil.getChildrenOfTypeAsList(this, childClass)

    override fun toString(): String {
        return "Caos Script"
    }

    override fun getName(): String {
        return myFile.name
    }

    fun quickFormat() {
        Companion.quickFormat(this)
    }

    companion object {
        @JvmStatic
        val ExplicitVariantUserDataKey =
            Key<CaosVariant?>("com.badahori.creatures.plugins.intellij.agenteering.caos.EXPLICIT_SCRIPT_VARIANT_KEY")

        @JvmStatic
        val ImplicitVariantUserDataKey =
            Key<CaosVariant?>("com.badahori.creatures.plugins.intellij.agenteering.caos.IMPLICIT_SCRIPT_VARIANT_KEY")

        fun quickFormat(caosFile: CaosScriptFile) {
            if (caosFile.project.isDisposed)
                return
            if (caosFile.didFormatInitial.getAndSet(true))
                return
            if (caosFile.virtualFile?.parent == null) {
                LOGGER.severe("Cannot QuickFormat CAOSScript file <${caosFile.virtualFile?.path}>. Parent is null")
                return
            }
            val project = caosFile.project
            val application = ApplicationManager.getApplication()
            when {
                application.isWriteAccessAllowed -> {
                    runQuickFormatInWriteAction(caosFile)
                }
                DumbService.isDumb(project) -> {
                    if (!application.isDispatchThread) {
                        invokeLater {
                            DumbService.getInstance(project).runWhenSmart {
                                if (project.isDisposed) {
                                    return@runWhenSmart
                                }
                                application.runWriteAction {
                                    runQuickFormatInWriteAction(caosFile)
                                }
                            }
                        }
                    } else {
                        DumbService.getInstance(project).runWhenSmart {
                            if (project.isDisposed) {
                                return@runWhenSmart
                            }
                            application.runWriteAction {
                                runQuickFormatInWriteAction(caosFile)
                            }
                        }
                    }
                }
                application.isDispatchThread -> {
                    application.runWriteAction(Computable {
                        runQuickFormatInWriteAction(caosFile)
                    })
                }
                else -> {
                    application.invokeLater {
                        application.runWriteAction {
                            runQuickFormatInWriteAction(caosFile)
                        }
                    }
                }
            }
        }

        private fun runQuickFormatInWriteAction(caosFile: CaosScriptFile) {
            CaosScriptExpandCommasIntentionAction.invoke(caosFile.project, caosFile)
        }
    }
}

val PsiFile.mModule: Module?
    get() {
        return virtualFile?.let { ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(it) }
    }

val PsiFile.module: Module?
    get()
    = this.mModule ?: containingFile?.originalFile?.mModule

val PsiElement.module: Module?
    get() = containingFile?.module ?: originalElement?.containingFile?.module

val RUN_INSPECTIONS_KEY = Key<Boolean?>("creatures.caos.RUN_INSPECTIONS")

var PsiFile.runInspections: Boolean
    get() {
        return getUserData(RUN_INSPECTIONS_KEY) ?: true
    }
    set(value) {
        putUserData(RUN_INSPECTIONS_KEY, value)
    }

val VirtualFile.cachedVariantExplicitOrImplicit: CaosVariant?
    get() = (this as? CaosVirtualFile)?.variant
        ?: ExplicitVariantFilePropertyPusher.readFromStorage(this)
        ?: ImplicitVariantFilePropertyPusher.readFromStorage(this)

fun VirtualFile.setCachedVariant(variant: CaosVariant?, explicit: Boolean) {
    (this as? CaosVirtualFile)?.setVariantBase(this, variant, explicit)
    if (explicit) {
        this.putUserData(CaosScriptFile.ExplicitVariantUserDataKey, variant)
        ExplicitVariantFilePropertyPusher.writeToStorage(this, variant ?: CaosVariant.UNKNOWN)
    } else {
        this.putUserData(CaosScriptFile.ImplicitVariantUserDataKey, variant)
        ImplicitVariantFilePropertyPusher.writeToStorage(this, variant ?: CaosVariant.UNKNOWN)
    }
}


val dumpRegex =
    "\\*+\\s*(Scriptorium|Dump|Scriptorium\\s*Dump).*".toRegex(RegexOption.IGNORE_CASE)

val PsiFile.isDump: Boolean
    get() {
        return collectElementsOfType(this, CaosScriptAtDirectiveComment::class.java)
            .any { dumpRegex.matches(it.text) }
    }

val CaosScriptFile.isCaos2Pray: Boolean
    get() = PsiTreeUtil.getChildOfType(
        this,
        CaosScriptCaos2Block::class.java
    )?.isCaos2Pray.orFalse()

val CaosScriptFile.caos2CobVariant: CaosVariant?
    get() = PsiTreeUtil.getChildOfType(
        this,
        CaosScriptCaos2Block::class.java
    )?.cobVariant


val CaosScriptFile.caos2Block: CaosScriptCaos2Block?
    get() = PsiTreeUtil.getChildOfType(
        this,
        CaosScriptCaos2Block::class.java
    )

val CaosScriptFile.caos2: String?
    get() {
        if (!this.isValid) {
            LOGGER.severe("Cannot calculate CAOS2 file is invalid")
            return null
        }

//        return  if (prayTags.any { PrayCommand.fromString(it.tag) == PrayCommand.PRAY_FILE })
//            CAOS2Pray
//        else if (prayTags.any { CobCommand.fromString(it.tag) == CobCommand.COBFILE })
//            CAOS2Cob
//        else {
//            PsiTreeUtil.getChildOfType(
//                this,
//                CaosScriptCaos2Block::class.java
//            )?.let {
//                if (it.isCaos2Pray)
//                    CAOS2Pray
//                else if (it.isCaos2Cob)
//                    CAOS2Cob
//                else
//                    null
//            }
//        }
        val time = now
        this.getUserData(CAOS2_KEY)?.let { (expiry, caos2) ->
            if (expiry > time) {
                return caos2
            }
        }
        val cacheForMillis = rand(cacheMin, cacheMax)
        val caos2 = calculateCaos2()
        this.putUserData(CAOS2_KEY, Pair(time + cacheForMillis, caos2))
        return caos2
//        return calculateCaos2()
    }


val CaosScriptFile.isCaos2Cob: Boolean
    get() = PsiTreeUtil.getChildOfType(
        this,
        CaosScriptCaos2Block::class.java
    )?.isCaos2Cob.orFalse()

val CaosScriptFile?.isMultiScript: Boolean
    get() {
        if (this == null) {
            return true
        }
        val time = now
        this.getUserData(IS_MULTI_SCRIPT)?.let { (expiry, isMultiScript) ->
            if (expiry > time) {
                return isMultiScript
            }
        }
        val cacheForMillis = rand(cacheMin, cacheMax)
        val isMultiScript = this.isDump || this.isCaos2Cob || this.isSupplement
        this.putUserData(IS_MULTI_SCRIPT, Pair(time + cacheForMillis, isMultiScript))
        return isMultiScript
    }

@Suppress("unused")
val CaosScriptFile?.agentNames
    get() = collectElementsOfType(this, CaosScriptCaos2Block::class.java)
        .flatMap { block -> block.agentBlockNames.map { it.second } }

private val CAOS2COB_VARIANT_REGEX = "^[*]{2}Caos2Cob\\s*(C1|C2)".toRegex(RegexOption.IGNORE_CASE)
private val CAOS2_BLOCK_VARIANT_REGEX =
    "^[*]#\\s*(C1|C2|CV|C3|DS|SM|[a-zA-Z][a-zA-Z0-9]{3}[ ])(-?Name)?".toRegex(
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.MULTILINE
        )
    )

fun getCaos2VariantRaw(text: CharSequence): CaosVariant? {
    val variant = text.trim().split('\n', limit = 2)[0].trim().let { firstLineText ->
        CAOS2COB_VARIANT_REGEX.matchEntire(firstLineText)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { variantCode ->
                CaosVariant.fromVal(variantCode)
            }
            ?.nullIfUnknown()
    }
    if (variant == null) {
        val variants = mutableListOf<CaosVariant>()
        CAOS2_BLOCK_VARIANT_REGEX
            .findAll(text)
            .iterator()
            .forEach { match ->
                match.groupValues.getOrNull(1)?.let { variantCode ->
                    when (variantCode) {
                        "AGNT" -> CaosVariant.C3
                        "DSAG" -> CaosVariant.DS
                        else -> {
                            CaosVariant.fromVal(variantCode).nullIfUnknown()?.let {
                                variants.add(it)
                            }
                        }
                    }
                }
            }
        return variants.minByOrNull { it.index }
    }
    return null
}

internal fun CaosScriptFile.getScripts(): Collection<CaosScriptScriptElement> {
    return collectElementsOfType(this, CaosScriptScriptElement::class.java)
}

@Suppress("unused")
internal inline fun <reified T : CaosScriptScriptElement> CaosScriptFile.getScriptsOfType(): List<CaosScriptScriptElement> {
    return collectElementsOfType(this, CaosScriptScriptElement::class.java)
        .filterIsInstance<T>()
}


private const val cacheMin = 3000
private const val cacheMax = 5000

// Cache KEYS
internal val HAS_CAOS2_KEY =
    Key<Pair<Long, Boolean>>("com.badahori.creatures.plugins.intellij.agenteering.caos.IS_CAOS2_CACHE_KEY")
internal val CAOS2_KEY =
    Key<Pair<Long, String?>>("com.badahori.creatures.plugins.intellij.agenteering.caos.CAOS2_CACHE_KEY")
internal val IS_SUPPLEMENT_KEY =
    Key<Pair<Long, Boolean>>("com.badahori.creatures.plugins.intellij.agenteering.caos.IS_SUPPLEMENT_KEY")
internal val IS_MULTI_SCRIPT =
    Key<Pair<Long, Boolean>>("com.badahori.creatures.plugins.intellij.agenteering.caos.IS_MULTI_SCRIPT")


// Header REGEX
private val CAOS2_HEADER_REGEX =
    "^[*]{2}\\s*CAOS2(PRAY|COB)".toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
private val CAOS2COB_REGEX =
    "^([*]#\\s*[Cc][12]\\s*-\\s*[Nn][Aa][Mm][Ee]\\s+[^\\n]+|[*]{2}[Cc][Aa][Oo][Ss]2[Cc][Oo][Bb])".toRegex(setOf(
        RegexOption.IGNORE_CASE,
        RegexOption.MULTILINE))
private val CAOS2PRAY_REGEX =
    "^([*]#\\s*([a-zA-Z0-9_!@#\$%&]{4}|[Dd][Ss]|[Cc]3)\\s*-\\s*[Nn][Aa][Mm][Ee]\\s+[^\\n \\t]+|[*]{2}[Cc][Aa][Oo][Ss]2[Pp][Rr][Aa][Yy])".toRegex(
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
private val IS_SUPPLEMENT_REGEX =
    "^[*]{2}\\s*(is\\s*)?(Supplement|link(ed))".toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

/**
 * Detect if this file has CAOS2 statements
 */
private fun CaosScriptFile.calculateHasCaos2Tags(): Boolean {
    val start = '*'.code
    val second = '#'.code
    val skip2 = ' '.code
    val skip1 = '\t'.code
    val end = '\n'.code

    var line = 0
    val maxLines = 20
    var inComment = false
    var inCaos2Comment = false
    var bufferByte: Int

    val text = text?.let {
        it.substring(0, min(4000, it.length))
    } ?: ""
    for (char in text) {
        bufferByte = char.code
        if (bufferByte < 0) {
            return false
        }
        if (bufferByte == end) {
            inComment = false
            inCaos2Comment = false
            if (line++ >= maxLines) {
                return false
            }
            continue
        }
        if (bufferByte == skip1 || bufferByte == skip2) {
            continue
        }
        if (inComment && bufferByte == second) {
            inCaos2Comment = true
            continue
        }
        if (bufferByte == start) {
            inComment = true
            continue
        }
        if (inCaos2Comment) {
            return true
        }
    }
    return false
}

/**
 * Determine the kind of CAOS2 file if any
 */
private fun CaosScriptFile.calculateCaos2(): String? {
    val text = this.text.nullIfEmpty()?.let {
        it.substring(0, min(4000, it.length - 1))
    } ?: return null
    CAOS2_HEADER_REGEX.find(text)
        ?.groupValues
        ?.getOrNull(1)
        ?.let {
            return if (it like "PRAY")
                CAOS2Pray
            else
                CAOS2Cob
        }
    if (CAOS2PRAY_REGEX.containsMatchIn(text)) {
        return CAOS2Pray
    }
    if (CAOS2COB_REGEX.containsMatchIn(text)) {
        return CAOS2Cob
    }
    return null
}

val CaosScriptFile?.isSupplement: Boolean
    get() {
        if (this == null) {
            return false
        }
        val time = now
        this.getUserData(IS_SUPPLEMENT_KEY)?.let { (expiry, isSupplement) ->
            if (expiry > time) {
                return isSupplement
            }
        }
        val isSupplement = calculateIsSupplement()
        val expiry = time + rand(cacheMin, cacheMax)
        this.putUserData(IS_SUPPLEMENT_KEY, Pair(expiry, isSupplement))
        return isSupplement
//        return calculateIsSupplement().apply {
//            LOGGER.info("isSupplement: $this")
//        }
    }

/**
 * Determine the kind of CAOS2 file if any
 */
private fun CaosScriptFile.calculateIsSupplement(): Boolean {
    val text = text?.let {
        it.substring(0, min(4000, it.length))
    } ?: ""
    return IS_SUPPLEMENT_REGEX.containsMatchIn(text)
}