package com.badahori.creatures.plugins.intellij.agenteering.caos.action

import bedalton.creatures.util.PathUtil
import bedalton.creatures.util.className
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.getScripts
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Cob
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfNotConcrete
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.injectionCheckDisabled
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.settings
import com.badahori.creatures.plugins.intellij.agenteering.injector.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.util.Key
import com.intellij.openapi.vcs.CodeSmellDetector
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.components.CheckBox
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel

//
//@Suppress("ComponentNotRegistered", "unused")
//internal class InjectCaosAction : AnAction(
//    "Inject CAOS",
//    "Inject CAOS into game",
//    AllIcons.Toolwindows.ToolWindowRun
//) {
//
//    override fun actionPerformed(e: AnActionEvent) {
//        val project = e.project
//            ?: return
//        val files = e.files
//        if (files.isEmpty())
//            return
//        if (files.size > 1) {
//            throw Exception("Only one file can be injected at a time")
//        }
//        val caosFile = files[0].getPsiFile(project) as? CaosScriptFile
//            ?: return
//        val variant = caosFile.variant
//            ?: return
//
//        caosInject(project, variant, GameInterfaceName(variant), caosFile)
//    }
//}


internal fun caosInject(
    project: Project,
    variant: CaosVariant,
    gameInterfaceName: GameInterfaceName,
    caosFile: CaosScriptFile,
) {

    val checkedSettings = JectSettings()
    // Persist JECT settings into file
    if (caosFile.getUserData(JectSettingsKey) == null)
        caosFile.putUserData(JectSettingsKey, checkedSettings)

    // If virtual file is valid run check for validity
    caosFile.virtualFile?.let { virtualFile ->
        if (!isValidForInject(project, virtualFile)) {
            CaosInjectorNotifications
                .createErrorNotification(project, "Syntax Errors", "Cannot inject CAOS code with known errors.")
                .addAction(object : AnAction("Ignore for Session and Inject") {
                    override fun actionPerformed(e: AnActionEvent) {
                        project.settings.injectionCheckDisabled = true
                        try {
                            injectActual(project, variant, gameInterfaceName, caosFile)
                        } catch (e: Exception) {
                            LOGGER.severe("Inject ignored for session failed: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                })
                .addAction(object : AnAction("Inject Anyways") {
                    override fun actionPerformed(e: AnActionEvent) {
                        try {
                            injectActual(project, variant, gameInterfaceName, caosFile)
                        } catch (e: Exception) {
                            LOGGER.severe("Inject always failed: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                })
                .show()
            return
        }
    }
    try {
        injectActual(project, variant, gameInterfaceName, caosFile)
    } catch (e: Exception) {
        LOGGER.severe("Inject actual failed: ${e.message}")
        e.printStackTrace()
    }
}

private fun injectActual(
    project: Project,
    variant: CaosVariant,
    gameInterfaceName: GameInterfaceName,
    caosFile: CaosScriptFile,
) {
    // If variant is CV+ ask which parts of the file to inject
    if (variant.isNotOld || caosFile.isCaos2Cob) {
        val scripts = caosFile.getScripts()
        if (scripts.isNotEmpty() && scripts.all { it is CaosScriptMacro }) {
            GlobalScope.launch {
                try {
                    Injector.inject(
                        project = project,
                        variant = variant,
                        gameInterfaceName = gameInterfaceName,
                        caosFile = caosFile,
                        totalFiles = 1,
                        jectFlags = 7,
                        tryJect = project.settings.useJectByDefault
                    )
                } catch (e: Exception) {
                    LOGGER.severe("Failed to inject C2e: ${e.message}")
                    e.printStackTrace()
                }
            }
        } else {
            injectC3WithDialog(caosFile, gameInterfaceName)
        }
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
            GlobalScope.launch {
                try {
                    Injector.inject(
                        project = project,
                        variant = variant,
                        gameInterfaceName = gameInterfaceName,
                        caosFile = caosFile,
                        1,
                        jectFlags = 7,
                        tryJect = false
                    )
                } catch (e: Exception) {
                    LOGGER.severe("Failed to inject through command ${e.message}")
                    e.printStackTrace()
                }
            }
        }

}

internal data class ScriptBundle(
    val scripts: Collection<CaosScriptScriptElement>,
) {
    val eventScripts by lazy {
        scripts.filterIsInstance<CaosScriptEventScript>()
    }
    val removalScripts by lazy {
        scripts.filterIsInstance<CaosScriptRemovalScript>()
    }
    val installScripts by lazy {
        scripts.filter { it is CaosScriptInstallScript || it is CaosScriptMacro }
    }
}

internal enum class JectScriptType(val plural: String, val singular: String) {
    REMOVAL("Removal Scripts", "Removal Script"),
    EVENT("Event Scripts", "Event Script"),
    INSTALL("Install Scripts", "Install Script")
}

internal val JectSettingsKey = Key<JectSettings>("creatures.caos.injector.JECT_SETTINGS")

/**
 * Holds settings for Ject dialog
 */
internal data class JectSettings(
    var injectRemovalScriptsSelected: Boolean = true,
    var injectEventScriptsSelected: Boolean = true,
    var injectInstallScriptsSelected: Boolean = true,
    var injectLinkedFiles: Boolean = false,
)

/**
 * Initializes, builds, and displays dialog box to select script types to inject
 */
private fun injectC3WithDialog(file: CaosScriptFile, gameInterfaceName: GameInterfaceName) {
    val project = file.project
    val linkedScripts = try {
        val linkedScripts = getLinkedScripts(project, file)
        ScriptBundle(linkedScripts)
    } catch (e: Exception) {
        val message = "<html>${(e.message ?: "Failed parsing linked files")}.<br />Continue without linked files</html>"

        val shouldContinue = DialogBuilder()
            .title("Injector Failure")
            .centerPanel(JLabel(message))
            .apply {
                val me = this
                addOkAction().apply {
                    this.setText("Continue")
                }
                addCancelAction()
                setCancelOperation {
                    me.dialogWrapper
                }
            }
            .showAndGet()
        LOGGER.severe("Error Getting linked scripts: ${e.className}(${e.message})")
        if (!shouldContinue)
            return
        null
    }
    val scripts = PsiTreeUtil.collectElementsOfType(file, CaosScriptScriptElement::class.java)
    val fileScripts = ScriptBundle(scripts)
    if (scripts.isEmpty() && linkedScripts?.scripts.isNullOrEmpty() && file.text.trim().isNotBlank()) {
        LOGGER.severe("Script is not blank, but no script elements found within")
        return
    }
    showC3InjectPanel(project, gameInterfaceName, file, fileScripts, linkedScripts)
}


private fun getLinkedScripts(project: Project, file: CaosScriptFile): List<CaosScriptScriptElement> {
    val commands = PsiTreeUtil.collectElementsOfType(file, CaosScriptCaos2Command::class.java)
    val parentDirectory = file.virtualFile?.parent
        ?: throw Exception("Failed to get reference to root CAOS file")
    val linkCommands = commands.filter {
        it.commandName.equals("Link", ignoreCase = true)
    }
    return linkCommands.flatMap { linkCommand ->
        linkCommand.caos2ValueList.flatMap { fileNameElement ->
            val fileName = fileNameElement.valueAsString
                ?: throw Exception("Invalid link value. Value is not a string")
            collectScriptsAtPath(project, parentDirectory, fileName)
        }
    }
}

private fun collectScriptsAtPath(
    project: Project,
    parentDirectory: VirtualFile,
    childPath: String,
): Collection<CaosScriptScriptElement> {
    val linkedVirtualFile = parentDirectory.findChildRecursive(childPath, true)
        ?: throw Exception(
            "Failed to find linked script: '$childPath'; File at absolutePath: '${
                PathUtil.combine(
                    parentDirectory.path,
                    childPath
                )
            }' does not exist"
        )
    val linkedFile = linkedVirtualFile.getPsiFile(project) as? CaosScriptFile
        ?: throw Exception("Failed parse linked file. '$childPath' is not a CAOS file")
    return PsiTreeUtil.collectElementsOfType(linkedFile, CaosScriptScriptElement::class.java)
}


/**
 * Creates a popup dialog box to select the kind of scripts to inject in cases of CV+
 */
internal fun showC3InjectPanel(
    project: Project,
    gameInterfaceName: GameInterfaceName,
    file: CaosScriptFile,
    scriptsIn: ScriptBundle,
    linked: ScriptBundle?,
) {

    val variant = file.variant.nullIfNotConcrete()

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
    val removalScriptsCheckBox =
        if (scriptsIn.removalScripts.isNotNullOrEmpty() || linked?.eventScripts.isNotNullOrEmpty())
            JCheckBox(JectScriptType.REMOVAL.plural).apply {
                this.isSelected = jectSettings.injectRemovalScriptsSelected
            }
        else
            null
    // Filter for event scripts and created checkbox if needed
    val eventScriptsCheckBox = if (scriptsIn.eventScripts.isNotNullOrEmpty() || linked?.eventScripts.isNotNullOrEmpty())
        JCheckBox(JectScriptType.EVENT.plural).apply {
            this.isSelected = jectSettings.injectEventScriptsSelected
        }
    else
        null

    // Filter for install/macro scripts and create checkbox if needed
    val installScriptsCheckBox =
        if (scriptsIn.installScripts.isNotNullOrEmpty() || linked?.installScripts.isNotNullOrEmpty())
            JCheckBox(JectScriptType.INSTALL.plural).apply {
                this.isSelected = jectSettings.injectInstallScriptsSelected
            }
        else
            null

    // Add checkboxes and count script types
    var scriptTypes = 0
    removalScriptsCheckBox?.let { scriptTypes++; panel.add(it) }
    eventScriptsCheckBox?.let { scriptTypes++; panel.add(it) }
    installScriptsCheckBox?.let { scriptTypes++; panel.add(it) }
    val injectLinkedCheckbox: JCheckBox? = if (linked != null && linked.scripts.isNotEmpty()) {
        CheckBox("Inject linked files", jectSettings.injectLinkedFiles).apply {
            panel.add(this)
        }
    } else
        null


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
                removalScriptsCheckBox?.isSelected?.apply {
                    jectSettings.injectRemovalScriptsSelected = this
                }
                eventScriptsCheckBox?.isSelected?.apply {
                    jectSettings.injectEventScriptsSelected = this
                }
                installScriptsCheckBox?.isSelected?.apply {
                    jectSettings.injectInstallScriptsSelected = this
                }

                injectLinkedCheckbox?.apply {
                    jectSettings.injectLinkedFiles = this.isSelected
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
//                val useJect: Boolean? = null
                // Inject CAOS
                if (injectLinkedCheckbox?.isSelected != true)
                    GlobalScope.launch {
                        try {
                            Injector.inject(project,
                                variant = variant,
                                gameInterfaceName = gameInterfaceName,
                                caosFile = file,
                                totalFiles = 1,
                                jectFlags = flags,
                                tryJect = false
                            )
                        } catch (e: Exception) {
                            LOGGER.severe("Failed to inject linked files ${e.message}")
                            e.printStackTrace()
                        }
                    }
                else {
                    val out = mutableMapOf<JectScriptType, List<CaosScriptScriptElement>>()
                    if (flags hasFlag Injector.REMOVAL_SCRIPT_FLAG)
                        out[JectScriptType.REMOVAL] = scriptsIn.removalScripts + linked?.removalScripts.orEmpty()
                    if (flags hasFlag Injector.EVENT_SCRIPT_FLAG)
                        out[JectScriptType.EVENT] = scriptsIn.eventScripts + linked?.eventScripts.orEmpty()
                    if (flags hasFlag Injector.INSTALL_SCRIPT_FLAG)
                        out[JectScriptType.INSTALL] = scriptsIn.installScripts + linked?.installScripts.orEmpty()

                    GlobalScope.launch {
                        Injector.inject(project, variant, gameInterfaceName, file.name, out)
                    }
                }
            }
        }.showModal(true)
}


//private const val INJECTION_METHOD_USE_JECT = "File with JECT"
//private const val INJECTION_METHOD_EACH_SCRIPT = "Each script individually"

private fun isValidForInject(project: Project, virtualFile: VirtualFile): Boolean {
    if (project.isDisposed) {
        return false
    }
    if (project.settings.injectionCheckDisabled)
        return true
    val detector = CodeSmellDetector.getInstance(project)
    val smells = detector.findCodeSmells(listOf(virtualFile))
        .filter {
            it.severity == HighlightSeverity.ERROR
        }
    return smells.isEmpty()
}