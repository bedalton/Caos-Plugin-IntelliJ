package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.caos.action.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.module.CaosModuleConfigurationEditor.Companion.invalidLines
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosProjectSettingsComponent.State
import com.badahori.creatures.plugins.intellij.agenteering.utils.TextPrompt
import com.badahori.creatures.plugins.intellij.agenteering.utils.addChangeListener
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.ui.FormBuilder
import java.awt.Color
import java.awt.Dimension
import javax.swing.*
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultHighlighter


class CaosProjectSettingsConfigurable(private val project: Project) : Configurable {

    private lateinit var panel: ProjectSettingsPanel

    private val service: CaosProjectSettingsService? by lazy {
        if (project.isDisposed)
            return@lazy null
        CaosProjectSettingsService.getInstance(project)
    }

    override fun reset() {
        panel.reset()
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return panel.getPreferredFocusComponent()
    }

    private val settings: State? by lazy {
        service?.state
    }

    override fun createComponent(): JComponent? {
        if (this::panel.isInitialized)
            return this.panel.panel
        val settings = settings
            ?: return null
        val panel = ProjectSettingsPanel(settings)
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
        val service = service
            ?: return
        val updated = panel.applyToProjectState(true)
            ?: return
        val updateApplicationState = panel.applyToApplicationSettings(true)
            ?: return
        CaosApplicationSettings.state = updateApplicationState
        service.loadState(updated)
    }

    override fun getDisplayName(): String {
        return "CAOS & Agenteering"
    }

}

private class ProjectSettingsPanel(private val settings: State) {

    private val originalCombineAttNodes = settings.combineAttNodes
    private val originalIgnoredFilesText = settings.ignoredFilenames.joinToString("\n")
    private val originalGameInterfaceName: String = (CaosApplicationSettingsService.getInstance().gameInterfaceNamesRaw + settings.gameInterfaceNames).toSet().joinToString("\n")
    private val originalDefaultVariant: String = settings.defaultVariant?.code ?: ""
    private val originalIsAutoPoseEnabled: Boolean = settings.isAutoPoseEnabled ?: CaosApplicationSettings.isAutoPoseEnabled

    private var mInterfaceNamesAreValid = true
    val interfaceNamesAreValid get() = mInterfaceNamesAreValid

