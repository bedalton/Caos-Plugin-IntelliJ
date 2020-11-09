package com.badahori.creatures.plugins.intellij.agenteering.caos.project.editor

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.generator.CaosDefinitionsGenerator
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
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.CaosConstants
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.trimErrorSpaces
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.injector.Injector
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem
import com.intellij.ProjectTopics
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.util.Key
import com.intellij.openapi.vcs.CodeSmellDetector
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.text.BlockSupport
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.intellij.util.ui.UIUtil.TRANSPARENT_COLOR
import java.awt.event.ActionListener
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel


/**
 * A editor notification provider
 * This is hijacked here to provide a persistent toolbar
 */
class CaosScriptEditorToolbar(val project: Project) : EditorNotifications.Provider<EditorNotificationPanel>() {

    override fun getKey(): Key<EditorNotificationPanel> = KEY

    init {
        project.messageBus.connect().subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                //notifications.updateAllNotifications()
            }
        })
    }

    override fun createNotificationPanel(virtualFile: VirtualFile, fileEditor: FileEditor, project: Project): EditorNotificationPanel? {
        val caosFile = virtualFile.getPsiFile(project) as? CaosScriptFile
                ?: return null
        val headerComponent = createCaosScriptHeaderComponent(virtualFile, caosFile)
        val panel = EditorNotificationPanel(TRANSPARENT_COLOR)
        panel.add(headerComponent)
        return panel
    }

    companion object {
        private val KEY: Key<EditorNotificationPanel> = Key.create("Caos Editor Toolbar")

    }
}

internal fun createCaosScriptHeaderComponent(virtualFile: VirtualFile, caosFile: CaosScriptFile): JPanel {

    val toolbar = EditorToolbar()
    val project = caosFile.project

    // Create a pointer to this file for use later
    val pointer = CaosScriptPointer(virtualFile, caosFile)

    // Initialize toolbar with color
    toolbar.panel.background = TRANSPARENT_COLOR

    // Add handler for trim spaces button
    toolbar.addTrimSpacesListener {
        pointer.element?.trimErrorSpaces()
    }

    // If variant is set, ensure that CAOS def has been generated
    caosFile.variant?.let {variant ->
        CaosDefinitionsGenerator.ensureVariantCaosDef(variant)
    }

    // If variant is unknown, allow for variant selection
    if (caosFile.module?.variant.let { it == null || it == CaosVariant.UNKNOWN } && caosFile.virtualFile !is CaosVirtualFile) {
        // Make variant selector visible
        toolbar.setVariantIsVisible(true)

        // Set existing variant if any
        caosFile.variant.let {
            toolbar.selectVariant(CaosConstants.VARIANTS.indexOf(it))
        }

        // Add variant change listener
        toolbar.addVariantListener variant@{
            // If state change is not selection (ie. deselect) return
            if (it.stateChange != ItemEvent.SELECTED)
                return@variant
            // Get the selected variant
            val selected = CaosVariant.fromVal(it.item as String)
            if (selected !in CaosConstants.VARIANTS) {
                toolbar.setDocsButtonEnabled(false)
                LOGGER.severe("Unhandled CAOS variant: ${selected.code}")
                return@variant
            }
            if (caosFile.variant == selected)
                return@variant
            // Ensure CAOSDef for variant has been created
            CaosDefinitionsGenerator.ensureVariantCaosDef(selected)
            // If can inject, enable injection button
            val canInject = Injector.canConnectToVariant(selected)
            toolbar.setInjectButtonEnabled(canInject)
            // Set default variant in settings
            CaosScriptProjectSettings.setVariant(selected)

            // Set selected variant in file
            caosFile.variant = selected

            // Re-parse file with new variant
            runWriteAction {
                try {
                    BlockSupport.getInstance(project).reparseRange(caosFile, 0, caosFile.endOffset, caosFile.text)
                } catch (e: Exception) {
                }
            }
            DaemonCodeAnalyzer.getInstance(project).restart(caosFile)

            // Enable docs button now that a variant has been set
            toolbar.setDocsButtonEnabled(true)
        }
    }

    // If variant selection is not firmly set, allow for it to be altered by user
    if (caosFile.module?.variant.let { it == null || it == CaosVariant.UNKNOWN } && caosFile.virtualFile !is CaosVirtualFile) {
        initToolbarWithVariantSelect(toolbar, pointer)
    } else {
        // If variant is set in module hide variant select
        toolbar.setVariantIsVisible(false)
    }

    // Set docs button click handler
    toolbar.addDocsButtonClickListener {
        val selectedVariant = caosFile.variant
        if (selectedVariant == null) {
            val builder = DialogBuilder(caosFile.project)
            builder.setErrorText("Failed to access module game variant")
            builder.title("Variant Error")
            builder.show()
            return@addDocsButtonClickListener
        }

        runWriteAction {
            CaosDefinitionsGenerator.ensureVariantCaosDef(selectedVariant)
        }

        // Set assumed doc path
        val docRelativePath = "$BUNDLE_DEFINITIONS_FOLDER/${selectedVariant.code}-Lib.caosdef"
        LOGGER.info("Looking up CAOS DOC at '$docRelativePath'")

        // Get the virtual file for it at that path, starting with the CAOS VFS
        val docVirtualFile = CaosVirtualFileSystem.instance.findFileByPath(docRelativePath)
                // File not found in CAOS virtual file system, try to find it in default system
                ?: CaosFileUtil.getPluginResourceFile(docRelativePath)
        // Get CAOS Def PSI file for navigation
        val file = docVirtualFile?.getPsiFile(project)
                ?: FilenameIndex.getFilesByName(project, "${selectedVariant}-Lib.caosdef", GlobalSearchScope.allScope(caosFile.project))
                        .firstOrNull()
        // If file CAOS def file could not be found, disable Docs button
        if (file == null) {
            toolbar.setDocsButtonEnabled(false)
            return@addDocsButtonClickListener
        }
        // Navigate to the docs psi file
        file.navigate(true)
    }

    // If the OS is not windows, and no POST url is set, hide injection button
    // show inject button
    if (!System.getProperty("os.name").contains("Windows") && CaosScriptProjectSettings.getInjectURL(project).isNullOrBlank()) {
        toolbar.showInjectionButton(false)
    }

    // Add injection handler to toolbar
    val checkedSettings = JectSettings()
    toolbar.addInjectionHandler(createInjectHandler(pointer, checkedSettings))
    return toolbar.panel
}

