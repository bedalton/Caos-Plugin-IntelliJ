package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang


import bedalton.creatures.agents.pray.compiler.PrayCompileOptions
import bedalton.creatures.agents.pray.compiler.PrayCompileOptionsImpl
import bedalton.creatures.common.util.stripSurroundingQuotes
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayAgentBlock
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayFileHeader
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.PrayInlineFile
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.stubs.AgentBlockStruct
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.stubs.InlineFileStruct
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.stubs.PrayFileStub
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFileType
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.FilePropertyPusher
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.openapi.vfs.newvfs.FileAttribute
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

class PrayFile constructor(viewProvider: FileViewProvider) :
    PsiFileBase(viewProvider, PrayLanguage) {

    val languageString get() = stub?.language ?: PsiTreeUtil
        .collectElementsOfType(this, PrayFileHeader::class.java)
        .firstOrNull()
        ?.languageString
        ?.text
        ?.stripSurroundingQuotes()

    val agentBlocks: List<AgentBlockStruct> get() {
        stub?.agentBlocks?.let {
            return it
        }
        return PsiTreeUtil.collectElementsOfType(this, PrayAgentBlock::class.java)
            .map {
                AgentBlockStruct(
                    blockTag = it.blockTagString,
                    blockName = it.blockNameString,
                    tags = it.tagStructs
                )
            }
    }

    val inlineFiles: List<InlineFileStruct> get() {
        stub?.inlineFiles?.let {
            return it
        }
        return PsiTreeUtil.collectElementsOfType(this, PrayInlineFile::class.java)
            .map {
                InlineFileStruct(
                    blockTag = it.blockTag?.text,
                    blockName = it.outputFileNameString,
                    inputFile = it.inputFileNameString
                )
            }
    }

    private var mCliOptions: PrayCompileOptions? = null
    var compilerSettings: PrayCompileOptions?
        get() = mCliOptions ?: getUserData(PRAY_COMPILER_SETTINGS_KEY)
        set(value) {
            mCliOptions = value
            putUserData(PRAY_COMPILER_SETTINGS_KEY, value)
            PraySettingsPropertyPusher.writeToStorage(virtualFile, value)
            virtualFile.putUserData(PRAY_COMPILER_SETTINGS_KEY, value)
        }


    val stub get() = super.getStub() as? PrayFileStub

    override fun getFileType(): FileType {
        return PrayFileType
    }

    fun <PsiT : PsiElement> getChildOfType(childClass: Class<PsiT>): PsiT? =
            PsiTreeUtil.getChildOfType(this, childClass)


    fun <PsiT : PsiElement> getChildrenOfType(childClass: Class<PsiT>): List<PsiT> =
            PsiTreeUtil.getChildrenOfTypeAsList(this, childClass)

    override fun toString(): String {
        return "PRAY File"
    }
}



class PraySettingsPropertyPusher private constructor() : FilePropertyPusher<PrayCompileOptions?> {

    override fun getDefaultValue(): PrayCompileOptions = PrayCompileOptionsImpl(
        mergeScripts = false,
        validate = true,
        mergeRscr = false,
        generateScriptRemovers = false,
        generateAgentRemovers = false
    )

    override fun getFileDataKey(): Key<PrayCompileOptions?> {
        return PRAY_COMPILER_SETTINGS_KEY
    }

    override fun pushDirectoriesOnly(): Boolean = false

    override fun afterRootsChanged(p1: Project) {}

    override fun getImmediateValue(project: Project, file: VirtualFile?): PrayCompileOptions? {
        if (file == null)
            return null
        val local = when (val psi = file.getPsiFile(project)) {
            is PrayFile -> psi.compilerSettings
            is CaosScriptFile -> psi.compilerSettings
            else -> null
        }
        if (local != null)
            return local
        return file.getUserData(PRAY_COMPILER_SETTINGS_KEY)
            ?: readFromStorage(file)
    }

    override fun getImmediateValue(module: Module): PrayCompileOptions? {
        return null
    }

    override fun persistAttribute(project: Project, file: VirtualFile, variant: PrayCompileOptions) {
        writeToStorage(file, variant)
    }

    override fun acceptsDirectory(directory: VirtualFile, project: Project): Boolean {
        return true
    }

    override fun acceptsFile(file: VirtualFile): Boolean {
        return file.fileType is PrayFileType || file.fileType is CaosScriptFileType
    }

    companion object {
        private val PRAY_COMPILER_ATTRIBUTES = FileAttribute("pray_compiler_options", 0, false)

        internal fun readFromStorage(file: VirtualFile): PrayCompileOptions? {
            // If file is not virtual file
            // Bail out as only VirtualFileWithId files
            // Have data that could be read through the stream.
            if (file !is VirtualFileWithId) {
                // Get possible session user data written on top of this file
                return file.getUserData(PRAY_COMPILER_SETTINGS_KEY)
            }

            val stream = PRAY_COMPILER_ATTRIBUTES.readAttribute(file)
                ?: return null

            // True if compiler settings is not null
            if (!stream.readBoolean())
                return null
            val validate = stream.readBoolean()
            val mergeScripts = stream.readBoolean()
            val mergeRscr = stream.readBoolean()
            val generateScriptRemovers = stream.readBoolean()
            val generateAgentRemovers = stream.readBoolean()
            stream.close()
            return PrayCompileOptionsImpl(
                validate = validate,
                mergeScripts = mergeScripts,
                mergeRscr = mergeRscr,
                generateScriptRemovers = generateScriptRemovers,
                generateAgentRemovers = generateAgentRemovers,
            )
        }

        internal fun writeToStorage(file: VirtualFile, options: PrayCompileOptions?) {
            if (file !is VirtualFileWithId)
                return
            val stream = PRAY_COMPILER_ATTRIBUTES.writeAttribute(file)
            stream.writeBoolean(options != null)
            if (options == null)
                return
            stream.writeBoolean(options.validate)
            stream.writeBoolean(options.mergeScripts)
            stream.writeBoolean(options.mergeRscr)
            stream.writeBoolean(options.generateScriptRemovers)
            stream.writeBoolean(options.generateAgentRemovers)
            stream.close()
        }
    }
}

internal val PRAY_COMPILER_SETTINGS_KEY =
    Key<PrayCompileOptions?>("com.badahori.creatures.plugins.intellij.agenteering.bundle.pray.PRAY_COMPILER_SETTINGS")
