package com.badahori.creatures.plugins.intellij.agenteering.sprites.editor

import bedalton.creatures.sprite.parsers.BlkParser
import bedalton.creatures.common.structs.Pointer
import bedalton.creatures.common.util.FileNameUtil
import bedalton.creatures.common.util.className
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosProjectSettingsService
import com.badahori.creatures.plugins.intellij.agenteering.common.saveImageWithDialog
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.sprites.editor.SpriteEditorImpl.Companion.cache
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.vfs.VirtualFileStreamReaderEx
import com.bedalton.io.bytes.ByteStreamReaderBase
import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.notification.Notification
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBScrollPane
import com.soywiz.korim.awt.toAwt
import kotlinx.coroutines.runBlocking
import java.awt.Dimension
import java.awt.FileDialog
import java.awt.Frame
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.beans.PropertyChangeListener
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.*
import kotlin.math.ceil


/**
 * Sprite viewer (eventually will be editor) for various Creatures file types
 */
internal class BlkPreviewViewImpl(project: Project, file: VirtualFile) : UserDataHolderBase(), FileEditor {
    private val myFile: VirtualFile = file
    private val myProject: Project = project
    private lateinit var mComponent: JScrollPane
    private lateinit var mImage: BufferedImage
    private var isLoading = AtomicBoolean()
    private lateinit var loadingLabel: JLabel

    override fun getComponent(): JScrollPane {
        if (this::mComponent.isInitialized)
            return mComponent
        val component = JBScrollPane()
        loadingLabel = object : JLabel("Stitching BLK...", SwingConstants.CENTER) {
            override fun getPreferredSize(): Dimension {
                return component.size
            }
        }
        component.setViewportView(loadingLabel)

        mComponent = component
        return component
    }

    private fun setImage(image: BufferedImage) {
        mImage = image
        val size = Dimension(image.width, image.height)
        val component = component
        component.verticalScrollBar.unitIncrement = 16
        component.horizontalScrollBar.unitIncrement = 16
        val label = ImagePanel(myProject, image, myFile.path, myFile.name)
        label.size = size
        label.minimumSize = size
        label.preferredSize = size
        component.remove(loadingLabel)
        component.setViewportView(label)
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return null
    }

    override fun getName(): String {
        return NAME
    }

    override fun getFile(): VirtualFile {
        return myFile
    }

    override fun setState(state: FileEditorState) {}
    override fun isModified(): Boolean {
        return false
    }

    override fun isValid(): Boolean {
        return myFile.isValid
    }

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
    override fun getCurrentLocation(): FileEditorLocation? {
        return null
    }

    override fun deselectNotify() {

    }

    override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? {
        return null
    }

    override fun dispose() {}

    override fun <T> getUserData(key: Key<T>): T? {
        return myFile.getUserData(key)
    }

    override fun <T> putUserData(key: Key<T>, value: T?) {
        myFile.putUserData(key, value)
    }

    override fun selectNotify() {
        stitchImage()
    }

    private fun stitchImage() {
        if (isLoading.getAndSet(true)) {
            return
        }
        if (this::mImage.isInitialized) {
            isLoading.set(false)
            return
        }

        SpriteEditorImpl.fromCacheAsAwt(file)?.getOrNull(0)?.let { image ->
            setImage(image.first())
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            runBlocking {
                stitchActual()
            }
        }
    }

    private fun shouldTrim(): Boolean {
        val project = myProject

        val settings = CaosProjectSettingsService
            .getInstance(project)
        val trim = settings.trimBLKs

        if (trim != null) {
            return trim
        }

        val actionTaken = Pointer(false)
        val notificationPointer: Pointer<Notification?> = Pointer(null)
        val doNotTrim = object: AnAction(
            "Do Not Trim BLKs"
        ) {
            override fun update(e: AnActionEvent) {
                super.update(e)
                if (actionTaken.value) {
                    e.presentation.isEnabled = false
                }
            }
            override fun actionPerformed(e: AnActionEvent) {
                runBlocking {
                    actionTaken.value = true
                    settings.trimBLKs = false
                    stitchActual()
                    notificationPointer.value?.expire()
                }
            }
        }

        val keepTrimming = object: AnAction(
            "Keep Trimming BLKs"
        ) {
            override fun update(e: AnActionEvent) {
                super.update(e)
                if (actionTaken.value) {
                    e.presentation.isEnabled = false
                }
            }
            override fun actionPerformed(e: AnActionEvent) {
                actionTaken.value = true
                settings.trimBLKs = true
                notificationPointer.value?.expire()
            }
        }

        notificationPointer.value = CaosNotifications.createInfoNotification(
            project,
            "BLK Parse: ${file.name}",
            "Black edges on the right and bottom sides were trimmed by default.\nIs this okay?",
        )
            .addAction(doNotTrim)
            .addAction(keepTrimming)
            .show()

        return true
    }

