package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.className
import com.badahori.creatures.plugins.intellij.agenteering.utils.toPngByteArray
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
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
    fun cobVirtualFileDirectory(file:VirtualFile) : CaosVirtualFile {
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
    fun decompiledCobFiles(cobFile:VirtualFile, project: Project): List<CaosVirtualFile> {
        val directory = cobVirtualFileDirectory(cobFile)
        directory.children.ifEmpty { null }?.let { return it.toList() }
        val data = try {
            CobToDataObjectDecompiler.decompile(cobFile)
                    ?: return emptyList<CaosVirtualFile>().apply { LOGGER.severe("Failed to decompile COB: '${cobFile.name}'")}
        } catch (e: Exception) {
            LOGGER.severe("Failed to decompile cob: '${cobFile.name}'; ${e.className}: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
        return when (data) {
            is CobFileData.C1CobData -> blockToVirtualFiles(project, CaosVariant.C1, data.cobBlock, directory)
            is CobFileData.C2CobData -> data.blocks.flatMap { blockToVirtualFiles(project, CaosVariant.C2, it, directory) }
            else -> emptyList()
        }
    }

    /**
     * Converts a COB block into a list of virtual files
     */
    private fun blockToVirtualFiles(project: Project, variant: CaosVariant, block: CobBlock, parent: CaosVirtualFile): List<CaosVirtualFile> {
        return when (block) {
            is CobBlock.FileBlock.SpriteBlock -> listOf(parent.createChildWithContent(block.fileName, block.contents))
            is CobBlock.FileBlock.SoundBlock -> listOf(parent.createChildWithContent(block.fileName, block.contents))
            is CobBlock.AuthorBlock -> emptyList()//parent.createChildWithContent("Author.txt", block.)
            is CobBlock.AgentBlock -> {
                val agentBlockName = block.name.nullIfEmpty() ?: UUID.randomUUID().toString()
                val agentDirectory = parent.createChildDirectory(agentBlockName)
                val installScripts:List<CaosVirtualFile> = block.installScripts.let { installScripts ->
                    if (installScripts.size == 1) {
                        val script = installScripts.first()
                        listOf(createChildCaosScript(project, agentDirectory, variant, script.scriptName, script.code))
                    } else {
                        val multipleInstallScripts = installScripts.size > 1
                        installScripts.mapIndexed { i, script ->
                            val scriptName = "Install ${script.scriptName}".let { scriptName ->
                                if (multipleInstallScripts)
                                    "$scriptName ($i)"
                                else
                                    scriptName
                            }
                            createChildCaosScript(project, agentDirectory, variant, scriptName, script.code)
                        }
                    }
                }
                val scripts: List<CaosVirtualFile> = installScripts + listOfNotNull(
                        block.removalScript?.let { createChildCaosScript(project, agentDirectory, variant, it.scriptName, it.code) }
                ) + block.eventScripts.map map@{ script ->
                    createChildCaosScript(project, agentDirectory, variant, script.scriptName, script.code)
                }
                val previews: List<CaosVirtualFile> = listOfNotNull(block.image?.let {
                    agentDirectory.createChildWithContent("Thumbnail.png", it.toPngByteArray())
                })
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
    fun createChildCaosScript(project: Project, parent:CaosVirtualFile, variant: CaosVariant, fileName: String, code: String): CaosVirtualFile {
        parent.findChild(fileName)?.let { return it }
        // Initialize file with contents
        @Suppress( "MemberVisibilityCanBePrivate") val virtualFile = try {
            parent.createChildWithContent(fileName, code, false).apply { this.setVariant(variant, true) }
        } catch(e:Exception) {
            LOGGER.severe("Failed to create virtual file. Error: ${e.message}")
            e.printStackTrace()
            throw e
        }
        virtualFile.fileType = CaosScriptFileType.INSTANCE
        val runnable = run@{
            try {
                if (project.isDisposed) {
                    return@run null
                }
                // Get PSI file after creating virtual file
                val psiFile = ApplicationManager.getApplication().runReadAction(Computable {
                    findFile(project, virtualFile)
                }) ?: return@run virtualFile.apply { LOGGER.severe("Failed to find CaosPsiFile after virtual file completion") }
                // Try quick format if file is found
                virtualFile.isWritable = true
                tryQuickFormat(psiFile, virtualFile)
            } catch(e:Exception) {
                LOGGER.severe("Failed to quick-format after creating virtual file")
                e.printStackTrace()
                virtualFile
            } finally {
                virtualFile.isWritable = false
            }
        }
        if (ApplicationManager.getApplication().isDispatchThread)
            runnable()
        else {
            ApplicationManager.getApplication().invokeLater {
                runnable()
            }
        }
        return virtualFile

    }
}

private fun findFile(project: Project, file:CaosVirtualFile) : CaosScriptFile? {
    if (project.isDisposed) {
        return null
    }
    return if (ApplicationManager.getApplication().isReadAccessAllowed) {
        PsiManager.getInstance(project).findFile(file) as? CaosScriptFile
    } else {
        ApplicationManager.getApplication().runReadAction( Computable {
            PsiManager.getInstance(project).findFile(file) as? CaosScriptFile
        })
    }
}

private fun tryQuickFormat(psiFile:CaosScriptFile, file:CaosVirtualFile) : CaosVirtualFile {
    if (psiFile.project.isDisposed || !psiFile.isValid || !file.isValid) {
        return file
    }
    val writable = file.isWritable
    try {
        file.isWritable = true
        psiFile.quickFormat()
    } catch (e: Exception) {
        LOGGER.severe("Failed to quick format in createChildScript method in CobVirtualFileUtil. Error: ${e.message}")
        e.printStackTrace()
    } finally {
        file.isWritable = writable
    }
    return psiFile.virtualFile as? CaosVirtualFile ?: file
}