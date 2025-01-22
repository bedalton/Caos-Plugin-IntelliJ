@file:Suppress("ActionPresentationInstantiatedInCtor")

package com.badahori.creatures.plugins.intellij.agenteering.bundles.agents.actions

import com.badahori.creatures.plugins.intellij.agenteering.bundles.agents.lang.AgentFileDetector
import com.badahori.creatures.plugins.intellij.agenteering.bundles.agents.lang.AgentFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.files
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.VirtualFileUtil
import com.badahori.creatures.plugins.intellij.agenteering.utils.rethrowAnyCancellationException
import com.badahori.creatures.plugins.intellij.agenteering.vfs.VirtualFileStreamReader
import com.bedalton.common.structs.Pointer
import com.bedalton.creatures.agents.pray.parser.parsePrayAgentToFiles
import com.bedalton.creatures.agents.util.RelativeFileSystem
import com.bedalton.io.bytes.MemoryByteStreamReader
import com.bedalton.vfs.LocalFileSystem
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileTooBigException
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

class DumpAgentAction : AnAction(), DumbAware {

    override fun isDumbAware(): Boolean = true


    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

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
            val commandName = if (files.size == 1) {
                "Dump ${files[0].name} Agent's Scripts and Files"
            } else {
                "Dump Agent Scripts and Files"
            }
            val createdFiles = mutableListOf<Pair<VirtualFile, VirtualFile>>()
            WriteCommandAction
                .writeCommandAction(project)
                .withGroupId("caos.AGENT_DUMP_${actionIndex.incrementAndGet()}")
                .withName(commandName)
                .withUndoConfirmationPolicy(UndoConfirmationPolicy.REQUEST_CONFIRMATION)
                .withGlobalUndo()
                .run<Exception> {
                    dump(project, files, parentPath, useChildDirectories, createdFiles)
                    val parent = VfsUtil.findFile(Paths.get(parentPath), true)
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
        VirtualFileUtil.ensureParentDirectory(path, createdFiles)
        val dumped = Pointer(0)
        val failed = Pointer(0)

        val onDone = {
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
        var done = 0
        val count = files.size
        for (i in files.indices) {
            val file = files[i]
            CommandProcessor.getInstance().runUndoTransparentAction {
                runBackgroundableTask("Dumping agent: ${file.name}") {
                    it.isIndeterminate = true
                    runBlocking {
                        dumpFile(
                            project,
                            coroutineContext,
                            it,
                            files,
                            file,
                            path,
                            useChildDirectories,
                            dumped,
                            failed,
                            createdFiles
                        )
                        if (++done >= count) {
                            invokeLater {
                                onDone()
                            }
                        }
                    }
                }
            }
        }

    }

    private suspend fun dumpFile(
        project: Project,
        coroutineContext: CoroutineContext,
        progressIndicator: ProgressIndicator,
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
        val parentFile = VirtualFileUtil.ensureParentDirectory(targetPath, createdFiles)
        // Try and dump agent
        // Dump may throw exception on agent parse failure
        val success = try {
            // Returns true if all images were written, false if some were not written
            dump(coroutineContext, progressIndicator, parentFile, file, useChildDirectories, createdFiles)
        } catch (e: FileTooBigException) {
            CaosNotifications.showError(
                project,
                "Agent Dump",
                "Could not dump ${file.name}. File is too big"
            )
            false
        } catch (e: Exception) {
            e.rethrowAnyCancellationException()
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
    private suspend fun dump(
        coroutineContext: CoroutineContext,
        progressIndicator: ProgressIndicator,
        parentVirtualFile: VirtualFile,
        file: VirtualFile,
        useChildDirectories: Boolean,
        createdFiles: MutableList<Pair<VirtualFile, VirtualFile>>
    ): Boolean {
        // Parse Agent and write files
        val stream = if (file.length < VirtualFileStreamReader.MAX_IN_MEMORY_STREAM_LENGTH) {
            MemoryByteStreamReader(file.contentsToByteArray())
        } else {
            VirtualFileStreamReader(file)
        }
        val prefix = if (useChildDirectories) "" else file.nameWithoutExtension
        val relativeWriter = RelativeFileSystem(LocalFileSystem!!, parentVirtualFile.path)

//        val result = try {
        val result = coroutineScope {
            parsePrayAgentToFiles(
                coroutineScope = this,
                fileName = file.name,
                prefix = prefix,
                reader = stream,
                fileSystem = relativeWriter,
                whitelist = "*"
            ) { i, total, _, _,_ ->
                if (progressIndicator.isIndeterminate) {
                    progressIndicator.isIndeterminate = false
                }
                if (total > 0) {
                    progressIndicator.fraction = i.toDouble() / total.toDouble()
                }
                if (i < total) {
                    progressIndicator.text2 = "Finished $i of $total; ${total - i} remaining"
                } else {
                    progressIndicator.isIndeterminate = true
                    progressIndicator.text2 = "Writing files"
                }
            }
        }
//        } catch (e: Exception) {
//            LOGGER.severe("Failed to dump agent: ${file.name} with error: ${e.message}")
//            return false
//        }
        createdFiles.add(Pair(file, parentVirtualFile))
        return coroutineScope {
            result.files(this).isNotEmpty()
        }
    }


    companion object {
        private var actionIndex = AtomicInteger(0)
    }
}