    private suspend fun stitchActual() {
        try {
            val reader = ByteStreamReaderBase(VirtualFileStreamReaderEx(file))
            var percent = 0
            val stitched = BlkParser.parse(
                reader,
                trim = shouldTrim(),
                10
            ) { i: Int, total: Int ->
                val progress = ceil((i * 100.0) / total).toInt()
                if (percent < progress) {
                    percent = progress
                    invokeLater {
                        loadingLabel.text = "Parsing BLK... $progress%      $i/$total"
                    }
                }
                null
            }
            cache(file, listOf(listOf(stitched)))
            setImage(stitched.toAwt())
        } catch (e: Exception) {
            val error = if (e.message?.length.orElse(0) > 0) {
                "${e::className}: ${e.message}"
            } else {
                "${e::className}"
            }
            LOGGER.severe(error)
            e.printStackTrace()
            showError(myProject, error)
        } finally {
            isLoading.set(false)
        }
    }

    private fun showError(myProject: Project?, error: String) {
        ApplicationManager.getApplication().invokeLater {
            if (myProject == null || myProject.isDisposed) {
                return@invokeLater
            }
            CaosNotifications.showWarning(
                myProject,
                "BLK Preview",
                "Failed to draw all cells in BLK preview pane"
            )
            component.setViewportView(JLabel("Failed to decompile sprite for stitching. $error"))
            isLoading.set(false)
        }
    }

    companion object {
        private const val NAME = "Stitched Preview"
    }
}


private class ImagePanel(
    private val project: Project,
    val mImage: BufferedImage,
    defaultDirectory: String?,
    val fileName: String,
) : JLabel(ImageIcon(mImage)) {
    private val defaultDirectory: String? = (defaultDirectory ?: System.getProperty("user.home")).nullIfEmpty()?.let {
        if (File(it).exists())
            it
        else
            null
    }
    var lastDirectory: String? = null
    private val popUp = PopUp()

    private fun initHandlers() {
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.button == MouseEvent.BUTTON3) {
                    showPopUp(e)
                }
            }

            override fun mousePressed(e: MouseEvent) {
                if (e.button == MouseEvent.BUTTON3) {
                    showPopUp(e)
                }
            }
        })
    }

    private fun showPopUp(e: MouseEvent) {
        if (e.isPopupTrigger || e.modifiersEx or KeyEvent.CTRL_DOWN_MASK == KeyEvent.CTRL_DOWN_MASK && e.button == 1) {
            popUp.show(e.component, e.x, e.y)
        }
    }

    fun saveImageAs() {
        if (project.isDisposed) {
            return
        }
        val image = mImage
        val fileChooser = FileDialog(null as Frame?)
        fileChooser.mode = FileDialog.SAVE
        fileChooser.isMultipleMode = false
        fileChooser.title = "Specify a file to save"
        var targetDirectory: File? = null
        val lastDirectory = lastDirectory
        if (lastDirectory != null && lastDirectory.length > 3) {
            targetDirectory = File(lastDirectory)
        }
        val defaultDirectory = defaultDirectory
        if ((targetDirectory == null || !targetDirectory.exists()) && defaultDirectory != null) {
            targetDirectory = File(defaultDirectory)
        }
        if (targetDirectory?.exists() != true) {
            targetDirectory = null
        }
        val defaultName = FileNameUtil.getFileNameWithoutExtension(fileName)?.let {
            "$it.png"
        }
        saveImageWithDialog(
            project,
            "BLK",
            targetDirectory?.path,
            defaultName,
            image
        )
    }


    fun copyToClipboard() {
        mImage.copyToClipboard()
    }


    inner class PopUp : JPopupMenu() {
        init {
            var item = JMenuItem("Save image as..")
            item.addActionListener { saveImageAs() }
            add(item)
            item = JMenuItem("Copy image to clipboard")
            item.addActionListener { copyToClipboard() }
            add(item)
        }
    }

    init {
        initHandlers()
    }

}