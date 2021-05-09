package com.badahori.creatures.plugins.intellij.agenteering.caos.project

import com.badahori.creatures.plugins.intellij.agenteering.att.indices.AttFileByVariantIndex
import com.badahori.creatures.plugins.intellij.agenteering.att.indices.AttFilesIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.library.CaosBundleSourcesRegistrationUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.library.CaosSdkProjectRootsChangeListener
import com.badahori.creatures.plugins.intellij.agenteering.sprites.indices.BreedSpriteIndex
import com.badahori.creatures.plugins.intellij.agenteering.utils.getModule
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
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope


class CaosProjectStartupActivity : StartupActivity {

    private var project:Project? = null

    private val callback = object : FileEditorManagerListener {
        override fun fileOpened(editorManager: FileEditorManager, file: VirtualFile) {
            registerOnAny()
        }
    }


    override fun runActivity(project: Project) {
        this.project = project
        registerFileOpenHandler(project)
        registerProjectRootChangeListener(project)
        registerFileSaveHandler()
        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart {
                registerOnAny()
            }
        } else {
            registerOnAny()
        }
    }

    private fun registerFileOpenHandler(project: Project) {
        project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, callback)
    }

    private fun registerFileSaveHandler() {
        ApplicationManager.getApplication().messageBus.connect().subscribe(AppTopics.FILE_DOCUMENT_SYNC, object : FileDocumentManagerListener {
            override fun beforeDocumentSaving(document: Document) {
                super.beforeDocumentSaving(document)
                val virtualFile = document.virtualFile
                        ?: return
                val caosVirtualFile = virtualFile as? CaosVirtualFile
                        ?: return
                CaosVirtualFileSystem.instance.fireOnSaveEvent(caosVirtualFile)
            }
        })
    }

    private fun hasAnyCaosFiles(project: Project): Boolean {
        return ATTACH_SOURCES_IF_FILE_TYPE_LIST.any { extension ->
            FilenameIndex.getAllFilesByExt(project, extension).isNotEmpty()
        }
    }

    private fun registerProjectRootChangeListener(project: Project) {
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

    private fun indexAttAndSpriteFiles(project: Project) {
        BreedSpriteIndex.indexOnce(project, GlobalSearchScope.projectScope(project))
        AttFileByVariantIndex.indexOnce(project, GlobalSearchScope.projectScope(project))
        AttFilesIndex.indexOnce(project, GlobalSearchScope.projectScope(project))
    }

    companion object {
        //private val LOGGER = Logger.getLogger("#" + CaosProjectStartupActivity::class.java)

        private val ATTACH_SOURCES_IF_FILE_TYPE_LIST = listOf("cos", "cob", "agent", "agents")
    }
}