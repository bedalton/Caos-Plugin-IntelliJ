package com.badahori.creatures.plugins.intellij.agenteering.caos.project

import com.badahori.creatures.plugins.intellij.agenteering.caos.project.library.CaosBundleSourcesRegistrationUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.library.CaosSdkProjectRootsChangeListener
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.badahori.creatures.plugins.intellij.agenteering.indices.SpriteAttPathPropertyPusher
import com.badahori.creatures.plugins.intellij.agenteering.sprites.indices.BreedSpriteIndex
import com.badahori.creatures.plugins.intellij.agenteering.utils.getModule
import com.badahori.creatures.plugins.intellij.agenteering.utils.invokeLater
import com.badahori.creatures.plugins.intellij.agenteering.utils.virtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem
import com.intellij.AppTopics
import com.intellij.ProjectTopics
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope


class CaosProjectStartupActivity : StartupActivity {

    private var project: Project? = null

    private val callback = object : FileEditorManagerListener {
        override fun fileOpened(editorManager: FileEditorManager, file: VirtualFile) {
            if (!file.isValid) {
                return
            }
            registerOnAny()
        }
    }

    private val attFileListenerCallback: BulkFileListener = object : BulkFileListener {
        val validExtensions = listOf(
            "spr",
            "s16",
            "c16",
            "att"
        )

        override fun after(events: MutableList<out VFileEvent>) {
            super.after(events)
            val project = project
                ?: return

            if (project.isDisposed) {
                this@CaosProjectStartupActivity.project = null
                return
            }

            if (events.none { it is VFileMoveEvent || it is VFileCreateEvent || it is VFileDeleteEvent || it is VFileCopyEvent }) {
                return
            }

            invokeLater {
                if (project.isDisposed) {
                    this@CaosProjectStartupActivity.project = null
                    return@invokeLater
                }
                if (DumbService.isDumb(project)) {
                    return@invokeLater
                }
                val keys = events
                    .mapNotNull map@{
                        val file = it.file
                            ?: return@map null
                        if (!file.isValid || file.extension?.lowercase() !in validExtensions)
                            return@map null
                        BreedPartKey.fromFileName(file.name)
                    }
                try {
//                    for (key in keys) {
//                        val sprites =
//                            BreedSpriteIndex.findMatching(project, key, GlobalSearchScope.projectScope(project))
//                        for (sprite in sprites) {
//                            if (sprite.isValid) {
//                                SpriteAttPathPropertyPusher.writeToStorage(sprite, null)
//                            }
//                        }
//                    }
                } catch (e: Exception) {
                    return@invokeLater
                }
            }
        }
    }

    override fun runActivity(project: Project) {
        this.project = project

        if (project.isDisposed) {
            this.project = null
            return
        }
        registerFileOpenHandler(project)
        registerProjectRootChangeListener(project)
        registerFileSaveHandler()
        registerAttClearOnSpriteHandler(project)
        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart {
                if (project.isDisposed) {
                    return@runWhenSmart
                }
                registerOnAny()
            }
        } else {
            registerOnAny()
        }
    }

    private fun registerFileOpenHandler(project: Project) {
        if (project.isDisposed) {
            return
        }
        project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, callback)
    }


    private fun registerAttClearOnSpriteHandler(project: Project) {
        if (project.isDisposed) {
            return
        }
        project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, attFileListenerCallback)
    }

    private fun registerFileSaveHandler() {
        ApplicationManager.getApplication().messageBus.connect()
            .subscribe(AppTopics.FILE_DOCUMENT_SYNC, object : FileDocumentManagerListener {
                override fun beforeDocumentSaving(document: Document) {
                    super.beforeDocumentSaving(document)
                    if (project?.isDisposed != false) {
                        return
                    }
                    val virtualFile = document.virtualFile
                        ?: return
                    if (!virtualFile.isValid) {
                        return
                    }
                    val caosVirtualFile = virtualFile as? CaosVirtualFile
                        ?: return
                    CaosVirtualFileSystem.instance.fireOnSaveEvent(caosVirtualFile)
                }
            })
    }

    private fun hasAnyCaosFiles(project: Project): Boolean {
        if (project.isDisposed) {
            return false
        }
        return ATTACH_SOURCES_IF_FILE_TYPE_LIST.any { extension ->
            FilenameIndex.getAllFilesByExt(project, extension).isNotEmpty()
        }
    }

    private fun registerProjectRootChangeListener(project: Project) {
        if (project.isDisposed) {
            return
        }
        project.messageBus.connect().subscribe(ProjectTopics.PROJECT_ROOTS, CaosSdkProjectRootsChangeListener)
    }


    private fun registerOnAny() {
        val project = project
            ?: return
        if (project.isDisposed) {
            this.project = null
            return
        }
        if (hasAnyCaosFiles(project))
            CaosBundleSourcesRegistrationUtil.register(null, project)
        DumbService.getInstance(project).runWhenSmart {
            if (project.isDisposed) {
                this.project = null
                return@runWhenSmart
            }
            val modules = ATTACH_SOURCES_IF_FILE_TYPE_LIST.flatMap { extension ->
                FilenameIndex.getAllFilesByExt(project, extension)
            }.mapNotNull {
                it.getModule(project)
            }.toSet()
            for (module in modules) {
                CaosBundleSourcesRegistrationUtil.register(module, project)
            }
        }
    }

    companion object {
        //private val LOGGER = Logger.getLogger("#" + CaosProjectStartupActivity::class.java)

        private val ATTACH_SOURCES_IF_FILE_TYPE_LIST = listOf("cos", "cob", "agent", "agents")
    }
}