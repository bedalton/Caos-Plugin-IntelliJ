package com.badahori.creatures.plugins.intellij.agenteering.caos.project.editor

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.library.BUNDLE_DEFINITIONS_FOLDER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.CaosConstants
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.trimErrorSpaces
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosInjectorNotifications
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.injector.Injector
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem
import com.intellij.ProjectTopics
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.util.Key
import com.intellij.openapi.vcs.CodeSmellDetector
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
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
 * Though not its original purpose, the notification provider functions as a persistent toolbar
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

    override fun createNotificationPanel(
        virtualFile: VirtualFile,
        fileEditor: FileEditor,
        project: Project
    ): EditorNotificationPanel? {
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

    val assignVariant = select@{ selected: CaosVariant? ->
        // If can inject, enable injection button
        val canInject = selected != null && Injector.canConnectToVariant(selected)
        toolbar.setInjectButtonEnabled(canInject)

        if (selected !in CaosConstants.VARIANTS) {
            toolbar.setDocsButtonEnabled(false)
            return@select
        }
        // Set default variant in settings
        CaosScriptProjectSettings.setVariant(selected!!)
        var file = pointer.element
            ?: return@select
        // Set selected variant in file
        file.variant = selected

        // Re-parse file with new variant
        runWriteAction run@{
            file = pointer.element
                ?: return@run
            try {
                BlockSupport.getInstance(project).reparseRange(file, 0, file.endOffset, file.text)
            } catch (e: Exception) {
            }
        }
        file = pointer.element
            ?: return@select
        DaemonCodeAnalyzer.getInstance(project).restart(file)

        // Enable docs button now that a variant has been set
        toolbar.setDocsButtonEnabled(true)
    }

    val listener =
        CaosFileTreeChangedListener(project, pointer, caosFile.variant ?: caosFile.module?.variant) { variant ->
            assignVariant(variant)
            toolbar.selectVariant(variant?.code ?: "")
        }
    PsiManager.getInstance(project).addPsiTreeChangeListener(listener)

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
            if (caosFile.variant == selected)
                return@variant
            assignVariant(selected)
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
    toolbar.addDocsButtonClickListener(createDocsOpenClickHandler(toolbar, pointer))

    // If the OS is not windows, and no POST url is set, hide injection button
    // show inject button
    if (!System.getProperty("os.name").contains("Windows") && CaosScriptProjectSettings.getInjectURL(project)
            .isNullOrBlank()
    ) {
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
private fun createDocsOpenClickHandler(toolbar: EditorToolbar, pointer: CaosScriptPointer): ActionListener =
    ActionListener listener@{
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
            ?: FilenameIndex.getFilesByName(
                project,
                "$selectedVariant-Lib.caosdef",
                GlobalSearchScope.allScope(caosFile.project)
            )
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
        CaosNotifications.showError(
            project,
            "Set Variant",
            "Failed to set variant to selected variant: ${variant.code}. Variant is still '${currentVariant.code}'"
        )
        LOGGER.severe("Failed to set variant to selected variant: ${variant.code}")
        return
    }
    // Rerun annotations
    DaemonCodeAnalyzer.getInstance(project).restart(theFile)
    // Show success message
    CaosNotifications.showInfo(project, "Set Variant", "Variant set to ${currentVariant.code}")
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
        if (CaosScriptProjectSettings.injectionCheckDisabled)
            return@let
        val detector = CodeSmellDetector.getInstance(project)
        val smells = detector.findCodeSmells(listOf(virtualFile))
            .filter {
                it.severity == HighlightSeverity.ERROR
            }
        if (smells.isNotEmpty()) {
            CaosInjectorNotifications
                .createErrorNotification(project, "Syntax Errors", "Cannot inject CAOS code with known errors.")
                .addAction(object : AnAction("Ignore for session and inject") {
                    override fun actionPerformed(e: AnActionEvent) {
                        CaosScriptProjectSettings.injectionCheckDisabled = true
                        injectActual(project, variant, caosFile)
                    }
                })
                .addAction(object : AnAction("Inject Anyways") {
                    override fun actionPerformed(e: AnActionEvent) {
                        injectActual(project, variant, caosFile)
                    }
                })
                .show()
            return@handler
        }
    }
    injectActual(project, variant, caosFile)
}

