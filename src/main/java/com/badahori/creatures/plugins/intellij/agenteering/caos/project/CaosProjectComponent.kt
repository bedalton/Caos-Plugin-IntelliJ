package com.badahori.creatures.plugins.intellij.agenteering.caos.project

import com.badahori.creatures.plugins.intellij.agenteering.caos.project.library.CaosBundleSourcesRegistrationUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.library.CaosSdkProjectRootsChangeListener
import com.badahori.creatures.plugins.intellij.agenteering.utils.getModule
import com.badahori.creatures.plugins.intellij.agenteering.utils.virtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem
import com.intellij.AppTopics
import com.intellij.ProjectTopics
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import java.util.logging.Logger


class CaosProjectComponent(private val project: Project) : ProjectComponent {

    init {
        registerFileOpenHandler(project)
        registerProjectRootChangeListener(project)
        registerFileSaveHandler()
    }

    private fun registerFileOpenHandler(project: Project) {
        val bus = project.messageBus
        bus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun fileOpened(editorManager: FileEditorManager, file: VirtualFile) {
                registerOnAny()
            }
        })
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

    override fun projectOpened() {
        super.projectOpened()
        registerOnAny()
    }

    private fun registerOnAny() {
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

    companion object {
        private val LOGGER = Logger.getLogger("#" + CaosProjectComponent::class.java)

        private val ATTACH_SOURCES_IF_FILE_TYPE_LIST = listOf("cos", "cob", "agent")

        internal fun registerSourcesOnFileOpen(project: Project, file: VirtualFile) {
            val module = file.getModule(project)
            if (module != null) {
                CaosBundleSourcesRegistrationUtil.register(module, project)
            }
        }
    }
}