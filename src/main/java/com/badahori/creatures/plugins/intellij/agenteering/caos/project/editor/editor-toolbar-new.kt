package com.badahori.creatures.plugins.intellij.agenteering.caos.project.editor

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CompileCAOS2Action
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.AddGameInterfaceAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.CaosInjectFileAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.InjectorActionGroup
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosInjectorApplicationSettingsService
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.allGameInterfaceNames
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.settings
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.DisposablePsiTreChangeListener
import com.badahori.creatures.plugins.intellij.agenteering.injector.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.bedalton.common.structs.Pointer
import com.bedalton.common.util.className
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.text.BlockSupport
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.event.ItemEvent
import java.awt.event.MouseEvent
import java.util.*
import java.util.Timer
import javax.swing.*


private val KEY: Key<EditorNotificationPanel> = Key.create("creatures.caos.CaosEditorToolbar")
internal val EDITOR_INJECTOR_KEY: Key<GameInterfaceName?> = Key.create("creatures.caos.CaosEditorInjector")
internal val EDITOR_VARIANT_KEY: Key<CaosVariant?> = Key.create("creatures.caos.CaosEditorVariant")


/**
 * An editor notification provider
 * Though not its original purpose, the notification provider functions as a persistent toolbar
 */
class CaosScriptEditorToolbar(
    val project: Project
) : EditorNotifications.Provider<EditorNotificationPanel>(), DumbAware, Disposable {

    override fun getKey(): Key<EditorNotificationPanel> = KEY

    override fun createNotificationPanel(
        virtualFile: VirtualFile,
        fileEditor: FileEditor,
        project: Project
    ): EditorNotificationPanel? {
        val caosFile = virtualFile.getPsiFile(project) as? CaosScriptFile
            ?: return null
        if (!caosFile.isValid) {
            LOGGER.severe("CAOSFile is invalid on createNotificationsPanel")
            return null
        }
        ProgressIndicatorProvider.checkCanceled()
        val backgroundColor: Color = UIUtil.getPanelBackground()
        val panel = EditorNotificationPanel(fileEditor, backgroundColor)
        try {
            val headerComponent = createCaosScriptHeaderComponent(
                project = project,
                fileEditor = fileEditor,
                virtualFile = virtualFile,
                caosFile = caosFile,
            )
                ?: return panel
            headerComponent.background = backgroundColor
            panel.add(headerComponent)
        } catch (e: ProcessCanceledException) {
            LOGGER.severe("Process was canceled during notification panel creation")
            e.rethrowAnyCancellationException()
            e.printStackTrace()
            return panel
        } catch (e: Exception) {
            e.rethrowAnyCancellationException()
            LOGGER.severe("ERROR creating header component. ${e.className}(${e.message}) @ ${virtualFile.name}")
        }
        return panel
    }

    override fun dispose() {
    }
}

internal fun createCaosScriptHeaderComponent(
    project: Project,
    fileEditor: FileEditor,
    virtualFile: VirtualFile,
    caosFile: CaosScriptFile
): JComponent? {

    if (project.isDisposed) {
        return null
    }

    // Create base toolbar
    val toolbar = JPanel()
    toolbar.layout = BoxLayout(toolbar, BoxLayout.X_AXIS)


    if (!caosFile.isValid) {
        LOGGER.severe("File is invalid in create toolbar")
        return null
    }
    // Create a pointer to this file for use later
    val pointer = try {
        SmartPointerManager.createPointer(caosFile)
    } catch (e: Exception) {
        e.rethrowAnyCancellationException()
        LOGGER.severe("Failed to create smart pointer for CAOSScriptFile. ${e.className}(${e.message})")
        e.printStackTrace()
        return null
    }

    invokeLater {
        runReadAction {
            populate(project, fileEditor, virtualFile, pointer, toolbar)
        }
    }
    return toolbar
}