    private val defaultVariantComboBox by lazy {
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

    private val ignoredFileNamesTextArea by lazy {
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
        Dimension(800, 200)
    }

    private val hintColor get() = JBColor(Color(150, 170, 170), Color(150, 160, 170))

    private val gameInterfaceNamesTextArea by lazy {

        JTextArea().apply {
            this.preferredSize = preferredTextBoxSize
            this.border = BorderFactory.createLineBorder(Color.WHITE)
            val errorHighlighter = DefaultHighlighter.DefaultHighlightPainter(JBColor.RED)
            TextPrompt(CaosBundle.message("caos.settings.injector-names.placeholder"), this)
                .apply {
                    this.foreground = hintColor
                }

            this.addChangeListener {
                val text = this.text
                val invalidLines = invalidLines(text)
                this.highlighter.removeAllHighlights()
                mInterfaceNamesAreValid = invalidLines.isEmpty()
                for (lineNumber in invalidLines) {
                    try {
                        val startIndex = this.getLineStartOffset(lineNumber)
                        val endIndex = this.getLineEndOffset(lineNumber)
                        this.highlighter.addHighlight(startIndex, endIndex, errorHighlighter)
                    } catch (ignored: BadLocationException) {

                    }
                }
            }
        }
    }


    val combineAttNodesCheckbox by lazy {
        JCheckBox().apply {
            this.isSelected = originalCombineAttNodes
        }
    }

    val autoPoseCheckbox by lazy {
        JCheckBox().apply {
            this.isSelected = originalIsAutoPoseEnabled
        }
    }

    val panel: JPanel by lazy {
        FormBuilder.createFormBuilder()
            .addLabeledComponent(JLabel("Default Variant"), defaultVariantComboBox, 1, false)
            .addLabeledComponent(JLabel("Combine ATT file nodes"), combineAttNodesCheckbox, 1, false)
            .addComponent(JLabel("ATT files will be displayed under a single node. i.e. \"*04a\""))
            .addLabeledComponent(JLabel("Ignored File Names"), ignoredFileNamesTextArea, 1, true)
            .addLabeledComponent(JLabel("Game Interface Names"), gameInterfaceNamesTextArea, 1, true)
            .addLabeledComponent(JLabel("Game Interface Names"), gameInterfaceNamesTextArea, 1, true)
            .addLabeledComponent(JLabel("Enable AutoPose action"), autoPoseCheckbox, 1, false)
            .panel
            .apply {
                this.alignmentY = 0f
            }
    }

    fun getPreferredFocusComponent(): JComponent {
        return defaultVariantComboBox
    }

    fun modified(): Boolean {
        if (defaultVariantComboBox.selectedItem != originalDefaultVariant)
            return true
        if (combineAttNodesCheckbox.isSelected == originalCombineAttNodes) {
            return true
        }
        if (ignoredFileNamesTextArea.text != originalIgnoredFilesText)
            return true
        if (gameInterfaceNamesTextArea.text != originalGameInterfaceName)
            return true
        if (autoPoseCheckbox.isSelected != originalIsAutoPoseEnabled)
            return true
        return false
    }

    fun applyToProjectState(force: Boolean): State? {
        val gameInterfaceNames = getGameInterfaceNames(force)
            ?: return null
        return settings.copy(
            defaultVariant = CaosVariant.fromVal(defaultVariantComboBox.selectedItem as? String).nullIfUnknown(),
            combineAttNodes = combineAttNodesCheckbox.isSelected,
            ignoredFilenames = getIgnoredFileNames(),
            gameInterfaceNames = gameInterfaceNames,
        )
    }

    fun applyToApplicationSettings(force: Boolean): CaosApplicationSettingsComponent.State? {
        val initialState = CaosApplicationSettingsService.getInstance()
            .state
        val gameInterfaceNames = getGameInterfaceNames(force)
            ?: return null
        return initialState.copy(
            gameInterfaceNames = gameInterfaceNames,
            autoPoseEnabled = autoPoseCheckbox.isSelected
        )
    }

    private fun getIgnoredFileNames(): List<String> {
        return ignoredFileNamesTextArea.text
            .split("\n")
            .mapNotNull {
                it.trim().nullIfEmpty()
            }
    }

    fun reset() {
        defaultVariantComboBox.selectedItem = originalDefaultVariant
        defaultVariantComboBox.updateUI()
        ignoredFileNamesTextArea.text = originalIgnoredFilesText
        gameInterfaceNamesTextArea.text = originalGameInterfaceName
        autoPoseCheckbox.isSelected = originalIsAutoPoseEnabled
        combineAttNodesCheckbox.isSelected = originalCombineAttNodes
    }

    /**
     * Gets the text area text as a list of interface names
     * If any line is invalid, null is returned, except if forced != true
     * If force == true, then bad lines are removed and ignored and good lines are returned
     * @param force <b>TRUE</b> force values to return filtering out bad values
     */
    private fun getGameInterfaceNames(force: Boolean): List<GameInterfaceName>? {
        val lines = gameInterfaceNamesTextArea.text
            .split("\n")
            .mapNotNull {
                it.trim().nullIfEmpty()
            }

        if (!force) {
            val out = mutableListOf<GameInterfaceName>()
            for (line in lines) {
                val interfaceName = GameInterfaceName.fromString(line)
                    ?: return null
                out.add(interfaceName)
            }
            return out
        }

        return lines.mapNotNull { line ->
            GameInterfaceName.fromString(line)
        }
    }

}