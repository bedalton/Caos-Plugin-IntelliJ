package com.badahori.creatures.plugins.intellij.agenteering.caos.project.editor

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.library.BUNDLE_DEFINITIONS_FOLDER
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.*
import com.intellij.ProjectTopics
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.suggested.endOffset
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.intellij.util.ui.UIUtil.TRANSPARENT_COLOR
import javax.swing.JPanel


class CaosScriptEditorToolbar(val project: Project) : EditorNotifications.Provider<EditorNotificationPanel>() {

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
    val variant = caosFile.variant

    toolbar.selectVariant(CaosConstants.VARIANTS.indexOf(variant) + 1)
    toolbar.addVariantListener variant@{
        val selected = CaosVariant.fromVal(it.item as String)
        if (caosFile.variant == selected || selected !in CaosConstants.VARIANTS)
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
                ?: FilenameIndex.getFilesByName(caosFile.project, "$selectedVariant-Lib.caosdef", GlobalSearchScope.allScope(caosFile.project))
                        .firstOrNull()
        if (file == null) {
            toolbar.setDocsButtonEnabled(false)
            return@addDocsButtonClickListener
        }
        file.navigate(true)
    }
    return toolbar.panel
}