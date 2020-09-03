package com.badahori.creatures.plugins.intellij.agenteering.caos.project.editor

import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptCollapseNewLineIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CollapseChar
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.library.BUNDLE_DEFINITIONS_FOLDER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptMacroLike
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRemovalScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptScriptElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.endOffset
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.injector.Injector
import com.intellij.ProjectTopics
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.runReadAction
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
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.intellij.util.ui.UIUtil.TRANSPARENT_COLOR
import java.awt.event.ItemEvent
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel


class CaosScriptEditorToolbar(val project: Project) : EditorNotifications.Provider<EditorNotificationPanel>() {

    override fun getKey(): Key<EditorNotificationPanel> = KEY

    init {
        project.messageBus.connect().subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
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
    if (caosFile.module?.variant.let { it == null || it == CaosVariant.UNKNOWN }) {
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

    val checkedSettings = JectSettings()
    toolbar.addInjectionHandler handler@{

        if (caosFile.getUserData(JectSettingsKey) == null)
            caosFile.putUserData(JectSettingsKey, checkedSettings)
        caosFile.virtualFile?.let { virtualFile ->
            val detector = CodeSmellDetector.getInstance(project)
            val smells = detector.findCodeSmells(listOf(virtualFile))
                    .filter {
                        it.severity == HighlightSeverity.ERROR
                    }
            if (smells.isNotEmpty()) {
                Injector.postError(project, "Syntax Errors", "Cannot inject caos code with known errors.")
                return@handler
            }
        }

        if (caosFile.variant?.isNotOld.orTrue()) {
            injectC3WithDialog(caosFile)
            return@handler
        }

        val content = CaosScriptCollapseNewLineIntentionAction.collapseLinesInCopy(caosFile, CollapseChar.SPACE).text
        if (content.isBlank()) {
            Injector.postInfo(project, "Empty Injection", "Empty code body was not injected")
            return@handler
        }
        val variant = caosFile.variant
        if (variant == null) {
            Injector.postError(project, "Variant error", "File variant could not be determined")
            return@handler
        }
        executeOnPooledThread {
            Injector.inject(project, variant, content)
        }
    }
    return toolbar.panel
}

private enum class JectScriptType(val type: String) {
    REMOVAL("Removal Scripts"),
    EVENT("Event Scripts"),
    INSTALL("Install Scripts")
}

private val JectSettingsKey = Key<JectSettings>("com.badahori.creature.plugins.intellij.injector.JECT_SETTINGS")

/**
 * Holds settings for Ject dialog
 */
private data class JectSettings(
        var injectRemovalScriptsSelected: Boolean = true,
        var injectEventScriptsSelected: Boolean = true,
        var injectInstallScriptsSelected: Boolean = true
)

/**
 * Initializes, builds, and displays dialog box to select script types to inject
 */
private fun injectC3WithDialog(file: CaosScriptFile) {
    val project = file.project
    val variant = file.variant
    if (variant == null) {
        DialogBuilder(project)
                .title("Variant Error")
                .apply {
                    setErrorText("No variant detected for CAOS file")
                }
                .showModal(true)
        return
    }
    val removalScripts = PsiTreeUtil.collectElementsOfType(file, CaosScriptRemovalScript::class.java)
    val eventScripts = PsiTreeUtil.collectElementsOfType(file, CaosScriptEventScript::class.java)
    val installScripts = PsiTreeUtil.collectElementsOfType(file, CaosScriptMacroLike::class.java)
    val options = mutableListOf<ScriptBundle>()
    if (removalScripts.isNotEmpty())
        options.add(ScriptBundle(JectScriptType.REMOVAL, removalScripts))
    if (eventScripts.isNotEmpty())
        options.add(ScriptBundle(JectScriptType.EVENT, eventScripts))
    if (installScripts.isNotEmpty())
        options.add(ScriptBundle(JectScriptType.INSTALL, installScripts))
    if (options.size < 0 && file.text.trim().isNotBlank()) {
        LOGGER.severe("Script is not blank, but no script elements found within")
        return
    }
    showC3InjectPanel(project, variant, file, options)
}

private data class ScriptBundle(val type: JectScriptType, val scripts: Collection<CaosScriptScriptElement>)


/**
 * Creates a popup dialog box to select the kind of scripts to inject in cases of CV+
 */
private fun showC3InjectPanel(project: Project, variant: CaosVariant, file: CaosScriptFile, scriptsIn: List<ScriptBundle>) {

    // Init panel
    val panel = JPanel()
    panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
    panel.add(JLabel("Select scripts to install"))
    val jectSettings = file.getUserData(JectSettingsKey) ?: JectSettings()
    // Filter for removal scripts and create checkbox if needed
    val removalScripts = scriptsIn.firstOrNull { it.type == JectScriptType.REMOVAL }?.scripts
    val removalScriptsCheckBox = if (removalScripts != null && removalScripts.isNotEmpty())
        JCheckBox(JectScriptType.REMOVAL.type).apply {
            this.isSelected = jectSettings.injectRemovalScriptsSelected
        }
    else
        null
    // Filter for event scripts and created checkbox if needed
    val eventScripts = scriptsIn.firstOrNull { it.type == JectScriptType.REMOVAL }?.scripts
    val eventScriptsCheckBox = if (eventScripts != null && eventScripts.isNotEmpty())
        JCheckBox(JectScriptType.EVENT.type).apply {
            this.isSelected = jectSettings.injectRemovalScriptsSelected
        }
    else
        null

    // Filter for install/macro scripts and create checkbox if needed
    val installScripts = scriptsIn.firstOrNull { it.type == JectScriptType.INSTALL }?.scripts
    val installScriptsCheckBox = if (installScripts != null && installScripts.isNotEmpty())
        JCheckBox(JectScriptType.INSTALL.type).apply {
            this.isSelected = jectSettings.injectRemovalScriptsSelected
        }
    else
        null

    // Add checkboxes and count script types
    var scriptTypes = 0
    removalScriptsCheckBox?.let { scriptTypes++; panel.add(it) }
    eventScriptsCheckBox?.let { scriptTypes++; panel.add(it) }
    installScriptsCheckBox?.let { scriptTypes++; panel.add(it) }

    // If only install scripts, run them
    if (scriptTypes < 2 && installScripts.isNotNullOrEmpty()) {
        inject(project, variant, installScripts)
        return
    }

    // Build actual injection dialog box
    DialogBuilder(project)
            .centerPanel(panel).apply {
                okActionEnabled(true)
                setOkOperation {
                    this.dialogWrapper.close(0)
                    val scriptsInOrder = mutableListOf<CaosScriptScriptElement>()
                    removalScriptsCheckBox?.isSelected?.let {
                        jectSettings.injectRemovalScriptsSelected = it
                    }
                    eventScriptsCheckBox?.isSelected?.let {
                        jectSettings.injectEventScriptsSelected = it
                    }
                    installScriptsCheckBox?.isSelected?.let {
                        jectSettings.injectInstallScriptsSelected = it
                    }
                    file.putUserData(JectSettingsKey, jectSettings)
                    // Order is important
                    if (removalScriptsCheckBox?.isSelected.orFalse()) {
                        removalScripts?.let { scriptsInOrder.addAll(it) }
                    }
                    if (eventScriptsCheckBox?.isSelected.orFalse()) {
                        eventScripts?.let { inject(project, variant, it) }
                    }
                    if (installScriptsCheckBox?.isSelected.orFalse()) {
                        installScripts?.let { inject(project, variant, it) }
                    }
                    LOGGER.info("Finished trying to inject scripts")

                }
            }.showModal(true)
}

private fun inject(project: Project, variant: CaosVariant, scripts: Collection<CaosScriptScriptElement>): Boolean {
    return runReadAction run@{
        for (script in scripts) {
            val content = script.codeBlock?.let { CaosScriptCollapseNewLineIntentionAction.collapseLinesInCopy(it, CollapseChar.SPACE).text }
                    ?: continue
            val result = if (script is CaosScriptEventScript) {
                Injector.injectEventScript(project, variant, script.family, script.genus, script.species, script.eventNumber, content)
            } else {
                Injector.inject(project, variant, content)
            }
            if (!result)
                return@run false
        }
        return@run true
    }
}