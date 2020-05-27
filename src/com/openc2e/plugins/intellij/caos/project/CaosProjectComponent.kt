package com.openc2e.plugins.intellij.caos.project

import com.intellij.ProjectTopics
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.openc2e.plugins.intellij.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFile
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFileType
import com.openc2e.plugins.intellij.caos.utils.*
import java.util.logging.Logger
import javax.swing.JPanel


class CaosProjectComponent(project: Project) : ProjectComponent {

    init {
        registerFileOpenHandler(project)
        registerProjectRootChangeListener(project)
    }

    private fun registerFileOpenHandler(project: Project) {
        val bus = project.getMessageBus()
        bus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun fileOpened(editorManager: FileEditorManager, file: VirtualFile) {
                val psiFile = file.getPsiFile(project)
                if (psiFile is CaosScriptFile) {
                    onCaosFileOpened(project, editorManager, file, psiFile)
                }

            }
        })
    }

    private fun onCaosFileOpened(project:Project, editorManager: FileEditorManager, file:VirtualFile, caosFile: CaosScriptFile) {
        val module = file.getModule(project)
        if (module != null) {
            CaosBundleSourcesRegistrationUtil.register(module, project)
            initFrameworkDefaults(editorManager.selectedTextEditor, file)
        } else {
            LOGGER.info("Failed to locate CAOS module")
        }
        /*
        val editor = editorManager.selectedTextEditor
                ?: return
        editor.headerComponent = getCaosScriptHeaderComponent(caosFile)
        editor.contentComponent.add(getCaosScriptHeaderComponent(caosFile), 0)*/
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