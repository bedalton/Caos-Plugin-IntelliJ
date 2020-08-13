package com.badahori.creatures.plugins.intellij.agenteering.caos.project.editor

import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptCollapseNewLineIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CollapseChar
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.library.BUNDLE_DEFINITIONS_FOLDER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.endOffset
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.injector.Injector
import com.intellij.ProjectTopics
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.util.Key
import com.intellij.openapi.vcs.CodeSmellDetector
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.text.BlockSupport
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.intellij.util.ui.UIUtil.TRANSPARENT_COLOR
import java.awt.event.ItemEvent
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

internal fun createCaosScriptHeaderComponent(caosFile: CaosScriptFile): JPanel {
    val toolbar = EditorToolbar()
    val project = caosFile.project
    toolbar.panel.background = TRANSPARENT_COLOR
    toolbar.addTrimSpacesListener {
        caosFile.trimErrorSpaces()
    }
    if (caosFile.module?.variant.let { it == null || it == CaosVariant.UNKNOWN}) {
        toolbar.setVariantIsVisible(true)
        caosFile.variant.let {
            toolbar.selectVariant(CaosConstants.VARIANTS.indexOf(it))
        }

        toolbar.addVariantListener variant@{
            if (it.stateChange != ItemEvent.SELECTED)
                return@variant
            val selected = CaosVariant.fromVal(it.item as String)
            if (caosFile.variant == selected || selected !in CaosConstants.VARIANTS)
                return@variant
            val canInject = Injector.canConnectToVariant(selected)
            toolbar.setInjectButtonEnabled(canInject)
            CaosScriptProjectSettings.setVariant(selected)
            caosFile.variant = selected
            runWriteAction {
                try {
                    BlockSupport.getInstance(project).reparseRange(caosFile, 0, caosFile.endOffset, caosFile.text)
                } catch (e: Exception) {
                }
            }
            DaemonCodeAnalyzer.getInstance(project).restart(caosFile)
            toolbar.setDocsButtonEnabled(true)
        }

    } else {
        toolbar.setVariantIsVisible(false)
    }
    toolbar.addDocsButtonClickListener {
        val selectedVariant = caosFile.variant
        if (selectedVariant == null) {
            val builder = DialogBuilder(caosFile.project)
            builder.setErrorText("Failed to access module game variant")
            builder.title("Variant Error")
            builder.show()
            return@addDocsButtonClickListener
        }
        val docRelativePath = "$BUNDLE_DEFINITIONS_FOLDER/$selectedVariant-Lib.caosdef"
        val virtualFile = CaosFileUtil.getPluginResourceFile(docRelativePath)
        val file = virtualFile?.getPsiFile(project)
                ?: FilenameIndex.getFilesByName(project, "$selectedVariant-Lib.caosdef", GlobalSearchScope.allScope(caosFile.project))
                        .firstOrNull()
        if (file == null) {
            toolbar.setDocsButtonEnabled(false)
            return@addDocsButtonClickListener
        }
        file.navigate(true)
    }
    if (!System.getProperty("os.name").contains("Windows") && CaosScriptProjectSettings.getInjectURL(project).isNullOrBlank()) {
        toolbar.showInjectionButton(false)
    }

    toolbar.addInjectionHandler handler@{
        val content = CaosScriptCollapseNewLineIntentionAction.collapseLinesInCopy(caosFile, CollapseChar.SPACE).text
        if (content.isBlank()) {
            Injector.postInfo(project, "Empty Injection", "Empty code body was not injected");
            return@handler
        }
        caosFile.virtualFile?.let {
            val detector = CodeSmellDetector.getInstance(project)
            val smells = detector.findCodeSmells(listOf(it))
                    .filter {
                        it.severity == HighlightSeverity.ERROR
                    }
            if (smells.isNotEmpty()) {
                Injector.postError(project, "Syntax Errors", "Cannot inject caos code with known errors.")
                return@handler
            }
        }
        val variant = caosFile.variant
        if (variant == null) {
            Injector.postError(project, "Variant error", "File variant could not be determined")
            return@handler
        }
        Injector.inject(project, variant, content)
    }
    return toolbar.panel
}