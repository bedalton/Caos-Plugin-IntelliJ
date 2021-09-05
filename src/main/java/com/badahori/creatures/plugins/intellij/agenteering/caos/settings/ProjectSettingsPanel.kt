package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.caos.action.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.project.module.CaosModuleConfigurationEditor.Companion.invalidLines
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosProjectSettingsComponent.State
import com.badahori.creatures.plugins.intellij.agenteering.utils.addChangeListener
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.ui.FormBuilder
import javax.swing.*
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultHighlighter


class CaosProjectSettingsConfigurable(private val project: Project): Configurable {

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
        service?.getState()
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
    }

    override fun getDisplayName(): String {
        return "CAOS & Agenteering"
    }

}

private class ProjectSettingsPanel(private val settings: State) {

    private val originalIgnoredFilesText = settings.ignoredFilenames.joinToString("\n")
    private val originalGameInterfaceName: String = settings.gameInterfaceNames.joinToString("\n")
    private val originalDefaultVariant: String = settings.defaultVariant?.code ?: ""

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
        val placeholder = JLabel("New line delimited list of file names to ignore")
        JTextArea().apply {
            this.add(placeholder)
            this.addChangeListener {
                placeholder.isVisible = this.text.isEmpty()
            }
        }
    }

    private val gameInterfaceNames by lazy {
        val placeholder = JLabel(
            "Format: {VARIANT}:{machine.cfg game name}[{Display name:Optional}]\n" +
                "examples: \n" +
                "\tDS:Other-DS-Instance[Other DS Display Name]\n" +
                "\tC3:AnotherInstance\n" +
                "\t*:AnyVariantInterface[Name for Anything injector]"
        )
        JTextArea().apply {
            this.add(placeholder)
            val errorHighlighter = DefaultHighlighter.DefaultHighlightPainter(JBColor.RED)
            this.addChangeListener {
                val text = this.text
                placeholder.isVisible = text.isEmpty()
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

    val panel: JPanel by lazy {
        FormBuilder.createFormBuilder()
            .addLabeledComponent(JLabel("Default Variant"), defaultVariant, 1, false)
            .addLabeledComponent(JLabel("Ignored File Names"), ignoredFileNames, 1, true)
            .addLabeledComponent(JLabel("Game Interface Names"), gameInterfaceNames, 1, true)
            .panel
    }

    fun getPreferredFocusComponent(): JComponent {
        return defaultVariant
    }

    fun modified(): Boolean {
        if (defaultVariant.selectedItem != originalDefaultVariant)
            return true
        if (ignoredFileNames.text != originalIgnoredFilesText)
            return true
        if (gameInterfaceNames.text != originalGameInterfaceName)
            return true
        return false
    }

    fun apply(force: Boolean): State? {
        val gameInterfaceNames = getGameInterfaceNames(force)
            ?: return null
        return settings.copy(
            defaultVariant = CaosVariant.fromVal(defaultVariant.selectedItem as? String).nullIfUnknown(),
            ignoredFilenames = getIgnoredFileNames(),
            gameInterfaceNames = gameInterfaceNames
        )
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