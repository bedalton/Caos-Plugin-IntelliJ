package com.badahori.creatures.plugins.intellij.agenteering.caos.project.editor

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.actions.CompileCaos2CobAction
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Pray
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.editor.PrayCompilerToolbarActions
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.library.BUNDLE_DEFINITIONS_FOLDER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptMacroLike
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRemovalScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.injectionCheckDisabled
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.settings
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.CaosConstants
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.addVariantChangeListener
import com.badahori.creatures.plugins.intellij.agenteering.injector.*
import com.badahori.creatures.plugins.intellij.agenteering.injector.postError
import com.badahori.creatures.plugins.intellij.agenteering.injector.postInfo
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.vcs.CodeSmellDetector
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.text.BlockSupport
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ui.UIUtil.TRANSPARENT_COLOR
import java.awt.event.ActionListener
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.JPanel


@Suppress("unused")
internal fun oldCreateCaosScriptHeaderComponent(virtualFile: VirtualFile, caosFile: CaosScriptFile): JPanel {

    val toolbar = EditorToolbar()
    val project = caosFile.project

    // Create a pointer to this file for use later
    val pointer = CaosScriptPointer(virtualFile, caosFile)

    // Initialize toolbar with color
    toolbar.panel.background = TRANSPARENT_COLOR
    val currentIsCaos2 = caosFile.caos2
    toolbar.compilerSettings.isVisible = currentIsCaos2 != null
    toolbar.compilerOptionsButton.isVisible = currentIsCaos2 == CAOS2Pray
    // Add handler for trim spaces button
    toolbar.compileButton.addClickListener { e ->
        if (e.isButton1) {
            val file = pointer.element
                ?: return@addClickListener
            if (file.isCaos2Cob) {
                CompileCaos2CobAction.compile(project, file)
            } else if (file.isCaos2Pray) {
                PrayCompilerToolbarActions.compile(project, caosFile)
            } else {
                LOGGER.severe("CAOS2Compile button pressed with non-CAOS2 file")
            }
        }
    }

    toolbar.compilerOptionsButton.addClickListener { e ->
        if (e.isButton1) {
            PrayCompilerToolbarActions.requestUpdatedOpts(caosFile)
        }
    }

    val assignVariant = select@{ selected: CaosVariant? ->
        // If script can be injected, enable injection button
        val canInject = selected != null && Injector.canConnectToVariant(selected)
        toolbar.setInjectButtonEnabled(canInject)


        if (selected !in CaosConstants.VARIANTS) {
            toolbar.setDocsButtonEnabled(false)
            return@select
        }

        toolbar.compilerOptionsButton.isVisible = selected?.isNotOld == true

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

    // Add variant change listener
    val listener =
        caosFile.addVariantChangeListener { variant ->
            assignVariant(variant)
            toolbar.selectVariant(variant?.code ?: "")
            updateCAOS2CobButtons(caosFile, toolbar)
        }
    PsiManager.getInstance(project).addPsiTreeChangeListener(listener)


    // Add Caos2 change listener
    val caos2Listener = caosFile.addCaos2ChangeListener { caos2String ->
        toolbar.compilerSettings.isVisible = caos2String != null
        toolbar.compilerOptionsButton.isVisible = caos2String != CAOS2Pray
    }
    if (caos2Listener != null) {
        PsiManager.getInstance(project).addPsiTreeChangeListener(caos2Listener)
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
            // If state change is not selection (i.e. deselect) return
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

/**
 * Reparse CAOS file after setting variant
 */
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
        postError(project, "Variant error", "File variant could not be determined")
        return@handler
    }

    // Persist JECT settings into file
    if (caosFile.getUserData(JectSettingsKey) == null)
        caosFile.putUserData(JectSettingsKey, checkedSettings)

    // If virtual file is valid run check for validity
    caosFile.virtualFile?.let { virtualFile ->
        if (CaosScriptProjectSettings.injectionCheckDisabled || project.settings.injectionCheckDisabled)
            return@let
        val detector = CodeSmellDetector.getInstance(project)
        val smells = detector.findCodeSmells(listOf(virtualFile))
            .filter {
                it.severity == HighlightSeverity.ERROR
            }
        if (smells.isNotEmpty()) {
            CaosInjectorNotifications
                .createErrorNotification(project, "Syntax Errors", "Cannot inject CAOS code with known errors.")
                .addAction(object : AnAction("Ignore for Session and Inject") {
                    override fun actionPerformed(e: AnActionEvent) {
                        CaosScriptProjectSettings.injectionCheckDisabled = true
                        project.settings.injectionCheckDisabled = true
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
    if (caosFile.variant?.isNotOld.orTrue() || caosFile.isCaos2Cob) {
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
                postInfo(project, "Empty Injection", "Empty code body was not injected")
                return@run
            }
            // Add inject command to thread pool
            executeOnPooledThread {
                Injector.inject(project, variant, GameInterfaceName(variant), caosFile, 7)
            }
        }

}


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
    showC3InjectPanel(project, GameInterfaceName(variant), file, options)
}

/**
 * Shows/Hides/Enables the CAOS2Cob button if file CAOS2Compiler status changes
 */
private fun updateCAOS2CobButtons(file: CaosScriptFile, toolbar: EditorToolbar) {
    val isCaos2 = file.isCaos2Cob || file.isCaos2Pray
    toolbar.compilerSettings.isVisible = isCaos2
    toolbar.compileButton.isEnabled = isCaos2
}
