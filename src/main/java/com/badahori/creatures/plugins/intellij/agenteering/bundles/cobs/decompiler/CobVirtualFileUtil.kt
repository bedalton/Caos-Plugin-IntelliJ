package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.toPngByteArray
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem
import com.bedalton.common.util.className
import com.bedalton.common.util.nullIfEmpty
import com.intellij.openapi.application.*
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import java.util.*
import java.util.concurrent.atomic.AtomicInteger


object CobVirtualFileUtil {
    /**
     * Used to ensure that a cob directory is unique
     */
    private val decompiledId = AtomicInteger(0)


    /**
     * Key for storing the cob decompile virtual folder path
     */
    private val VIRTUAL_FILE_PATH_KEY = Key<String>("creatures.cob.decompiler.DIRECTORY")

    @Suppress("MemberVisibilityCanBePrivate")
    fun cobVirtualFileDirectory(file: VirtualFile): CaosVirtualFile {
        return file.getUserData(VIRTUAL_FILE_PATH_KEY)?.let { path ->
            CaosVirtualFileSystem.instance.getDirectory(path)
        } ?: getOrCreateCobVirtualFileDirectory(file)
    }

    /**
     * Creates a virtual file directory to hold decompiled COB files
     */
    fun getOrCreateCobVirtualFileDirectory(file: VirtualFile): CaosVirtualFile {
        file.getUserData(VIRTUAL_FILE_PATH_KEY)?.nullIfEmpty()?.let { path ->
            return CaosVirtualFileSystem.instance.getDirectory(path, true)!!
        }
        val path = "Decompiled/${decompiledId.incrementAndGet()}/${file.name}"
        file.putUserData(VIRTUAL_FILE_PATH_KEY, path)
        return CaosVirtualFileSystem.instance.getDirectory(path, true)!!.apply {
            isWritable = true
        }
    }

    fun decompiledCobFiles(cobFile: VirtualFile): List<CaosVirtualFile> {
        val directory = cobVirtualFileDirectory(cobFile)
        directory.children.ifEmpty { null }?.let { return it.toList() }
        val data = try {
            CobToDataObjectDecompiler.decompile(cobFile)
                ?: return emptyList<CaosVirtualFile>().apply {
                    LOGGER.severe("Failed to decompile COB: '${cobFile.name}'")
                }
        } catch (e: Exception) {
            LOGGER.severe("Failed to decompile cob: '${cobFile.name}'; ${e.className}: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
        return when (data) {
            is CobFileData.C1CobData -> blockToVirtualFiles(CaosVariant.C1, data.cobBlock, directory)
            is CobFileData.C2CobData -> data.blocks.flatMap {
                blockToVirtualFiles(
                    variant = CaosVariant.C2,
                    block = it,
                    parent = directory
                )
            }

            else -> emptyList()
        }
    }

    /**
     * Converts a COB block into a list of virtual files
     */
    private fun blockToVirtualFiles(
        variant: CaosVariant,
        block: CobBlock,
        parent: CaosVirtualFile
    ): List<CaosVirtualFile> {
        return when (block) {
            is CobBlock.FileBlock.SpriteBlock -> listOf(parent.createChildWithContent(block.fileName, block.contents))
            is CobBlock.FileBlock.SoundBlock -> listOf(parent.createChildWithContent(block.fileName, block.contents))
            is CobBlock.AuthorBlock -> emptyList()
            is CobBlock.AgentBlock -> {

                // Get virtual file directory
                val agentBlockName = block.name.nullIfEmpty() ?: UUID.randomUUID().toString()
                val agentDirectory = parent.createChildDirectory(agentBlockName)

                // Create install script files
                val installScripts: List<CaosVirtualFile> = block.installScripts.let { installScripts ->
                    if (installScripts.size == 1) {
                        val script = installScripts.first()
                        listOf(createChildCaosScript(agentDirectory, variant, script.scriptName, script.code))
                    } else {
                        val multipleInstallScripts = installScripts.size > 1
                        installScripts.mapIndexed { i, script ->
                            val tail = if (multipleInstallScripts) " ($i)" else ""
                            val scriptName = "Install ${script.scriptName}$tail"
                            createChildCaosScript(agentDirectory, variant, scriptName, script.code)
                        }
                    }
                }

                // Create remover script files
                val removalScriptElement = block.removalScript
                val removalScript = createNullableChildCaosScript(
                    parent = agentDirectory,
                    variant = variant,
                    fileName = removalScriptElement?.scriptName ?: "Removal Script",
                    code = removalScriptElement?.code
                )

                // Create event script files
                val eventScripts = block.eventScripts.map map@{ script ->
                    createChildCaosScript(
                        parent = agentDirectory,
                        variant = variant,
                        fileName = script.scriptName,
                        code = script.code
                    )
                }

                // All scripts
                val scripts: List<CaosVirtualFile> = installScripts +
                        listOfNotNull(removalScript) +
                        eventScripts

                // Preview Image
                val previews: List<CaosVirtualFile> = listOfNotNull(block.image?.let {
                    agentDirectory.createChildWithContent("Thumbnail.png", it.toPngByteArray())
                })

                // All children
                previews + scripts.map { file ->
                    file.apply { this.setVariant(variant, true) }
                }
            }

            is CobBlock.UnknownCobBlock -> emptyList()
        }
    }

    /**
     * Creates a caos script virtual file in the given directory
     * Attempts to format file with new lines and indents
     */
    fun createChildTextFile(
        parent: CaosVirtualFile,
        fileName: String,
        text: String,
        fileType: FileType = PlainTextFileType.INSTANCE
    ): CaosVirtualFile {
        parent.findChild(fileName)?.let { return it }
        // Initialize file with contents
        val virtualFile = try {
            parent.createChildWithContent(fileName, text, false)
        } catch (e: Exception) {
            LOGGER.severe("Failed to create virtual file. Error: ${e.message}")
            e.printStackTrace()
            throw e
        }
        virtualFile.fileType = fileType

        val runnable = run@{
            virtualFile.isWritable = false
        }
        if (ApplicationManager.getApplication().isDispatchThread) {
            runWriteAction(runnable)
        } else {
            invokeLater(ModalityState.defaultModalityState()) {
                runWriteAction(runnable)
            }
        }
        return virtualFile
    }

    /**
     * Creates a caos script virtual file in the given directory
     * Attempts to format file with new lines and indents
     */
    fun createChildCaosScript(
        parent: CaosVirtualFile,
        variant: CaosVariant,
        fileName: String,
        code: String
    ): CaosVirtualFile {
        return createNullableChildCaosScript(
            parent = parent,
            variant = variant,
            fileName = fileName,
            code = code
        ) ?: throw NullPointerException("Child CAOS script was null but CAOS script was not")
    }

    /**
     * Creates a caos script virtual file in the given directory
     * Attempts to format file with new lines and indents
     */
    private fun createNullableChildCaosScript(
        parent: CaosVirtualFile,
        variant: CaosVariant,
        fileName: String,
        code: String?
    ): CaosVirtualFile? {

        if (code == null) {
            return null
        }

        parent.findChild(fileName)?.let { return it }
        // Initialize file with contents
        @Suppress("MemberVisibilityCanBePrivate") val virtualFile = try {
            parent.createChildWithContent(fileName, code, false)
                .apply { this.setVariant(variant, true) }
        } catch (e: Exception) {
            LOGGER.severe("Failed to create virtual file. Error: ${e.message}")
            e.printStackTrace()
            throw e
        }
        virtualFile.fileType = CaosScriptFileType.INSTANCE
        return virtualFile
    }
}