private fun populate(
    project: Project,
    fileEditor: FileEditor,
    virtualFile: VirtualFile,
    pointer: SmartPsiElementPointer<CaosScriptFile>,
    toolbar: JPanel
) {

    if (project.isDisposed) {
        return
    }

    // Variants
    val variantPanel = JPanel()
    val variantSelect = ComboBox(
        arrayOf(
            "",
            "C1",
            "C2",
            "CV",
            "C3",
            "DS",
            "SM"
        )
    )

    val naturalInjectorSelect = Pointer(false)
    variantSelect.updateUI()
    variantPanel.add(JLabel("Variant:"))
    variantPanel.add(variantSelect)
    variantPanel.add(JSeparator(SwingConstants.VERTICAL))


    // Injector
    val injectorsContainer = JPanel()
    val injectors = ComboBox<AnAction>()
    injectors.renderer = ActionCellRender
    injectors.updateUI()
    val injectorActionGroup = pointer.element?.let { caosFile ->
        InjectorActionGroup(caosFile)
    } ?: return
    val initialInjectorsList = injectorActionGroup.getChildren(null)
    val injectorModel = DefaultComboBoxModel(initialInjectorsList)
    injectors.model = injectorModel
    var lastInjector: AnAction?


    // Action to run whatever injector is currently selected
    val runInjectorAction = RunInjectorAction(
        project,
        pointer,
        null,
        injectors
    )

    val runInjectorActionPresentation = Presentation()


    // Actual injection button
    val runInjectorButton = ActionButton(
        runInjectorAction,
        runInjectorActionPresentation,
        virtualFile.path,
        ActionToolbar.NAVBAR_MINIMUM_BUTTON_SIZE
    )
    var injectorList: List<GameInterfaceName> = CaosInjectorApplicationSettingsService
        .getInstance()
        .allGameInterfaceNames
    val updateInjectors: (variant: CaosVariant?, GameInterfaceName?) -> Unit = update@{ variant, newGameInterface ->

        val newActions: List<AnAction> = InjectorActionGroup
            .getActions(pointer)
            .filter {
                it !is CaosInjectFileAction || (OsUtil.isWindows || it.gameInterfaceName !is NativeInjectorInterface)
            }
        val injectorActions = newActions.filterIsInstance<CaosInjectFileAction>()
        val injectorNames = injectorActions.map { it.gameInterfaceName }
        if ((injectorNames + injectorList).distinct().isEmpty())
            return@update

        val targetInterface = newGameInterface ?: (injectors.selectedItem as? CaosInjectFileAction)?.gameInterfaceName
        injectorModel.removeAllElements()
        injectorModel.addAll(newActions.toList())
        val newSelection = injectorActions.firstOrNull { it.gameInterfaceName == targetInterface }
            ?: injectorActions.firstOrNull { it.gameInterfaceName.variant == variant }
            ?: injectorActions.firstOrNull { it.gameInterfaceName.isVariant(variant) }
        lastInjector = newSelection

        naturalInjectorSelect.value = false
        injectors.selectedItem = newSelection
        naturalInjectorSelect.value = true
        runInjectorAction.setAction(newSelection)
        injectors.updateUI()
        fileEditor.putUserData(EDITOR_INJECTOR_KEY, newGameInterface)
        virtualFile.putUserData(EDITOR_INJECTOR_KEY, newGameInterface)

    }
    injectorsContainer.add(JLabel("Inject:"))
    injectorsContainer.add(injectors)
    injectorsContainer.add(runInjectorButton)
    injectorsContainer.add(JSeparator(SwingConstants.VERTICAL))

    // Compile Panel
    val compilePanel = JPanel()
    compilePanel.layout = GridBagLayout()
    val gbc = GridBagConstraints()
    val compileAction = CompileCAOS2Action().apply {
        this.file = virtualFile
    }

    val compileButtonPresentation = Presentation("Compile Agent")

    val compileButton = ActionButton(
        compileAction,
        compileButtonPresentation,
        virtualFile.path,
        ActionToolbar.NAVBAR_MINIMUM_BUTTON_SIZE
    )

    compilePanel.add(compileButton, gbc)
    compilePanel.add(JSeparator(SwingConstants.VERTICAL), gbc)

    // Initialize toolbar with color
    //toolbar.panel.background = TRANSPARENT_COLOR

    // Add Docs button for whatever variant is selected
    val docsButton = JButton("DOCS")
    docsButton.addClickListener { e ->
        if (e.button == MouseEvent.BUTTON1) {
            val variant = pointer.element?.variant?.nullIfUnknown()
                ?: return@addClickListener
            openDocs(project, variant)
        }
    }

    // Callback for when variant is assigned
    val assignVariant = select@{ selected: CaosVariant?, explicit: Boolean ->
        // If script can be injected, enable injection button
        val canInject = selected != null && Injector.canConnectToVariant(selected)

        injectorsContainer.isVisible = canInject

        fileEditor.putUserData(EDITOR_VARIANT_KEY, selected)
        virtualFile.putUserData(EDITOR_VARIANT_KEY, selected)

        invokeLater {
            naturalInjectorSelect.value = false
            updateInjectors(pointer.element?.variant, null)
        }

        // Set default variant in settings
        if (selected != null && explicit) {
            project.settings.lastVariant = selected
        }

        var file = pointer.element
            ?: return@select
        if (selected == file.variant) {
            return@select
        }
        // Set selected variant in file
        file.setVariant(selected, explicit)

        // Reparse file with new variant
        runWriteAction run@{
            file = pointer.element
                ?: return@run
            try {
                BlockSupport.getInstance(project).reparseRange(file, 0, file.endOffset, file.text)
            } catch (e: Exception) {
                e.rethrowAnyCancellationException()
            }
        }
        file = pointer.element
            ?: return@select
        DaemonCodeAnalyzer.getInstance(project).restart(file)
    }

    val moduleVariant = pointer.element?.module?.variant
    val showVariantSelect = try {
        (moduleVariant == null ||
                moduleVariant == CaosVariant.UNKNOWN ||
                moduleVariant.isC3DS) &&
                pointer.element?.virtualFile !is CaosVirtualFile
    } catch (e: Exception) {
        e.rethrowAnyCancellationException()
        false
    }

    if (showVariantSelect) {
        if (moduleVariant?.isC3DS == true) {
            variantSelect.removeAllItems()
            variantSelect.addItem("C3")
            variantSelect.addItem("DS")
        }
    }


    // Listener for when CAOS2Compiler directive changes variants
    runReadAction run@{
        pointer.element?.let { caosFile ->
            val caos2Listener = caosFile.addCaos2ChangeListener { isCaos2 ->
                compilePanel.isVisible = isCaos2 != null
                compileButton.isVisible = isCaos2 != null
                compileButton.revalidate()
                compilePanel.revalidate()
            } ?: return@run
            PsiManager.getInstance(project).addPsiTreeChangeListener(caos2Listener, fileEditor)

        }
    }


    // Add variant change listener
    variantSelect.addItemListener variant@{ e ->
        // If state change is not selection (i.e. deselect) return
        if (e.stateChange != ItemEvent.SELECTED)
            return@variant
        // Get the selected variant
        val selected = CaosVariant.fromVal(variantSelect.selectedItem as String)
            .nullIfUnknown()
        val oldVariant = pointer.element?.variant
        if (oldVariant == selected) {
            return@variant
        }
        assignVariant(selected, true)

    }

    // If variant is unknown, allow for variant selection
    if (!showVariantSelect) {
        // If variant is set in module hide variant select
        variantPanel.isVisible = false
    }

    CaosInjectorApplicationSettingsService.addSettingsChangedListener(fileEditor) { _, settings ->
        if ((settings.gameInterfaceNames + injectorList).distinct().isEmpty()) {
            return@addSettingsChangedListener
        }
        injectorList = settings.gameInterfaceNames
        naturalInjectorSelect.value = false
        updateInjectors(pointer.element?.variant, null)
        naturalInjectorSelect.value = true
    }

    val setInitialInjector: (initialVariant: CaosVariant?, initialLastInterfaceName: GameInterfaceName?) -> Unit =
        { initialVariant, initialLastInterfaceName ->
            val newActions: Array<AnAction> = InjectorActionGroup
                .getActions(pointer)
                .filter {
                    it !is CaosInjectFileAction || OsUtil.isWindows || it.gameInterfaceName !is NativeInjectorInterface
                }
                .toTypedArray()
            injectorModel.removeAllElements()
            injectorModel.addAll(newActions.toList())
            injectors.updateUI()
            lastInjector = newActions.filterIsInstance<CaosInjectFileAction>()
                .firstOrNull { anInterface ->
                    anInterface.gameInterfaceName == initialLastInterfaceName
                }
                ?: newActions.firstOrNull {
                    (it as? CaosInjectFileAction)?.gameInterfaceName?.variant == initialVariant
                } ?: newActions.firstOrNull {
                    (it as? CaosInjectFileAction)?.gameInterfaceName?.isVariant(initialVariant) == true
                }
            naturalInjectorSelect.value = false
            injectors.selectedItem = lastInjector
            naturalInjectorSelect.value = true
            injectors.updateUI()
            updateInjectors(initialVariant, initialLastInterfaceName)
            injectors.addItemListener { e ->
                if (e.stateChange == ItemEvent.SELECTED) {
                    val selected = injectors.selectedItem as? AnAction
                        ?: return@addItemListener
                    if (selected is AddGameInterfaceAction) {
                        val created = selected.create(virtualFile)
                        if (created == null) {
                            naturalInjectorSelect.value = false
                            injectors.selectedItem = lastInjector
                            return@addItemListener
                        }
                        pointer.element?.lastInjector = created
                        invokeLater {
                            injectorActionGroup.getChildren(null).let { action ->
                                injectorModel.removeAllElements()
                                injectorModel.addAll(action.toList())
                                val thisAction = action
                                    .filterIsInstance<CaosInjectFileAction>()
                                    .firstOrNull { it.gameInterfaceName == created }
                                    ?: lastInjector
                                lastInjector = thisAction
                                runInjectorAction.setAction(thisAction)
                                naturalInjectorSelect.value = true
                                injectorModel.selectedItem = thisAction
                            }
                        }
                        return@addItemListener
                    } else if (selected is CaosInjectFileAction) {
                        val file: CaosScriptFile = pointer.element
                            ?: return@addItemListener
                        val gameInterface = selected.gameInterfaceName
                        val lastInterface = (lastInjector as? CaosInjectFileAction)?.gameInterfaceName
                        if (gameInterface != lastInterface) {
                            lastInjector = selected
                            if (naturalInjectorSelect.value) {
                                file.lastInjector = selected.gameInterfaceName
                            }
                            runInjectorAction.setAction(selected)
                        }
                    }
                    naturalInjectorSelect.value = true
                }
            }
        }

    val setInitialVariant: (CaosVariant?) -> Unit = { initialVariant ->
        if (initialVariant != null) {
            variantSelect.selectedItem = initialVariant.code
        }
    }


    // Populate toolbar
    val main = JPanel()
    main.layout = FlowLayout(FlowLayout.LEFT)
    main.add(variantPanel)
    main.add(injectorsContainer)
    main.add(compilePanel)
    toolbar.add(main)
    toolbar.add(Box.createHorizontalGlue())
    toolbar.add(docsButton)

    pointer.element?.apply {
        this.addListener { newVariant ->
            if (newVariant != null) {
                assignVariant(newVariant, false)
            }
        }
    }
    invokeLater {
        interruptedInitializer(
            project,
            fileEditor,
            pointer,
            setInitialVariant,
            setInitialInjector
        )
    }

}


