package com.badahori.creatures.plugins.intellij.agenteering.caos.lang

import bedalton.creatures.pray.compiler.cli.PrayCliOptions
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
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptFileStub
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiTreeUtil.collectElementsOfType
import com.intellij.util.FileContentUtilCore
import java.util.concurrent.atomic.AtomicBoolean

class CaosScriptFile constructor(viewProvider: FileViewProvider, private val myFile: VirtualFile) :
    PsiFileBase(viewProvider, CaosScriptLanguage), HasVariant {


    private val didFormatInitial = AtomicBoolean(false)
    override var variant: CaosVariant?
        get() {
            return (variantOverride
                ?: getUserData(VariantUserDataKey)
                ?: myFile.cachedVariant ?: (this.originalFile as? CaosScriptFile)?.myFile?.cachedVariant
                ?: (module ?: originalFile.module)?.variant
                ?: project.settings.defaultVariant)
                .nullIfUnknown()
        }
        set(newVariant) {
            variantOverride = newVariant
            putUserData(VariantUserDataKey, newVariant)
            this.myFile.let { virtualFile ->
                (virtualFile as? CaosVirtualFile)?.variant = newVariant
                if (virtualFile is VirtualFileWithId) {
                    VariantFilePropertyPusher.writeToStorage(virtualFile, newVariant ?: CaosVariant.UNKNOWN)
                }
                virtualFile.putUserData(VariantUserDataKey, newVariant)
                if (ApplicationManager.getApplication().isDispatchThread) {
                    runWriteAction {
                        try {
                            if (virtualFile.parent != null)
                                FileContentUtilCore.reparseFiles(virtualFile)
                        } catch (e: Exception) {
                            LOGGER.severe("Failed to reparse file. Error: ${e.className}(${e.message})")
                        }
                    }
                }
            }
            if (ApplicationManager.getApplication().isDispatchThread && this.isValid) {
                runWriteAction {
                    DaemonCodeAnalyzer.getInstance(project).restart(this)
                }
            }
        }

    private var variantOverride: CaosVariant? = null

    internal var lastInjector: GameInterfaceName?
        get() = getUserData(INJECTOR_INTERFACE_USER_DATA_KEY)
            ?: InjectorInterfacePropertyPusher
                .readFromStorage(project, myFile)
            ?: module?.settings?.lastGameInterface(project)
            ?: project.settings.lastInterface(variant)
        set(gameInterface) {
            LOGGER.info("Setting CAOS injector to ${gameInterface?.name}")
            if (gameInterface == null)
                return
            putUserData(INJECTOR_INTERFACE_USER_DATA_KEY, gameInterface)
            InjectorInterfacePropertyPusher.writeToStorage(myFile, gameInterface)
            module?.settings?.lastGameInterface(gameInterface)
            variant.nullIfUnknown()?.let { variant ->
                project.settings.lastInterface(variant, gameInterface)
            }
        }

    private var mCliOptions: PrayCliOptions? = null
    var compilerSettings: PrayCliOptions?
        get() = mCliOptions ?: getUserData(PRAY_COMPILER_SETTINGS_KEY)
        set(value) {
            mCliOptions = value
            putUserData(PRAY_COMPILER_SETTINGS_KEY, value)
            PraySettingsPropertyPusher.writeToStorage(virtualFile, value)
            virtualFile.putUserData(PRAY_COMPILER_SETTINGS_KEY, value)
        }

    val prayTags: List<PrayTagStruct<*>>
        get() {
            return stub?.prayTags ?: collectElementsOfType(
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
        val VariantUserDataKey =
            Key<CaosVariant?>("com.badahori.creatures.plugins.intellij.agenteering.caos.SCRIPT_VARIANT_KEY")

        fun quickFormat(caosFile: CaosScriptFile) {
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
                        DumbService.getInstance(project).runWhenSmart {
                            application.runWriteAction {
                                runQuickFormatInWriteAction(caosFile)
                            }
                        }
                    } else {
                        DumbService.getInstance(project).runWhenSmart {
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
            if (caosFile.virtualFile?.apply { isWritable = true }?.isWritable.orFalse())
                CaosScriptExpandCommasIntentionAction.invoke(caosFile.project, caosFile)
        }
    }
}

val PsiFile.module: Module?
    get() {
        return virtualFile?.let { ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(it) }
    }

val RUN_INSPECTIONS_KEY = Key<Boolean?>("creatures.caos.RUN_INSPECTIONS")

var PsiFile.runInspections: Boolean
    get() {
        return getUserData(RUN_INSPECTIONS_KEY) ?: true
    }
    set(value) {
        putUserData(RUN_INSPECTIONS_KEY, value)
    }

@Suppress("unused")
val VirtualFile.cachedVariantStrict
    get() = (this as? CaosVirtualFile)?.variant
        ?: this.getUserData(CaosScriptFile.VariantUserDataKey)

var VirtualFile.cachedVariant: CaosVariant?
    get() = (this as? CaosVirtualFile)?.variant
        ?: this.getUserData(CaosScriptFile.VariantUserDataKey)
        ?: VariantFilePropertyPusher.readFromStorage(this)
    set(variant) {
        (this as? CaosVirtualFile)?.variant = variant
        this.putUserData(CaosScriptFile.VariantUserDataKey, variant)
        VariantFilePropertyPusher.writeToStorage(this, variant ?: CaosVariant.UNKNOWN)
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

val CaosScriptFile.caos2: String?
    get() {
        if (!this.isValid) {
            return null
        }
        return  if (prayTags.any { PrayCommand.fromString(it.tag) == PrayCommand.PRAY_FILE })
            CAOS2Pray
        else if (prayTags.any { CobCommand.fromString(it.tag) == CobCommand.COBFILE })
            CAOS2Cob
        else {
            PsiTreeUtil.getChildOfType(
                this,
                CaosScriptCaos2Block::class.java
            )?.let {
                if (it.isCaos2Pray)
                    CAOS2Pray
                else if (it.isCaos2Cob)
                    CAOS2Cob
                else
                    null
            }
        }
    }


val CaosScriptFile.isCaos2Cob: Boolean
    get() = PsiTreeUtil.getChildOfType(
        this,
        CaosScriptCaos2Block::class.java
    )?.isCaos2Cob.orFalse()

val CaosScriptFile?.disableMultiScriptChecks: Boolean
    get() {
        return this == null || this.isDump || this.isCaos2Cob
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
        return variants.minBy { it.index }
    }
    return null
}

internal fun CaosScriptFile.getScripts(): Collection<CaosScriptScriptElement> {
    return collectElementsOfType(this, CaosScriptScriptElement::class.java)
}

@Suppress("unused")
internal inline fun <reified T:CaosScriptScriptElement> CaosScriptFile.getScriptsOfType(): List<CaosScriptScriptElement> {
    return collectElementsOfType(this, CaosScriptScriptElement::class.java)
        .filterIsInstance<T>()
}