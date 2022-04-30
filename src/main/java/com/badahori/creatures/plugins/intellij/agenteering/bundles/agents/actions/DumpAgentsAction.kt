package com.badahori.creatures.plugins.intellij.agenteering.bundles.agents.actions

import bedalton.creatures.bytes.RelativeFileWriter
import bedalton.creatures.pray.io.FileWriter
import bedalton.creatures.pray.parser.parsePrayAgentToFiles
import bedalton.creatures.structs.Pointer
import bedalton.creatures.util.PathUtil
import bedalton.creatures.util.pathSeparator
import com.badahori.creatures.plugins.intellij.agenteering.bundles.agents.lang.AgentFileDetector
import com.badahori.creatures.plugins.intellij.agenteering.bundles.agents.lang.AgentFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.files
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.AgentMessages
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.vfs.VirtualFileStreamReader
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileTooBigException
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import icons.CaosScriptIcons
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

class DumpAgentAction : AnAction(
    AgentMessages.message("actions.dump-agent.title"),
    AgentMessages.message("actions.dump-agent.description"),
    CaosScriptIcons.SDK_ICON
), DumbAware {

    override fun isDumbAware(): Boolean = true

    override fun startInTransaction(): Boolean = true

    override fun update(e: AnActionEvent) {
        val files = e.files
        if (files.isEmpty() || files.none { it.fileType == AgentFileType }) {
            e.presentation.isVisible = false
            return
        }
        e.presentation.isVisible = true
    }

    override fun actionPerformed(e: AnActionEvent) {
        val files = e.files
            .filter { it.fileType === AgentFileType || it.extension?.lowercase() in AgentFileDetector.AGENT_FILE_EXTENSIONS }
        val project = e.project
            ?: return
        if (files.isEmpty())
            return
        val initialPath = if (files.size == 1) {
            files[0].parent.path + File.separatorChar + files[0].nameWithoutExtension
        } else {
            files[0].parent.path + File.separatorChar
        }
        AgentDumpDialog.create(project, initialPath, files.size > 1) run@{ parentPath, useChildDirectories ->
            if (parentPath == null)
                return@run
            val commandName = if (files.size == 1)
                "Dump ${files[0].name} Agent's Scripts and Files"
            else
                "Dump Agent Scripts and Files"
            val createdFiles = mutableListOf<Pair<VirtualFile, VirtualFile>>()
            WriteCommandAction
                .writeCommandAction(project)
                .withGroupId("caos.AGENT_DUMP_${actionIndex.incrementAndGet()}")
                .withName(commandName)
                .withUndoConfirmationPolicy(UndoConfirmationPolicy.REQUEST_CONFIRMATION)
                .withGlobalUndo()
                .run<Exception> {
                    CommandProcessor.getInstance().runUndoTransparentAction {
                        dump(project, files, parentPath, useChildDirectories, createdFiles)
                    }
                    val parent = VfsUtil.findFile(Path.of(parentPath), true)
                    parent?.parent?.refresh(true, true)
                }
        }.apply {
            setCancelOperation {
                CaosNotifications.createInfoNotification(project, "Agent Dump", "No agent files dumped")
                this.dialogWrapper.close(1)
            }
            this.showAndGet()
        }
    }

    private fun dump(
        project: Project,
        files: List<VirtualFile>,
        path: String,
        useChildDirectories: Boolean,
        createdFiles: MutableList<Pair<VirtualFile, VirtualFile>>
    ) {
        ensureParentDirectory(path, createdFiles)
        val dumped = Pointer(0)
        val failed = Pointer(0)
        for (file in files) {
            CommandProcessor.getInstance().runUndoTransparentAction {
                dumpFile(
                    project,
                    files,
                    file,
                    path,
                    useChildDirectories,
                    dumped,
                    failed,
                    createdFiles
                )
            }
        }
        when {
            failed.value > 0 -> {
                CaosNotifications.showError(
                    project,
                    "Agent Dump",
                    "Failed to dump all agents. Failed (${failed.value}); Succeeded (${dumped.value})"
                )
            }
            dumped.value > 0 -> {
                CaosNotifications.showInfo(
                    project,
                    "Agent Dump",
                    if (dumped.value > 1) "Successfully dumped (${dumped.value}) agents" else "Successfully dumped agent"
                )
            }
            else -> {
                CaosNotifications.showInfo(project, "Agent Dump", "No files were dumped out of ${files.size}")
            }
        }
    }

    private fun dumpFile(
        project: Project,
        files: List<VirtualFile>,
        file: VirtualFile,
        parentPath: String,
        useChildDirectories: Boolean,
        dumped: Pointer<Int>,
        failed: Pointer<Int>,
        createdFiles: MutableList<Pair<VirtualFile, VirtualFile>>
    ) {
        val targetPath = if (files.size > 1 && useChildDirectories)
            parentPath + '/' + file.nameWithoutExtension
        else
            parentPath
        val parentFile = ensureParentDirectory(targetPath, createdFiles)
        // Try and dump agent
        // Dump may throw exception on agent parse failure
        val success = try {
            // Returns true if all images were written, false if some were not written
            dump(parentFile, file, useChildDirectories, createdFiles)
        } catch (e: FileTooBigException) {
            CaosNotifications.showError(
                project,
                "Agent Dump",
                "Could not dump ${file.name}. File is too big"
            )
            false
        } catch (e: Exception) {
            LOGGER.severe("Failed to dump agent file: ${file.name} to $parentPath with error: ${e.message}")
            e.printStackTrace()
            false
        }
        if (success) {
            dumped.value++
            VfsUtil.markDirtyAndRefresh(false, true, true, parentFile)
        } else {
            LOGGER.severe("Failed to dump agent file: ${file.name} to $parentPath")
            failed.value++
        }
    }

    /**
     * Dumps an agent file to a given parent directory
     */
    private fun dump(
        parentVirtualFile: VirtualFile,
        file: VirtualFile,
        useChildDirectories: Boolean,
        createdFiles: MutableList<Pair<VirtualFile, VirtualFile>>
    ): Boolean {
        // Parse Agent and write files
        val stream = VirtualFileStreamReader(file)
        val prefix = if (useChildDirectories) "" else file.nameWithoutExtension
        val relativeWriter = RelativeFileWriter(parentVirtualFile.path, FileWriter)
//        val result = try {
             val result = parsePrayAgentToFiles(file.name, prefix = prefix, stream, relativeWriter, "*")
//        } catch (e: Exception) {
//            LOGGER.severe("Failed to dump agent: ${file.name} with error: ${e.message}")
//            return false
//        }
        createdFiles.add(Pair(file, parentVirtualFile))
        return result.files.isNotEmpty()
    }

    private fun ensureParentDirectory(path: String, createdFiles: MutableList<Pair<VirtualFile, VirtualFile>>): VirtualFile {
        val first = getFirstExistsParent(path)
            ?: throw IOException("Path <$path> is invalid")
//        if (ApplicationManager.getApplication().isReadAccessAllowed)
//            throw IOException("Find file cannot be called from read thread")

        if (first.path == path.replace(pathSeparator, "/")) {
            return first
        }
        var tempParent: VirtualFile = first
        for (component in path.split(pathSeparator)) {
            if (component.isBlank())
                continue
            var current = tempParent.findChild(component)
            if (current?.isDirectory == false)
                throw IOException("Cannot dump agents. Path component ${tempParent.path + pathSeparator + component} is not a directory")
            if (current == null || !current.exists()) {
                current = tempParent.createChildDirectory(this@DumpAgentAction, component)
                createdFiles.add(tempParent to current)
            }
            if (tempParent.path == current.path) {
                throw IOException("Path not changed after set")
            }
            tempParent = current
        }
        return tempParent
    }

    private fun getFirstExistsParent(path: String): VirtualFile? {
        var currentPath = ""
        var current: VirtualFile? = null
        val pathNormalize = PathUtil.combine(*path.split(pathSeparator).toTypedArray())
        for (component in pathNormalize.split(path)) {
            currentPath += component + pathSeparator
            LocalFileSystem.getInstance().findFileByPath(currentPath)?.let {
                if (!it.exists())
                    return current
                current = it
            } ?: return current
        }
        return current
    }

    companion object {
        private var actionIndex = AtomicInteger(0)
    }
}