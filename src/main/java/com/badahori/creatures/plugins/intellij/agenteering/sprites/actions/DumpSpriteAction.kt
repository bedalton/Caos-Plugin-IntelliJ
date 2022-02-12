package com.badahori.creatures.plugins.intellij.agenteering.sprites.actions

import bedalton.creatures.structs.Pointer
import bedalton.creatures.util.PathUtil
import bedalton.creatures.util.pathSeparator
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.files
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.AgentMessages
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.sprites.blk.BlkFileType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.c16.C16FileType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.s16.S16FileType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.spr.SprFileType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser
import com.badahori.creatures.plugins.intellij.agenteering.utils.toPngByteArray
import com.badahori.creatures.plugins.intellij.agenteering.utils.writeChild
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import icons.CaosScriptIcons
import java.io.File
import java.io.IOException
import java.nio.file.Path

class DumpSpriteAction : AnAction(
    AgentMessages.message("actions.dump-sprite.title"),
    AgentMessages.message("actions.dump-sprite.description"),
    CaosScriptIcons.SDK_ICON
), DumbAware {

    override fun isDumbAware(): Boolean = true

    override fun startInTransaction(): Boolean = true

    override fun update(e: AnActionEvent) {
        val files = e.files
        if (files.isEmpty() || files.none { it.fileType in spriteFileTypes }) {
            e.presentation.isVisible = false
            return
        }
        e.presentation.isVisible = true
    }

    override fun actionPerformed(e: AnActionEvent) {
        val files = e.files
        val project = e.project
            ?: return
        if (files.isEmpty())
            return
        val initialPath = if (files.size == 1) {
            files[0].parent.path + File.separatorChar + files[0].nameWithoutExtension
        } else {
            files[0].parent.path + File.separatorChar
        }
        SpriteDumpDialog.create(project, initialPath, files.size > 1) run@{ parentPath, useChildDirectories ->
            if (parentPath == null)
                return@run
            val commandName = if (files.size == 1)
                "Dump ${files[0].name} Sprite"
            else
                "Dump Sprites"
            val createdFiles = mutableListOf<Pair<VirtualFile, VirtualFile>>()
            WriteCommandAction
                .writeCommandAction(project)
                .withGroupId("caos.SPRITE_DUMP")
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
                CaosNotifications.createInfoNotification(project, "Sprite Dump", "No sprites dumped")
                this.dialogWrapper.close(1)
            }
            this.showAndGet()
        }
    }

    private fun dump(
        project: Project,
        files: Array<VirtualFile>,
        path: String,
        useChildDirectories: Boolean,
        createdFiles: MutableList<Pair<VirtualFile, VirtualFile>>
    ) {
        ensureParentDirectory(path, createdFiles)
        val dumped = Pointer(0)
        val failed = Pointer(0)
        for (file in files) {
//            WriteCommandAction.writeCommandAction(project)
//                .withName("Dump Sprite: ${file.name}")
//                .withGroupId("caos.DUMP_SPRITE")
//                .withUndoConfirmationPolicy(UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION)
//                .run<Exception> {
//                    dumpFile(
//                        files,
//                        file,
//                        parent,
//                        useChildDirectories,
//                        dumped,
//                        failed,
//                        createdFiles
//                    )
//                }
            CommandProcessor.getInstance().runUndoTransparentAction {
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
        createdFiles: MutableList<Pair<VirtualFile, VirtualFile>>
    ) {
        if (file.fileType !in spriteFileTypes)
            return
        val targetPath = if (files.size > 1 && useChildDirectories)
            parentPath + '/' + file.nameWithoutExtension
        else
            parentPath
        val parentFile = ensureParentDirectory(targetPath, createdFiles)
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
        createdFiles: MutableList<Pair<VirtualFile, VirtualFile>>
    ): Boolean {
        // Parse sprite and get images
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
            CommandProcessor.getInstance().runUndoTransparentAction {
                // Format File Number
                val spriteNumber = "$i".padStart(indexLength, '0')
                // Create sprite image destination file
                val spriteFileName = prefix + spriteNumber + suffix
                val existing = parentVirtualFile.findChild(spriteFileName)
                existing?.delete(this@DumpSpriteAction)
                val imageBytes = image.toPngByteArray()
                // Write the sprite image to the file as PNG
                val child = parentVirtualFile.writeChild(spriteFileName, imageBytes)
                createdFiles.add(parentVirtualFile to child)
            }
        }
        return true
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
                throw IOException("Cannot dump sprites. Path component ${tempParent.path + pathSeparator + component} is not a directory")
            if (current == null || !current.exists()) {
                current = tempParent.createChildDirectory(this@DumpSpriteAction, component)
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
        private val spriteFileTypes = listOf(
            SprFileType,
            S16FileType,
            C16FileType,
            BlkFileType
        )
    }
}