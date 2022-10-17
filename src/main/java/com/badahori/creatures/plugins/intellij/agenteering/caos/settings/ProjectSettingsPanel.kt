package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.injector.CreateInjectorDialog
import com.badahori.creatures.plugins.intellij.agenteering.injector.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.util.ui.FormBuilder
import java.awt.Color
import java.awt.Dimension
import javax.swing.*


class CaosProjectSettingsConfigurable(private val project: Project) : Configurable {

    private lateinit var panel: ProjectSettingsPanel

    private val projectSettingsService: CaosProjectSettingsService? by lazy {
        if (project.isDisposed)
            return@lazy null
        CaosProjectSettingsService.getInstance(project)
    }

    private val applicationSettingsService: CaosApplicationSettingsService? by lazy {
        if (project.isDisposed)
            return@lazy null
        CaosApplicationSettingsService.getInstance()
    }

    override fun reset() {
        panel.reset()
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return panel.getPreferredFocusComponent()
    }

    private val applicationSettings: CaosApplicationSettingsComponent.State? by lazy {
        applicationSettingsService?.state
    }

    private val projectSettings: CaosProjectSettingsComponent.State? by lazy {
       projectSettingsService?.state
    }

    override fun createComponent(): JComponent? {
        if (this::panel.isInitialized)
            return this.panel.panel
        val applicationSettings = applicationSettings
            ?: return null
        val projectSettings = projectSettings
            ?: return null
        val panel = ProjectSettingsPanel(project, applicationSettings, projectSettings)
        this.panel = panel
        return panel.panel
    }

    override fun isModified(): Boolean {
        if (this::panel.isInitialized)
            return panel.modified() && this.panel.interfaceNamesAreValid
        return false
    }

    override fun apply() {
        if (!this::panel.isInitialized)
            return
        val applicationService = applicationSettingsService
            ?: return
        val updatedApplicationSettings = panel.applyApplicationSettings()
        applicationService.loadState(updatedApplicationSettings)
        val projectService = projectSettingsService
            ?: return
        val updatedProjectSettings = panel.applyProjectSettings()
        projectService.loadState(updatedProjectSettings)
        ProjectView.getInstance(project).refresh()
    }

    override fun getDisplayName(): String {
        return "CAOS & Agenteering"
    }

}