private fun interruptedInitializer(
    project: Project,
    disposable: Disposable,
    pointer: SmartPsiElementPointer<CaosScriptFile>,
    setVariant: (variant: CaosVariant?) -> Unit,
    setInitialInjector: (initialVariant: CaosVariant?, gameInterface: GameInterfaceName?) -> Unit
) {
    var initializer: DisposablePsiTreChangeListener? = null
    val delay = 800L
    var timer: Timer? = null

    val reschedule = {
        timer?.cancel()
        timer = Timer().apply {
            this.schedule(object : TimerTask() {
                override fun run() {
                    timer?.cancel()
                    val initializerObject = initializer
                    if (initializerObject != null) {
                        PsiManager.getInstance(project).removePsiTreeChangeListener(initializerObject)
                        initializer = null
                    }
                    invokeLater {
                        runWriteAction {
                            setWhenReady(project, pointer, setVariant, setInitialInjector)
                        }
                    }
                }
            }, delay)
        }
    }

    val strongInitializer = object : DisposablePsiTreChangeListener {

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
        }

        override fun childRemoved(event: PsiTreeChangeEvent) {
        }

        override fun childReplaced(event: PsiTreeChangeEvent) {
        }

        override fun childrenChanged(event: PsiTreeChangeEvent) {
        }

        override fun childMoved(event: PsiTreeChangeEvent) {
        }

        override fun propertyChanged(event: PsiTreeChangeEvent) {
            if (project.isDisposed) {
                return
            }
            val file = pointer.element
            if (file == null || !file.isValid) {
                PsiManager.getInstance(project).removePsiTreeChangeListener(this)
                return
            }
            val eventFile = event.file
                ?: return
            if (eventFile.virtualFile.path != file.virtualFile?.path && eventFile.virtualFile != file.virtualFile) {
                return
            }
            if (event.propertyName != "propUnloadedPsi") {
                return
            }
            reschedule()
        }

        override fun dispose() {
            if (project.isDisposed) {
                return
            }
            PsiManager.getInstance(project).removePsiTreeChangeListener(this)
        }
    }

    initializer = strongInitializer
    reschedule()
    PsiManager.getInstance(project).addPsiTreeChangeListener(strongInitializer, disposable)
}