private fun injectActual(project: Project, variant: CaosVariant, caosFile: CaosScriptFile) {
    // If variant is CV+ ask which parts of the file to inject
    if (caosFile.variant?.isNotOld.orTrue()) {
        injectC3WithDialog(caosFile)
        return
    }

    // Get contents of file and format for injection
    WriteCommandAction.writeCommandAction(project)
        .withUndoConfirmationPolicy(UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION)
        .shouldRecordActionForActiveDocument(false)
        .run<Throwable> run@{
            val content = caosFile.text
                ?: return@run
            if (content.isBlank()) {
                Injector.postInfo(project, "Empty Injection", "Empty code body was not injected")
                return@run
            }
            // Add inject command to thread pool
            executeOnPooledThread {
                Injector.inject(project, variant, content)
            }
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
private fun showC3InjectPanel(
    project: Project,
    variant: CaosVariant,
    file: CaosScriptFile,
    scriptsIn: List<ScriptBundle>
) {

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
            val content = script.codeBlock?.text
                ?: continue
            val result = if (script is CaosScriptEventScript) {
                Injector.injectEventScript(
                    project,
                    variant,
                    script.family,
                    script.genus,
                    script.species,
                    script.eventNumber,
                    content
                )
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
        get() = try {
            if (virtualFile.isValid)
                pointer.element ?: (PsiManager.getInstance(project).findFile(virtualFile) as? CaosScriptFile)
            else
                null
        } catch (e: Exception) {
            null
        }
}

private typealias OnVariantChangeListener = (variant: CaosVariant?) -> Unit


private class CaosFileTreeChangedListener(
    private var project: Project?,
    private var pointer: CaosScriptPointer?,
    private var currentVariant: CaosVariant?,
    private var variantChangedListener: OnVariantChangeListener?
) : PsiTreeChangeListener, Disposable {

    override fun beforeChildAddition(event: PsiTreeChangeEvent) {
    }

    override fun beforeChildRemoval(event: PsiTreeChangeEvent) {
    }

    override fun beforeChildReplacement(event: PsiTreeChangeEvent) {
    }

    override fun beforeChildMovement(event: PsiTreeChangeEvent) {
    }

    override fun beforeChildrenChange(event: PsiTreeChangeEvent) {
    }

    override fun beforePropertyChange(event: PsiTreeChangeEvent) {
    }

    override fun childAdded(event: PsiTreeChangeEvent) {
        onChange(event.child)
    }

    override fun childRemoved(event: PsiTreeChangeEvent) {
        onChange(event.child)
    }

    override fun childReplaced(event: PsiTreeChangeEvent) {
        onChange(event.child)
    }

    override fun childrenChanged(event: PsiTreeChangeEvent) {
        onChange(event.child)
    }

    override fun childMoved(event: PsiTreeChangeEvent) {
    }

    override fun propertyChanged(event: PsiTreeChangeEvent) {
    }

    override fun dispose() {
        val theProject = project
            ?: return
        project = null
        pointer = null
        currentVariant = null
        variantChangedListener = null
        PsiManager.getInstance(theProject).removePsiTreeChangeListener(this)
    }

    private fun onChange(child: PsiElement?) {
        try {
            val associatedFile = pointer?.element
                    ?: return dispose()
            if (!child?.containingFile?.isEquivalentTo(associatedFile).orFalse())
                return
            val block = child?.getSelfOrParentOfType(CaosScriptCaos2Block::class.java)
                ?: return
            val newVariant = block.caos2Variant
            if (newVariant == currentVariant) {
                return
            }
            currentVariant = newVariant ?: associatedFile.variant
            invokeLater {
                runWriteAction {
                    variantChangedListener?.let { it(newVariant) }
                }
            }
        } catch (e: PsiInvalidElementAccessException) {

        }
    }
}