private fun initToolbarWithVariantSelect(toolbar: EditorToolbar, pointer: CaosScriptPointer) {
    toolbar.setVariantIsVisible(true)
    pointer.element?.variant?.let {
        toolbar.selectVariant(CaosConstants.VARIANTS.indexOf(it))
    }

    toolbar.addVariantListener(createVariantSelectHandler(toolbar, pointer))
}

/**
 * Create action handler to handle the opening of the CAOS doc on click
 */
private fun createDocsOpenClickHandler(toolbar: EditorToolbar, pointer: CaosScriptPointer) = ActionListener listener@{
    // Get file from pointer
    val caosFile = pointer.element

    // If pointer was invalidated, return
    if (caosFile == null) {
        LOGGER.warning("CaosScript pointer was invalidated before show DOCS")
        return@listener
    }

    // Get project
    val project = caosFile.project

    // Get variant from current file
    val selectedVariant = caosFile.variant

    // If variant is null, show error message and abort
    if (selectedVariant == null) {
        toolbar.setDocsButtonEnabled(false)
        val builder = DialogBuilder(project)
        builder.setErrorText("Failed to access module game variant")
        builder.title("Variant Error")
        builder.show()
        return@listener
    }
    // Get path to documents
    val docRelativePath = "$BUNDLE_DEFINITIONS_FOLDER/$selectedVariant-Lib.caosdef"
    // Load document virtual file
    val virtualFile = CaosVirtualFileSystem.instance.findFileByPath(docRelativePath)
            ?: CaosFileUtil.getPluginResourceFile(docRelativePath)

    // Fetch psi file from virtual file
    val file = virtualFile?.getPsiFile(project)
            ?: FilenameIndex.getFilesByName(project, "$selectedVariant-Lib.caosdef", GlobalSearchScope.allScope(caosFile.project))
                    .firstOrNull()

    // If failed to find variant docs, disable button and return
    if (file == null) {
        toolbar.setDocsButtonEnabled(false)
        return@listener
    }

    // Navigate to Docs.
    file.navigate(true)
}

/**
 * Create handler for variant select in dropdown menu
 */
private fun createVariantSelectHandler(toolbar: EditorToolbar, pointer: CaosScriptPointer) = ItemListener listener@{
    // Make sure that the action comes from SELECTION and not de-selection
    if (it.stateChange != ItemEvent.SELECTED)
        return@listener
    // Get file from pointer
    val caosFile = pointer.element

    // If pointer was invalidated. Return
    if (caosFile == null) {
        LOGGER.warning("Caos script reference was invalidated before variant change handler")
        return@listener
    }
    // Get project
    val project = caosFile.project

    // Parse selection from string value
    val selected = CaosVariant.fromVal(it.item as String)

    // If variant is invalid or already selected, return
    if (caosFile.variant == selected || selected !in CaosConstants.VARIANTS)
        return@listener

    // If variant is valid, enable DOCs button
    toolbar.setDocsButtonEnabled(true)

    // Set inject active/disabled if game is running
    val canInject = Injector.canConnectToVariant(selected)
    toolbar.setInjectButtonEnabled(canInject)

    // Set project level variant based on selection
    CaosScriptProjectSettings.setVariant(selected)

    // Set file variant
    caosFile.variant = selected

    // Try to re-parse file with the new variant
    try {
        WriteCommandAction.writeCommandAction(project)
                .withName("Set CAOS variant ${selected.code}")
                .withGroupId("CaosScript")
                .shouldRecordActionForActiveDocument(false)
                .run<Throwable> {
                    reparseAfterSet(pointer, selected)
                }
    } catch (e: Exception) {
        LOGGER.severe("Failed to re-parse and re-analyze after variant change with error: '${e.message}'")
        e.printStackTrace()
    }
}