fun setWhenReady(
    project: Project,
    pointer: SmartPsiElementPointer<CaosScriptFile>,
    setVariant: (variant: CaosVariant?) -> Unit,
    setInitialInjector: (initialVariant: CaosVariant?, gameInterface: GameInterfaceName?) -> Unit
) {

    if (project.isDisposed) {
        return
    }

    val file = pointer.element
        ?: return
    if (!file.isValid)
        return
    val initialVariant = file.variant
    setVariant(initialVariant)
    val initialInjector = file.lastInjector
    setInitialInjector(initialVariant, initialInjector)
}


/**
 * Takes care of running a variable injector action contained inside another action
 */
private class RunInjectorAction(
    val project: Project,
    val pointer: SmartPsiElementPointer<CaosScriptFile>,
    action: AnAction?,
    val injectors: JComboBox<AnAction>
) : AnAction(
    "Inject CAOS",
    "Inject CAOS",
    AllIcons.RunConfigurations.TestState.Run
) {

    private var mAction: AnAction?

    init {
        mAction = action
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    fun setAction(action: AnAction?, presentation: Presentation? = null) {
        if (action == null) {
            return
        }
        if (action !is CaosInjectFileAction) {
            LOGGER.severe("RunInjector action is not CAOS injector; Action: ${action.className}")
            return
        }
        this.mAction = action
        if (presentation != null) {
            updatePresentation(presentation, action)
        }
    }

    override fun update(e: AnActionEvent) = runReadAction {
            super.update(e)
            var injector: AnAction? = mAction
            if (injector == null || injector is AddGameInterfaceAction) {
                // Get the selected item
                injector = injectors.selectItem(true) {
                    it !is AddGameInterfaceAction
                }
                // Set selected as an action
                setAction(injector, e.presentation)

                // If injector is null, return
                // NULL means this could be an AddGameInterfaceAction
                if (injector == null) {
                    return@runReadAction
                }
            }
            updatePresentation(e.presentation, injector)
        }

    private fun updatePresentation(presentation: Presentation, injector: AnAction?) {
        if (injector == null) {
            presentation.text = "Select Injector Interface"
            presentation.isEnabled = false
            return
        }
        val valid = injector !is AddGameInterfaceAction
        presentation.isEnabled = valid
        presentation.text = "Select Injector Interface" // injector.templateText ?: "Select Injector Interface"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val injector = mAction

        if (injector == null) {
            val notification = CaosNotifications.createErrorNotification(
                project,
                "CAOS Injector",
                "No game interface selected for inject"
            )
            val variant = pointer.element?.variant
            if (variant == null) {
                notification.show()
                return
            }

            val actions = InjectorActionGroup.getActions(pointer) { "Inject with: ${it.name}" }
            if (actions.isEmpty()) {
                notification.show()
                return
            }
            CaosInjectorNotifications.createInfoNotification(
                project,
                "CAOS Injector",
                "No game interface selected for inject"
            ).apply {
                addActions(*actions)
            }
            return
        }
        injector.actionPerformed(e)
    }
}


/**
 * In charge of rendering an action inside a cell such as a combo-box item
 */
private object ActionCellRender : ListCellRenderer<AnAction> {
    val label = JLabel()
    override fun getListCellRendererComponent(
        list: JList<out AnAction>?,
        value: AnAction?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        label.isVisible = value != null
        if (value != null) {
            label.text = value.templateText ?: "????"
        }
        return label
    }
}
