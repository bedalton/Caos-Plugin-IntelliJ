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
import com.intellij.ide.projectView.ProjectView
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
        val updated = panel.apply(true)
            ?: return
        service.loadState(updated)
        ProjectView.getInstance(project).refresh()
    }

    override fun getDisplayName(): String {
        return "CAOS & Agenteering"
    }

}

private class ProjectSettingsPanel(private var settings: State) {

    private val originalCombineAttNodes = settings.combineAttNodes
    private val originalReplicateAttToDuplicateSprite = settings.replicateAttToDuplicateSprite != false
    private val originalIgnoredFilesText = settings.ignoredFilenames.joinToString("\n")
    private val originalGameInterfaceName: String = settings.gameInterfaceNames.joinToString("\n")
    private val originalDefaultVariant: String = settings.defaultVariant?.code ?: ""
    private val originalIsAutoPoseEnabled: Boolean = settings.isAutoPoseEnabled

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
        Dimension(800, 200)
    }

    private val hintColor get() = JBColor(Color(150, 170, 170), Color(150, 160, 170))

    private val gameInterfaceNames by lazy {

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
        if (defaultVariant.selectedItem != settings.defaultVariant)
            return true
        if (combineAttNodes.isSelected != settings.combineAttNodes) {
            return true
        }
        if (ignoredFileNames.text != settings.ignoredFilenames.joinToString("\n")) {
            return true
        }
        if (gameInterfaceNames.text != settings.gameInterfaceNames.joinToString("\n")) {
            return true
        }
        if (autoPoseCheckbox.isSelected != settings.isAutoPoseEnabled) {
            return true
        }
        if (replicateAttToDuplicateSprites.isSelected != settings.replicateAttToDuplicateSprite) {
            return true
        }
        return false
    }

    /**
     * Apply the current panel's settings to the settings object
     */
    fun apply(force: Boolean): State? {
        val gameInterfaceNames = getGameInterfaceNames(force)
            ?: return null
        val newSettings = settings.copy(
            defaultVariant = CaosVariant.fromVal(defaultVariant.selectedItem as? String).nullIfUnknown(),
            combineAttNodes = combineAttNodes.isSelected,
            ignoredFilenames = getIgnoredFileNames(),
            gameInterfaceNames = gameInterfaceNames,
            isAutoPoseEnabled = autoPoseCheckbox.isSelected,
            replicateAttToDuplicateSprite = replicateAttToDuplicateSprites.isSelected
        )
        settings = newSettings
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
        gameInterfaceNames.text = originalGameInterfaceName
        autoPoseCheckbox.isSelected = originalIsAutoPoseEnabled
        combineAttNodes.isSelected = originalCombineAttNodes
        replicateAttToDuplicateSprites.isSelected = originalReplicateAttToDuplicateSprite
    }

    /**
     * Gets the text area text as a list of interface names
     * If any line is invalid, null is returned, except if forced != true
     * If force == true, then bad lines are removed and ignored and good lines are returned
     * @param force <b>TRUE</b> force values to return filtering out bad values
     */
    private fun getGameInterfaceNames(force: Boolean): List<GameInterfaceName>? {
        val lines = gameInterfaceNames.text
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