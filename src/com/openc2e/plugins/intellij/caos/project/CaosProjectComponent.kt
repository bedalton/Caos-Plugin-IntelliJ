package com.openc2e.plugins.intellij.caos.project

import com.intellij.ProjectTopics
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFileType
import com.openc2e.plugins.intellij.caos.utils.getModule
import com.openc2e.plugins.intellij.caos.utils.virtualFile
import java.util.logging.Logger


class CaosProjectComponent(project: Project) : ProjectComponent {

    init {
        registerFileOpenHandler(project)
        registerProjectRootChangeListener(project)
    }

    private fun registerFileOpenHandler(project: Project) {
        val bus = project.getMessageBus()
        bus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun fileOpened(editorManager: FileEditorManager, file: VirtualFile) {
                val extension = file.extension
                if (extension != CaosScriptFileType.DEFAULT_EXTENSION)
                    return
                val module = file.getModule(project)
                if (module != null) {
                    CaosBundleSourcesRegistrationUtil.register(module, project)
                    initFrameworkDefaults(editorManager.selectedTextEditor, file)
                }
            }
        })
    }

    private fun initFrameworkDefaults(editor: Editor?, file: VirtualFile) {
        val editorVirtualFile = editor?.virtualFile ?: return
        if (editorVirtualFile.path != file.path)
            return
        LOGGER.info("File opened is current file in current editor")

    }

    private fun registerProjectRootChangeListener(project: Project) {
        project.messageBus.connect().subscribe(ProjectTopics.PROJECT_ROOTS, CaosSdkProjectRootsChangeListener)
    }

    override fun projectOpened() {
        super.projectOpened()

    }

    companion object {
        private val LOGGER = Logger.getLogger("#" + CaosProjectComponent::class.java)
    }
}