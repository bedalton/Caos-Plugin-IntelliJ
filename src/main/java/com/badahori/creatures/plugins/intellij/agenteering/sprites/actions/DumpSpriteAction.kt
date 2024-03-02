package com.badahori.creatures.plugins.intellij.agenteering.sprites.actions

import com.bedalton.common.structs.Pointer
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.files
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.AgentMessages
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.sprites.blk.BlkFileType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.c16.C16FileType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.s16.S16FileType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.spr.SprFileType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.vfs.VirtualFileStreamReader
import com.bedalton.creatures.sprite.parsers.BlkParser
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import korlibs.image.awt.toAwt
import icons.CaosScriptIcons
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger

class DumpSpriteAction : AnAction(
    { AgentMessages.message("actions.dump-sprite.title") },
    { AgentMessages.message("actions.dump-sprite.description") },
    CaosScriptIcons.SDK_ICON
), DumbAware {

    override fun isDumbAware(): Boolean = true

    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = isVisible(e)
    }

    private fun isVisible(e: AnActionEvent): Boolean {
        val files = e.files
        val fileCount = files.size

        return when {
            fileCount == 0 -> false
            fileCount > 15 -> true
            else -> files.any { it.fileType in spriteFileTypes }
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val files = e.files
        val project = e.project
            ?: return
        if (files.isEmpty())
            return

        val allBlks = files.all { it.extension?.lowercase() == "blk" }
        val initialPath = (if (files.size == 1 && !allBlks) {
            files[0].parent.path + File.separatorChar + files[0].nameWithoutExtension
        } else {
            files[0].parent.path + File.separatorChar
        }).replace('/', File.separatorChar)
        SpriteDumpDialog.create(project, initialPath, !allBlks) run@{ parentPath, useChildDirectories ->
            if (parentPath == null)
                return@run
            val commandName = if (files.size == 1)
                "Dump ${files[0].name} Sprite"
            else
                "Dump Sprites"
            val createdFiles = mutableListOf<Pair<VirtualFile, VirtualFile>>()
            val groupId = "caos.SPRITE_DUMP-" + spriteDumpId.incrementAndGet()
            WriteCommandAction
                .writeCommandAction(project)
                .withGroupId(groupId)
                .withName(commandName)
                .withUndoConfirmationPolicy(UndoConfirmationPolicy.REQUEST_CONFIRMATION)
                .withGlobalUndo()
                .run<Exception> {
                    GlobalScope.launch {
                        dump(
                            project,
                            commandName,
                            groupId,
                            files,
                            parentPath,
                            useChildDirectories,
                            createdFiles
                        )
                        val parent = VfsUtil.findFile(Paths.get(parentPath), true)
                        parent?.parent?.refresh(true, true)
                    }
                }
        }.apply {
            setCancelOperation {
                CaosNotifications.createInfoNotification(project, "Sprite Dump", "No sprites dumped")
                this.closeWithCancelExitCode()
            }
            this.showAndGet()
        }
    }

    private suspend fun dump(
        project: Project,
        name: String,
        groupId: String,
        files: Array<VirtualFile>,
        path: String,
        useChildDirectories: Boolean,
        createdFiles: MutableList<Pair<VirtualFile, VirtualFile>>,
    ) {
        VirtualFileUtil.ensureParentDirectory(path, createdFiles)
        val dumped = Pointer(0)
        val failed = Pointer(0)
        files.mapAsync { file ->
            WriteCommandAction.writeCommandAction(project)
                .withName(name)
                .withGroupId(groupId)
                .run<Throwable> {
                    runBlocking {
                        dumpFile(
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
        }

        when {
            failed.value > 0 -> {
                CaosNotifications.showError(
                    project,
                    "Sprite Dump",
                    "Failed to dump all sprites. Failed (${failed.value}); Succeeded (${dumped.value})"
                )
            }
            dumped.value > 0 -> {
                CaosNotifications.showInfo(
                    project,
                    "Sprite Dump",
                    if (dumped.value > 1) "Successfully dumped (${dumped.value}) sprites" else "Successfully dumped sprite"
                )
            }
            else -> {
                CaosNotifications.showInfo(project, "Sprite Dump", "No files were dumped")
            }
        }
    }

    private fun dumpFile(
        files: Array<VirtualFile>,
        file: VirtualFile,
        parentPath: String,
        useChildDirectories: Boolean,
        dumped: Pointer<Int>,
        failed: Pointer<Int>,
        createdFiles: MutableList<Pair<VirtualFile, VirtualFile>>,
    ) {
        if (file.fileType !in spriteFileTypes)
            return
        val targetPath = if (files.size > 1 && useChildDirectories && file.extension?.lowercase() != "blk")
            parentPath + '/' + file.nameWithoutExtension
        else
            parentPath
        val parentFile = VirtualFileUtil.ensureParentDirectory(targetPath, createdFiles)
        // Try and dump sprite
        // Dump may throw exception on sprite parse failure
        val success = try {
            // Returns true if all images were written, false if some were not written
            dump(parentFile, file, createdFiles)
        } catch (e: Exception) {
            LOGGER.severe("Failed to write sprite file: ${file.name} with error: ${e.message}")
            e.printStackTrace()
            false
        }
        if (success) {
            VfsUtil.refreshAndFindChild(parentFile.parent, parentFile.name)
            dumped.value++
        } else
            failed.value++
    }

    /**
     * Dumps a sprite file to a given parent directory
     */
    private fun dump(
        parentVirtualFile: VirtualFile,
        file: VirtualFile,
        createdFiles: MutableList<Pair<VirtualFile, VirtualFile>>,
    ): Boolean {
        // Parse sprite and get images
        val blk = file.extension?.lowercase() == "blk"

        if (blk) {
            return try {
                val stream = VirtualFileStreamReader(file)
                val png = BlkParser.parse(stream).toAwt()
                write(parentVirtualFile, file.nameWithoutExtension + ".png", png, createdFiles)
                true
            } catch (e: Exception) {
                false
            }
        }
        val sprite = SpriteParser.parse(file)
        val images = sprite.images

        // Get sprite filename parts
        val prefix = file.nameWithoutExtension + "-"
        val suffix = ".png"
        // Get sprite number pad length
        val indexLength = when {
            images.size < 999 -> 3
            images.size < 9999 -> 4
            else -> 5
        }

        // Write each sprite image to the parent directory
        for (i in images.indices) {
            val image = images[i]
            // Format File Number
            val spriteNumber = "$i".padStart(indexLength, '0')
            // Create sprite image destination file
            val spriteFileName = prefix + spriteNumber + suffix
            write(parentVirtualFile, spriteFileName, image, createdFiles)
        }
        return true
    }

    private fun write(
        parentVirtualFile: VirtualFile,
        spriteFileName: String,
        image: BufferedImage,
        createdFiles: MutableList<Pair<VirtualFile, VirtualFile>>,
    ) {
        val existing = parentVirtualFile.findChild(spriteFileName)
        existing?.delete(this@DumpSpriteAction)
        val imageBytes = image.toPngByteArray()
        // Write the sprite image to the file as PNG
        val child = parentVirtualFile.writeChild(spriteFileName, imageBytes)
        createdFiles.add(parentVirtualFile to child)
    }

    companion object {
        private val spriteDumpId = AtomicInteger(0)
        private val spriteFileTypes = listOf(
            SprFileType,
            S16FileType,
            C16FileType,
            BlkFileType
        )
    }
}