private fun reparseAfterSet(pointer: CaosScriptPointer, variant: CaosVariant) {
    // Ensure pointer is still valid
    val theFile = pointer.element
    // IF pointer was invalidated, return
    if (theFile == null || !theFile.isValid) {
        LOGGER.severe("Cannot re-parse CAOS file as it has become invalid")
        return
    }
    val project = theFile.project
    try {
        // Re-parse script to clear/add errors
        BlockSupport.getInstance(project).reparseRange(theFile, 0, theFile.endOffset, theFile.text)
    } catch (e: Exception) {
        LOGGER.severe("Failed to re-parse after variant change with error: '${e.message}'")
        e.printStackTrace()
        return
    }

    // Get current file variant after set
    val currentVariant = pointer.element?.variant
    if (currentVariant == null) {
        CaosNotifications.showError(
                project,
                "Set Variant",
                "Failed to set variant to selected variant: ${variant.code}. Variant is still 'NULL'"
        )
        return
    }
    // Check that the caos file actually did have its variant set.
    if (currentVariant != variant) {
        CaosNotifications.showError(project, "Set Variant", "Failed to set variant to selected variant: ${variant.code}. Variant is still '${currentVariant.code}'")
        LOGGER.severe("Failed to set variant to selected variant: ${variant.code}")
        return
    }
    // Rerun annotations
    DaemonCodeAnalyzer.getInstance(project).restart(theFile)
    // Show success message
    CaosNotifications.showInfo(project, "Set Variant", "Did set variant to ${currentVariant.code}")
}

/**
 * Creates an action handler to inject CAOS code into the Creatures game variant
 */
private fun createInjectHandler(pointer: CaosScriptPointer, checkedSettings: JectSettings) = ActionListener handler@{
    // Get file if valid
    val caosFile = pointer.element
    // If file was invalidated, return
    if (caosFile == null) {
        LOGGER.warning("CAOS file pointer became invalid before injection")
        return@handler
    }

    // Get project
    val project = caosFile.project

    // If variant of file cannot be determined, abort
    val variant = caosFile.variant
    if (variant == null) {
        Injector.postError(project, "Variant error", "File variant could not be determined")
        return@handler
    }

    // Persist JECT settings into file
    if (caosFile.getUserData(JectSettingsKey) == null)
        caosFile.putUserData(JectSettingsKey, checkedSettings)

    // If virtual file is valid run check for validity
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

    // If variant is CV+ ask which parts of the file to inject
    if (caosFile.variant?.isNotOld.orTrue()) {
        injectC3WithDialog(caosFile)
        return@handler
    }

    // Get contents of file and format for injection
    val content = CaosScriptCollapseNewLineIntentionAction.collapseLinesInCopy(caosFile, CollapseChar.SPACE).text
    if (content.isBlank()) {
        Injector.postInfo(project, "Empty Injection", "Empty code body was not injected")
        return@handler
    }

    // Add inject command to thread pool
    executeOnPooledThread {
        Injector.inject(project, variant, content)
    }
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
    val removalScriptsCheckBox = if (removalScripts.isNotNullOrEmpty())
        JCheckBox(JectScriptType.REMOVAL.type).apply {
            this.isSelected = jectSettings.injectRemovalScriptsSelected
        }
    else
        null
    // Filter for event scripts and created checkbox if needed
    val eventScripts = scriptsIn.firstOrNull { it.type == JectScriptType.EVENT }?.scripts
    val eventScriptsCheckBox = if (eventScripts.isNotNullOrEmpty())
        JCheckBox(JectScriptType.EVENT.type).apply {
            this.isSelected = jectSettings.injectEventScriptsSelected
        }
    else
        null

    // Filter for install/macro scripts and create checkbox if needed
    val installScripts = scriptsIn.firstOrNull { it.type == JectScriptType.INSTALL }?.scripts
    val installScriptsCheckBox = if (installScripts.isNotNullOrEmpty())
        JCheckBox(JectScriptType.INSTALL.type).apply {
            this.isSelected = jectSettings.injectInstallScriptsSelected
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

/**
 * Creates a stronger pointer for use in toolbar actions
 */
private class CaosScriptPointer(private val virtualFile: VirtualFile, caosFileIn: CaosScriptFile) {
    private val pointer = SmartPointerManager.createPointer(caosFileIn)
    private val project: Project = caosFileIn.project
    val element: CaosScriptFile?
        get() = pointer.element ?: (PsiManager.getInstance(project).findFile(virtualFile) as? CaosScriptFile)
}