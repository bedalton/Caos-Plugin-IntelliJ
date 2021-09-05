package com.badahori.creatures.plugins.intellij.agenteering.caos.action

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Cob
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEventScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptMacroLike
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRemovalScript
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptScriptElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosScriptProjectSettings
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.injectionCheckDisabled
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.settings
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosInjectorNotifications
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.injector.Injector
import com.badahori.creatures.plugins.intellij.agenteering.injector.postInfo
import com.badahori.creatures.plugins.intellij.agenteering.utils.executeOnPooledThread
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.isNotNullOrEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.icons.AllIcons
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.util.Key
import com.intellij.openapi.vcs.CodeSmellDetector
import com.intellij.psi.util.PsiTreeUtil
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel

@Suppress("ComponentNotRegistered")
internal class InjectCaosAction: AnAction(
    "Inject CAOS",
    "Inject CAOS into game",
    AllIcons.Toolwindows.ToolWindowRun
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
            ?: return
        val files = e.files
        if (files.isEmpty())
            return
        if (files.size > 1) {
            throw Exception("Only one file can be injected at a time")
        }
        val caosFile = files[0].getPsiFile(project) as? CaosScriptFile
            ?: return
        val variant = caosFile.variant
            ?: return

        caosInject(project, variant, GameInterfaceName(variant), caosFile)
    }
}


internal fun caosInject(
    project: Project,
    variant: CaosVariant,
    gameInterfaceName: GameInterfaceName,
    caosFile: CaosScriptFile
) {

    val checkedSettings = JectSettings()
    // Persist JECT settings into file
    if (caosFile.getUserData(JectSettingsKey) == null)
        caosFile.putUserData(JectSettingsKey, checkedSettings)

    // If virtual file is valid run check for validity
    caosFile.virtualFile?.let { virtualFile ->
        if (CaosScriptProjectSettings.injectionCheckDisabled || project.settings.injectionCheckDisabled)
            return
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
                        injectActual(project, variant, gameInterfaceName, caosFile)
                    }
                })
                .addAction(object : AnAction("Inject Anyways") {
                    override fun actionPerformed(e: AnActionEvent) {
                        injectActual(project, variant, gameInterfaceName, caosFile)
                    }
                })
                .show()
            return
        }
    }

    injectActual(project, variant, gameInterfaceName, caosFile)
}

private fun injectActual(
    project: Project,
    variant: CaosVariant,
    gameInterfaceName: GameInterfaceName,
    caosFile: CaosScriptFile
) {
    // If variant is CV+ ask which parts of the file to inject
    if (caosFile.variant?.isNotOld != false || caosFile.isCaos2Cob) {
        injectC3WithDialog(caosFile, gameInterfaceName)
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
                @Suppress("ComponentNotRegistered")
                Injector.inject(
                    project = project,
                    fallbackVariant = variant,
                    gameInterfaceName = gameInterfaceName,
                    caosFile = caosFile,
                    jectFlags = 7,
                    tryJect = false
                )
            }
        }

}

internal data class ScriptBundle(val type: JectScriptType, val scripts: Collection<CaosScriptScriptElement>)

internal enum class JectScriptType(val type: String) {
    REMOVAL("Removal Scripts"),
    EVENT("Event Scripts"),
    INSTALL("Install Scripts")
}

internal val JectSettingsKey = Key<JectSettings>("creatures.caos.injector.JECT_SETTINGS")

/**
 * Holds settings for Ject dialog
 */
internal data class JectSettings(
    var injectRemovalScriptsSelected: Boolean = true,
    var injectEventScriptsSelected: Boolean = true,
    var injectInstallScriptsSelected: Boolean = true
)

/**
 * Initializes, builds, and displays dialog box to select script types to inject
 */
private fun injectC3WithDialog(file: CaosScriptFile, gameInterfaceName: GameInterfaceName) {
    val project = file.project
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
    showC3InjectPanel(project, gameInterfaceName, file, options)
}


/**
 * Creates a popup dialog box to select the kind of scripts to inject in cases of CV+
 */
internal fun showC3InjectPanel(
    project: Project,
    gameInterfaceName: GameInterfaceName,
    file: CaosScriptFile,
    scriptsIn: List<ScriptBundle>
) {

    val variant = gameInterfaceName.variant ?: file.variant
    if (variant == null) {
        CaosNotifications.showError(
            project,
            "CAOS Injection Error",
            "Cannot inject CAOS script without game variant"
        )
        return
    }
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

//    val injectionMethod = JComboBox<String>(arrayOf("", INJECTION_METHOD_USE_JECT, INJECTION_METHOD_EACH_SCRIPT)).apply {
//        this.toolTipText = "Method to inject scripts into the game"
//    }

//    if (Injector.canJect(variant, gameInterfaceName)) {
//
//        panel.add(JLabel("Inject.."))
//        injectionMethod.selectedIndex = if (project.settings.useJectByDefault)
//            1
//        else
//            2
//        panel.add(injectionMethod)
//        injectionMethod.updateUI()
//    }

    // Build actual injection dialog box
    DialogBuilder(project)
        .centerPanel(panel).apply {
            okActionEnabled(true)
            setOkOperation {
                this.dialogWrapper.close(0)
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

                var flags = 0
                // Order is important
                if (removalScriptsCheckBox?.isSelected.orFalse()) {
                    flags += Injector.REMOVAL_SCRIPT_FLAG
                }
                if (eventScriptsCheckBox?.isSelected.orFalse()) {
                    flags += Injector.EVENT_SCRIPT_FLAG
                }
                if (installScriptsCheckBox?.isSelected.orFalse()) {
                    flags += Injector.INSTALL_SCRIPT_FLAG
                }

                // Decide to use JECT. If nothing is selected, do not use ject, and do not update settings
//                val useJect: Boolean? = when(injectionMethod.selectedItem as String) {
//                    INJECTION_METHOD_USE_JECT -> true
//                    INJECTION_METHOD_EACH_SCRIPT -> false
//                    else -> null
//                }
                // If Injection method was selected, set it in project settings.
//                if (useJect != null) {
//                    project.settings.useJectByDefault = useJect
//                }
                // Do not use ject for now.
                // TODO figure out how to get directories from Registry
                val useJect: Boolean? = null
                // Inject CAOS
                Injector.inject(project, variant, gameInterfaceName, file, flags, useJect == true)
            }
        }.showModal(true)
}


private const val INJECTION_METHOD_USE_JECT = "File with JECT"
private const val INJECTION_METHOD_EACH_SCRIPT = "Each script individually"