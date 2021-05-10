package com.badahori.creatures.plugins.intellij.agenteering.sprites.actions

import com.badahori.creatures.plugins.intellij.agenteering.caos.action.files
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.sprites.c16.C16FileType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.s16.S16FileType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.spr.SprFileType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser
import com.badahori.creatures.plugins.intellij.agenteering.utils.toPngByteArray
import com.badahori.creatures.plugins.intellij.agenteering.utils.runWriteAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import icons.CaosScriptIcons
import java.io.File

class DumpSpriteAction : AnAction(
    CaosBundle.message("actions.dump-sprite.title"),
    CaosBundle.message("actions.dump-sprite.description"),
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
            runWriteAction {
                dump(project, files, parentPath, useChildDirectories)
            }
        }.apply {
            setCancelOperation {
                CaosNotifications.createInfoNotification(project, "Sprite Dump", "No sprites dumped")
                this.dialogWrapper.close(1)
            }
            this.showAndGet()
        }
    }

    private fun dump(project:Project, files: Array<VirtualFile>, path: String, useChildDirectories:Boolean) {
        val parent = File(path)
        if (!parent.exists())
            parent.mkdirs()
        var dumped = 0
        var failed = 0
        for (file in files) {
            if (file.fileType !in spriteFileTypes)
                continue
            val parentFile = if (files.size > 1 && useChildDirectories)
                File(parent, file.nameWithoutExtension)
            else
                parent
            if (!parentFile.exists())
                parentFile.mkdirs()
            // Try and dump sprite
            // Dump may throw exception on sprite parse failure
            val success = try {
                // Returns true if all images were written, false if some where not written
                dump(file, parentFile)
            } catch(e:Exception) {
                LOGGER.severe("Failed to write sprite file: ${file.name} with error: ${e.message}")
                e.printStackTrace()
                false
            }
            if (success) {
                VfsUtil.findFileByIoFile(parentFile, true)
                dumped++
            } else
                failed++
        }
        when {
            failed > 0 -> {
                CaosNotifications.showError(project, "Sprite Dump", "Failed to dump all sprites. Failed ($failed); Succeeded ($dumped)")
            }
            dumped > 0 -> {
                CaosNotifications.showInfo(project, "Sprite Dump", if (dumped > 1) "Successfully dumped ($dumped) sprites" else "Successfully dumped sprite")
            }
            else -> {
                CaosNotifications.showInfo(project, "Sprite Dump", "No files were dumped")
            }
        }
    }

    /**
     * Dumps a sprite file to a given parent directory
     */
    private fun dump(file:VirtualFile, parent:File) : Boolean {
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

        // Keep track of skipped files
        var skipped = 0

        // Write each sprite image to the parent directory
        for (i in images.indices) {
            val image = images[i]
            if (image == null) {
                skipped++
                continue
            }
            // Format File Number
            val spriteNumber = "$i".padStart(indexLength, '0')
            // Create sprite image destination file
            val spriteFile = File(parent, prefix + spriteNumber + suffix)
            // Create sprite file if it does not yet exist
            if (!spriteFile.exists())
                spriteFile.createNewFile()
            // Write the sprite image to the file as PNG
            spriteFile.writeBytes(image.toPngByteArray())
        }
        return skipped == 0
    }

    companion object {
        private val spriteFileTypes = listOf(
            SprFileType,
            S16FileType,
            C16FileType
        )
    }
}