package com.badahori.creatures.plugins.intellij.agenteering.caos.lang

import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptExpandCommasIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCaos2Block
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCaos2Tag
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.HasVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptFileStub
import com.badahori.creatures.plugins.intellij.agenteering.utils.VariantFilePropertyPusher
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.utils.variant
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

class CaosScriptFile constructor(viewProvider: FileViewProvider, val myFile: VirtualFile) :
    PsiFileBase(viewProvider, CaosScriptLanguage), HasVariant {
    private val didFormatInitial = AtomicBoolean(false)
    override var variant: CaosVariant?
        get() {
            return variantOverride
                ?: getUserData(VariantUserDataKey)
                ?: myFile.cachedVariant ?: (this.originalFile as? CaosScriptFile)?.myFile?.cachedVariant
                ?: (module ?: originalFile.module)?.variant
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
                if (ApplicationManager.getApplication().isDispatchThread)
                    FileContentUtilCore.reparseFiles(virtualFile)
            }
            if (ApplicationManager.getApplication().isDispatchThread && this.isValid) {
                DaemonCodeAnalyzer.getInstance(project).restart(this)
            }
        }

    var variantOverride: CaosVariant? = null

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

val RUN_INSPECTIONS_KEY = Key<Boolean?>("com.badahori.creatures.plugins.intellij.agenteering.caos.RUN_INSPECTIONS")

var PsiFile.runInspections: Boolean
    get() {
        return getUserData(RUN_INSPECTIONS_KEY) ?: true
    }
    set(value) {
        putUserData(RUN_INSPECTIONS_KEY, value)
    }
val VirtualFile.cachedVariantStrict
    get() = (this as? CaosVirtualFile)?.variant
        ?: this.getUserData(CaosScriptFile.VariantUserDataKey)

val VirtualFile.cachedVariant: CaosVariant?
    get() = (this as? CaosVirtualFile)?.variant
        ?: this.getUserData(CaosScriptFile.VariantUserDataKey)
        ?: VariantFilePropertyPusher.readFromStorage(this)


val maxDumpHeader = "* Scriptorium Dump".length + 4 // Arbitrary spaces pad

val dumpRegex =
    "\\*\\s*([Ss][Cc][Rr][Ii][Pp][Tt][Oo][Rr][Ii][Uu][Mm]|[Dd][Uu][Mm][Pp]|[Ss][Cc][Rr][Ii][Pp][Tt][Oo][Rr][Ii][Uu][Mm]\\s*[Dd][Uu][Mm][Pp]).*".toRegex()

val PsiFile.isDump: Boolean
    get() {
        return text.trim().let { text ->
            val commentText: String = text.split("\n", ignoreCase = true, limit = 2)[0]
            dumpRegex.matches(commentText)
        }
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
val CaosScriptFile.isCaos2Cob: Boolean
    get() = PsiTreeUtil.getChildOfType(
        this,
        CaosScriptCaos2Block::class.java
    )?.isCaos2Cob.orFalse()

val CaosScriptFile.tags: Map<String, String>
    get() {
        return collectElementsOfType(this, CaosScriptCaos2Tag::class.java).mapNotNull tags@{ tag ->
            val value: String = tag.value
                ?: return@tags null
            tag.tagName to value
        }.toMap()
    }

val CaosScriptFile?.disableMultiScriptChecks: Boolean
    get() {
        return this == null || this.isDump || this.isCaos2Cob
    }

private val CAOS2COB_VARIANT_REGEX = "[*]{2}Caos2Cob\\s*(C1|C2)".toRegex(RegexOption.IGNORE_CASE)
private val CAOS2_BLOCK_VARIANT_REGEX =
    "^[*]#\\s*(C1|C2|CV|C3|DS|[A-Z]{2}|[a-zA-Z][a-zA-Z0-9]{3})(-?Name)?".toRegex(RegexOption.IGNORE_CASE)

fun getCaos2VariantRaw(text: CharSequence): CaosVariant? {
    val variant = text.trim().split('\n', limit = 2)[0].trim().let { firstLineText ->
        CAOS2COB_VARIANT_REGEX.matchEntire(firstLineText)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { variantCode ->
                CaosVariant.fromVal(variantCode)
            }
    }
    if (variant == null || variant == CaosVariant.UNKNOWN) {
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
                            val parsedVariant = CaosVariant.fromVal(variantCode)
                            if (parsedVariant != CaosVariant.UNKNOWN)
                                variants.add(parsedVariant)
                        }
                    }
                }
            }
        return variants.minBy { it.index }
    }
    return null
}