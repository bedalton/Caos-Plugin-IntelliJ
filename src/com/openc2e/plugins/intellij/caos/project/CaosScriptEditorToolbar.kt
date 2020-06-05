package com.openc2e.plugins.intellij.caos.project

import com.intellij.ProjectTopics
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.refactoring.suggested.endOffset
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.intellij.util.ui.UIUtil.TRANSPARENT_COLOR
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFile
import com.openc2e.plugins.intellij.caos.lang.variant
import com.openc2e.plugins.intellij.caos.utils.*
import javax.swing.JPanel


class CaosScriptEditorToolbar(val project: Project) : EditorNotifications.Provider<EditorNotificationPanel>() {

    private var variant:String? = null
    override fun getKey(): Key<EditorNotificationPanel> = KEY

    init {
        project.messageBus.connect(project).subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                //notifications.updateAllNotifications()
            }
        })
    }

    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor, project: Project): EditorNotificationPanel? {
        val caosFile = file.getPsiFile(project) as? CaosScriptFile
                ?: return null
        val headerComponent = createCaosScriptHeaderComponent(caosFile)
        val panel = EditorNotificationPanel(TRANSPARENT_COLOR)
        panel.add(headerComponent)
        return panel
    }


    companion object {
        private val KEY: Key<EditorNotificationPanel> = Key.create("Caos Editor Toolbar")

    }
}

internal fun createCaosScriptHeaderComponent(caosFile: CaosScriptFile) : JPanel {
    val toolbar = EditorToolbar()
    toolbar.panel.background = TRANSPARENT_COLOR
    toolbar.addCopyOneLineListener {
        caosFile.copyAsOneLine()
    }
    toolbar.addTrimSpacesListener {
        caosFile.trimErrorSpaces()
    }
    val variant = caosFile.variant.toUpperCase()

    toolbar.selectVariant(CaosConstants.VARAINTS.indexOf(variant) + 1)
    toolbar.addVariantListener variant@{
        val selected = it.item as String
        if (caosFile.variant == selected || selected !in CaosConstants.VARAINTS)
            return@variant

        CaosScriptProjectSettings.setVariant(selected)
        //caosFile.variant = selected
        runWriteAction {
            com.intellij.psi.text.BlockSupport.getInstance(caosFile.project).reparseRange(caosFile, 0, caosFile.endOffset, caosFile.text)
        }
        DaemonCodeAnalyzer.getInstance(caosFile.project).restart(caosFile)
        toolbar.setDocsButtonEnabled(true)
    }

    toolbar.addDocsButtonClickListener {
        val selectedVariant = toolbar.selectedVariant
        val docRelativePath = "$BUNDLE_DEFINITIONS_FOLDER/$selectedVariant-Lib.caosdef"
        val virtualFile = CaosFileUtil.getPluginResourceFile(docRelativePath)
        val file = virtualFile?.getPsiFile(caosFile.project)
        if (file == null) {
            toolbar.setDocsButtonEnabled(false)
            return@addDocsButtonClickListener
        }
        file.navigate(true)
    }
    return toolbar.panel
}