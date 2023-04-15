package com.badahori.creatures.plugins.intellij.agenteering.att.actions

import com.badahori.creatures.plugins.intellij.agenteering.att.lang.AttFileType
import com.badahori.creatures.plugins.intellij.agenteering.att.parser.AttAutoFill
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.files
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.cachedVariantExplicitOrImplicit
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.inferVariantHard
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.sprites.indices.SpriteLocator
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import icons.CaosScriptIcons
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger

class AttNewFileFromSpritesAction : AnAction(
    /* text = */ CaosBundle.message("att.actions.new-file-from-sprites.multi.title"),
    /* description = */ CaosBundle.message("att.actions.new-file-from-sprites.multi.description"),
    /* icon = */ CaosScriptIcons.ATT_FILE_ICON
) {

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project
            ?: return
        if (project.isDisposed) {
            return
        }
        if (DumbService.isDumb(project)) {
            return
        }

        val bodyPartSprites = files(e)
        val bodyPartSpriteCount = bodyPartSprites.size
        val hasSprites = bodyPartSpriteCount > 0
        e.presentation.isVisible = hasSprites
        e.presentation.isEnabledAndVisible = hasSprites
        e.presentation.isEnabled = hasSprites
        if (!hasSprites) {
            return
        }
        e.presentation.text = if (bodyPartSpriteCount == 1) {
            CaosBundle.message("att.actions.new-file-from-sprites.single.title", bodyPartSprites[0].name)
        } else {
            CaosBundle.message("att.actions.new-file-from-sprites.multi.title")
        }

        e.presentation.description = if (bodyPartSpriteCount == 1) {
            CaosBundle.message("att.actions.new-file-from-sprites.single.description", bodyPartSprites[0].name)
        } else {
            CaosBundle.message("att.actions.new-file-from-sprites.multi.description")
        }
    }

    override fun actionPerformed(e: AnActionEvent) {

        val project = e.project

        if (project == null || project.isDisposed) {
            return
        }

        val files = files(e).nullIfEmpty()
            ?: return
        val parents = files.map { it.parent }.distinct()
        val parentCounts = parents.map { it.path }.distinct().size
        val targetDirectory = if (parentCounts == 1) {
            val hasAtts = VirtualFileUtil
                .childrenWithExtensions(parents[0]!!, true, "att")
                .isNotEmpty()
            if (!hasAtts) {
                val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
                    .apply {
                        roots = listOf(parents[0])
                        description = if (files.size == 1) {
                            CaosBundle.message("att.actions.new-file-from-sprites.single.file-chooser-description",
                                files[0].nameWithoutExtension + ".att")
                        } else {
                            CaosBundle.message("att.actions.new-file-from-sprites.multi.file-chooser-description")
                        }
                    }
                FileChooser.chooseFiles(descriptor, e.project, parents[0])
                    .firstOrNull()
            } else {
                null
            }
        } else {
            null
        }

        val projectVariant = project.inferVariantHard() ?: CaosVariant.UNKNOWN

        val targetParentFile: PsiDirectory? = targetDirectory?.getPsiFile(project) as? PsiDirectory
        var okay = 0
        var error = 0

        val singleFile = files.size == 1

        val groupId = newFileActionId + (atomicId.incrementAndGet()).toString().padStart(4, '0')
        val commandText = if (singleFile) {
            CaosBundle.message("att.actions.new-file-from-sprites.single.command-title", files[0].name)
        } else {
            CaosBundle.message("att.actions.new-file-from-sprites.multi.command-title")
        }
        WriteCommandAction.writeCommandAction(project)
            .withGlobalUndo()
            .withName(commandText)
            .withGroupId(groupId)
            .withUndoConfirmationPolicy(UndoConfirmationPolicy.REQUEST_CONFIRMATION)
            .run<Throwable> {
                for (file in files) {
                    if (project.isDisposed) {
                        return@run
                    }
                    val didInit = init(project, projectVariant, targetParentFile, file, singleFile)
                    if (didInit) {
                        okay++
                    } else {
                        error++
                        CaosNotifications.showError(project,
                            "ATT File",
                            "Failed to create ATT ${file.nameWithoutExtension}.att")
                    }
                }
                val newFilesParents = if (targetDirectory != null) {
                    arrayOf(targetDirectory)
                } else {
                    files
                        .map { it.parent }
                        .distinctBy { it.path }
                        .toTypedArray()
                }
                VfsUtil.markDirtyAndRefresh(true, true, true, *newFilesParents)
            }
    }


    companion object {

        private val atomicId = AtomicInteger(0)
        const val newFileActionId = "Att-New-From-Sprites-"

        fun files(e: AnActionEvent): List<VirtualFile> {
            return e.files
                .filter {
                    it.extension?.lowercase() in SpriteParser.VALID_SPRITE_EXTENSIONS && BreedPartKey.isPartName(it.nameWithoutExtension)
                }
        }

        fun init(project: Project, projectVariant: CaosVariant, targetParentFile: PsiDirectory?, file: VirtualFile, navigate: Boolean): Boolean {
            val fileName = file.nameWithoutExtension + ".att"

            val parent = (targetParentFile ?: file.getPsiFile(project)?.containingDirectory)!!
            if (parent.findFile(fileName) != null) {
                return true
            }
            var variant = file.cachedVariantExplicitOrImplicit
                ?: file.getModule(project)?.inferVariantHard()
            val closest = SpriteLocator.findClosest(variant, file.nameWithoutExtension, file.parent)

            if (variant == null && closest != null) {
                variant = SpriteParser.getBodySpriteVariant(closest, projectVariant)
            }
            val (trueVariant, data) = closest?.let {
                AttAutoFill.paddedData(file.nameWithoutExtension, it, variant ?: projectVariant)
            } ?: Pair(variant, AttAutoFill.blankAttData(fileName, variant ?: projectVariant))

            if (trueVariant == null) {
                return true
            }
            val text = data?.toFileText(trueVariant) ?: ""
            val newFile: PsiFile = try {
                PsiFileFactory.getInstance(project)
                    .createFileFromText(fileName, AttFileType, text)
            } catch (e: Exception) {
                if (e is ProcessCanceledException) {
                    throw e
                }
                LOGGER.severe("Failed to create ATT file for ${file.name}")
                e.printStackTrace()
                return false
            }
            return try {
                parent.add(newFile)
                invokeLater {
                    if (navigate && newFile.canNavigate()) {
                        newFile.navigate(true)
                    }
                }
                true
            } catch (e: Exception) {
                if (e is ProcessCanceledException) {
                    throw e
                }
                LOGGER.severe("Failed to add new ATT to parent directory")
                e.printStackTrace()
                false
            }
        }
    }


}