private class ProjectSettingsPanel(
    private val project: Project,
    private var applicationSettings: CaosApplicationSettingsComponent.State,
    private var projectSettings: CaosProjectSettingsComponent.State
    ) {

    private val originalCombineAttNodes = applicationSettings.combineAttNodes
    private val originalReplicateAttToDuplicateSprite = applicationSettings.replicateAttToDuplicateSprite != false
    private val originalIgnoredFilesText = projectSettings.ignoredFilenames.joinToString("\n")
    private val originalGameInterfaceNames: List<GameInterfaceName> = applicationSettings.gameInterfaceNames
    private val originalGameInterfaceNamesSerialized = originalGameInterfaceNames.serialized()
    private val originalDefaultVariant: String = projectSettings.defaultVariant?.code ?: ""
    private val originalIsAutoPoseEnabled: Boolean = applicationSettings.isAutoPoseEnabled

    private var mInterfaceNamesAreValid = true
    val interfaceNamesAreValid get() = mInterfaceNamesAreValid

    private val defaultVariant by lazy {
        JComboBox(
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
    }

    private val ignoredFileNames by lazy {
        JTextArea().apply {
            TextPrompt(CaosBundle.message("caos.settings.ignored-files.placeholder"), this)
                .apply {
                    this.foreground = hintColor
                }
            this.preferredSize = preferredTextBoxSize
            this.border = BorderFactory.createLineBorder(Color.WHITE)
        }
    }

    private val preferredTextBoxSize by lazy {
        Dimension(550, 200)
    }

    private val hintColor get() = JBColor(Color(150, 170, 170), Color(150, 160, 170))

    private var gameInterfaceListModel: MutableList<GameInterfaceName> = originalGameInterfaceNames.toMutableList()

    private val gameInterfaceNames by lazy {

        val listView = GameInterfaceNamesList(
            project,
            originalGameInterfaceNames
        ) { i, name ->
            if (name != null) {
                if (i >= 0) {
                    gameInterfaceListModel.removeAt(i)
                }
                if (i < 0 || i >= gameInterfaceListModel.size) {
                    gameInterfaceListModel.add(name)
                } else {
                    gameInterfaceListModel.add(i, name)
                }
            }
            setItems(gameInterfaceListModel)
            reload()
        }
        listView.reload()
        JScrollPane(listView).apply {
            maximumSize = preferredTextBoxSize
        }
    }


    val combineAttNodes by lazy {
        JCheckBox().apply {
            this.isSelected = originalCombineAttNodes
        }
    }

    val replicateAttToDuplicateSprites by lazy {
        JCheckBox().apply {
            this.isSelected = originalReplicateAttToDuplicateSprite != false
        }
    }

    val autoPoseCheckbox by lazy {
        JCheckBox().apply {
            this.isSelected = originalIsAutoPoseEnabled
        }
    }

    val panel: JPanel by lazy {
        FormBuilder.createFormBuilder()
            .addLabeledComponent(JLabel("Default Variant"), defaultVariant, 1, false)
            .addLabeledComponent(JLabel("Replicate ATTs to Duplicate Images"), replicateAttToDuplicateSprites, 1, false)
            .addLabeledComponent(JLabel("Combine ATT file nodes"), combineAttNodes, 1, false)
            .addComponent(JLabel("ATT files will be displayed under a single node. i.e. \"*04a\""))
            .addLabeledComponent(JLabel("Ignored File Names"), ignoredFileNames, 1, true)
            .addLabeledComponent(JLabel("Game Interface Names"), gameInterfaceNames, 1, true)
            .addLabeledComponent(JLabel("Enable AutoPose action"), autoPoseCheckbox, 1, false)
            .panel
            .apply {
                this.alignmentY = 0f
            }
    }

    fun getPreferredFocusComponent(): JComponent {
        return defaultVariant
    }

    fun modified(): Boolean {
        if (defaultVariant.selectedItem != projectSettings.defaultVariant)
            return true
        if (combineAttNodes.isSelected != applicationSettings.combineAttNodes) {
            return true
        }
        if (ignoredFileNames.text != projectSettings.ignoredFilenames.joinToString("\n")) {
            return true
        }
        if (gameInterfaceListModel.serialized().equalIgnoringOrder(originalGameInterfaceNamesSerialized)) {
            return true
        }
        if (autoPoseCheckbox.isSelected != applicationSettings.isAutoPoseEnabled) {
            return true
        }
        if (replicateAttToDuplicateSprites.isSelected != applicationSettings.replicateAttToDuplicateSprite) {
            return true
        }
        return false
    }


    /**
     * Apply the current panel's settings to the settings object
     */
    fun applyProjectSettings(): CaosProjectSettingsComponent.State {
        val newSettings = projectSettings.copy(
            defaultVariant = CaosVariant.fromVal(defaultVariant.selectedItem as? String).nullIfUnknown(),
            ignoredFilenames = getIgnoredFileNames(),
        )
        projectSettings = newSettings
        return newSettings
    }

    /**
     * Apply the current panel's settings to the settings object
     */
    fun applyApplicationSettings(): CaosApplicationSettingsComponent.State {
        val gameInterfaceNames = getGameInterfaceNames()
        val newSettings = applicationSettings.copy(
            combineAttNodes = combineAttNodes.isSelected,
            gameInterfaceNames = gameInterfaceNames,
            isAutoPoseEnabled = autoPoseCheckbox.isSelected,
            replicateAttToDuplicateSprite = replicateAttToDuplicateSprites.isSelected
        )
        applicationSettings = newSettings
        return newSettings
    }

    private fun getIgnoredFileNames(): List<String> {
        return ignoredFileNames.text
            .split("\n")
            .mapNotNull {
                it.trim().nullIfEmpty()
            }
    }

    fun reset() {
        defaultVariant.selectedItem = originalDefaultVariant
        defaultVariant.updateUI()
        ignoredFileNames.text = originalIgnoredFilesText
        gameInterfaceListModel.clear()
        gameInterfaceListModel.addAll(originalGameInterfaceNames)
        autoPoseCheckbox.isSelected = originalIsAutoPoseEnabled
        combineAttNodes.isSelected = originalCombineAttNodes
        replicateAttToDuplicateSprites.isSelected = originalReplicateAttToDuplicateSprite
    }

    /**
     * Gets the text area text as a list of interface names
     * If any line is invalid, null is returned, except if forced != true
     * If force == true, then bad lines are removed and ignored and good lines are returned
     */
    private fun getGameInterfaceNames(): List<GameInterfaceName> {
        return gameInterfaceListModel.toList()
    }

}

private class GameInterfaceCell(
    private val project: Project,
    private val list: GameInterfaceNamesList,
    private val hasOwnInterface: Boolean,
    private val onChange: GameInterfaceNamesList.(index: Int, new: GameInterfaceName?) -> Unit,
): JPanel(), Disposable {

    constructor(
        project: Project,
        list: GameInterfaceNamesList,
        onChange: GameInterfaceNamesList.(index: Int, new: GameInterfaceName?) -> Unit,
    ): this(project, list, true, onChange)

    private val label = JLabel("")
    private val edit by lazy {
        JLabel(if (hasOwnInterface) AllIcons.Actions.Edit else AllIcons.General.Add).apply {
            isEnabled = true
            isFocusable = true
            addClickListener { edit() }
        }
    }
    private var delete: JLabel? = null
    private var copy: JLabel? = null
    private var interfaceName: GameInterfaceName? = null
    private var isDisposed = false
    private var index: Int = -1

    private var disposeEditor: (() -> Unit)? = null
    init {
        initUI()
    }

    private fun initUI() {
        // Init panel
        val boxLayout = BoxLayout(this, BoxLayout.LINE_AXIS)
        layout = boxLayout
        preferredSize = Dimension(600, 40)
        maximumSize = Dimension(1200, 40)

        // Init buttons
        add(Box.createHorizontalStrut(7))
        add(label)
        add(Box.createHorizontalGlue())
        add(edit)

        // Initialize other buttons (copy/delete) if this cell has concrete interface
        if (hasOwnInterface) {
            initRegularEditingButtons()
            add(Box.createHorizontalStrut(7))
            add(delete)
            add(Box.createHorizontalStrut(7))
            add(copy)
        } else {
            background = Color(0,0,0, 0)
        }

        // Add space before edge
        add(Box.createHorizontalStrut(7))
    }

    /**
     * Instantiate additional editor buttons for concrete game interface cells
     */
    private fun initRegularEditingButtons() {
        delete = JLabel(AllIcons.Actions.Cancel).apply {
            isEnabled = true
            isFocusable = true
            addClickListener { onDelete() }
        }

        copy = JLabel(AllIcons.Actions.Copy).apply {
            isEnabled = true
            isFocusable = true
            addClickListener { showCopyPanel() }
        }
    }

    private fun onUpdate(interfaceName: GameInterfaceName?) {
        if (interfaceName == null) {
            return
        }
        if (hasOwnInterface) {
            setInterfaceName(index, interfaceName)
        }
        list.onChange(index, interfaceName)
    }

    private fun onDelete() {
        if (!hasOwnInterface) {
            return
        }
        val name = interfaceName
        if (name == null) {
            isVisible = false
            return
        }
        DialogBuilder().apply {
            setTitle("Delete Injector Interface")
            setCenterPanel(JLabel("Are you sure you want to delete CAOS interface '${name.name}'"))
            addOkAction()
            setOkOperation {
                list.onChange(index, null)
                isVisible = false
                // add your code here if necessary
                dialogWrapper.close(DialogWrapper.OK_EXIT_CODE)
                this.dispose()
            }
            addCancelAction()
            setCancelOperation {
                // add your code here if necessary
                dialogWrapper.close(DialogWrapper.CANCEL_EXIT_CODE)
                this.dispose()
            }
            showAndGet()
            disposeEditor?.invoke()
            disposeEditor = null
        }
    }

    fun edit() {
        val injectorInterface = showInjectorDialogAndGet()
        disposeEditor = null
        onUpdate(injectorInterface)
    }

    fun setInterfaceName(index: Int, interfaceName: GameInterfaceName?) {
        if (!hasOwnInterface) {
            LOGGER.severe("CreateNew button should not have an interface name assigned to it")
            return
        }
        this.index = index
        this.interfaceName = interfaceName
        if (interfaceName == null) {
            isVisible = false
            return
        }
        isEnabled = true
        isFocusable = true
        this.label.text = interfaceName.name
        edit.toolTipText = "Edit ${interfaceName.name}"
        delete?.toolTipText = "Delete ${interfaceName.name}"
        copy?.toolTipText = "Copy ${interfaceName.name} with dialog"
    }

    fun showInjectorDialogAndGet(): GameInterfaceName? {
        if (isDisposed) {
            isVisible = false
            return null
        }
        val interfaceName = interfaceName
        val editor = if (interfaceName == null) {
            // Interface name was blank despite trying to edit
            if (hasOwnInterface) {
                isVisible = false
                return null
            }
            val state = CaosProjectSettingsService.getInstance(project).state
            val projectVariant = state.defaultVariant
                ?: state.lastVariant
                ?: project.inferVariantHard()
                ?: CaosVariant.DS
            CreateInjectorDialog(project, projectVariant)
        } else {
            CreateInjectorDialog(project, interfaceName.variant).apply {
                setInterface(interfaceName)
            }
        }
        disposeEditor = editor::dispose
        val updated = editor.showAndGetInterface()
            ?: return null
        disposeEditor = null
        return updated
    }

    fun showCopyPanel() {
        val newInterface = showInjectorDialogAndGet()
        list.onChange(-1, newInterface)
    }


    override fun dispose() {
        if (isDisposed) {
            return
        }
        this.isDisposed = true
    }


}


/**
 * Cell renderer for individual sprite image in Sprite file list
 */
internal class GameInterfaceNamesList(
    private val project: Project,
    private var listItems: List<GameInterfaceName>,
    private val onChange: GameInterfaceNamesList.(index: Int, new: GameInterfaceName?) -> Unit,
) : JPanel() {

    private val pool: MutableList<GameInterfaceCell> = mutableListOf()

    private var color: Color = EditorColorsManager.getInstance().globalScheme.defaultBackground

    private val createNewButton by lazy {
        GameInterfaceCell(project, this, false, onChange)
    }

    init {
        if (color.alpha > 0) {
            this.background = color
            this.isOpaque = true
        }
    }

    private fun get(index: Int): GameInterfaceCell {
        while (index >= pool.size) {
            val component = GameInterfaceCell(project, this, onChange)
            pool.add(component)
            add(component)
        }
        return pool[index]
    }

    fun setItems(newItems: List<GameInterfaceName>) {
        this.listItems = newItems
        val size = newItems.size
        if (pool.size > size) {
            pool.forEachIndexed { i, component ->
                if (i < size) {
                    return
                }
                component.isVisible = false
            }
        }
        reload()
    }

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        reload()
    }

    fun reload() {
        remove(createNewButton)
        listItems.forEachIndexed { i, item ->
            setCell(item, i)
        }
        add(createNewButton)
        revalidate()
        repaint()
    }

    private fun setCell(value: GameInterfaceName, index: Int) {
        val panel = get(index)
        panel.setInterfaceName(index, value)
    }
}

private fun List<GameInterfaceName>.serialized(): List<String> {
    return map { it.serialize() }
}