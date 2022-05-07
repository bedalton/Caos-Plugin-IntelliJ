package com.badahori.creatures.plugins.intellij.agenteering.att.editor

import com.badahori.creatures.plugins.intellij.agenteering.att.lang.getInitialVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.util.PopupUtil
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import java.beans.PropertyChangeListener
import javax.swing.JComponent


/**
 * Sprite viewer (eventually will be editor) for various Creatures file types
 */
internal class AttEditorImpl(
    project: Project,
    file: VirtualFile,
    spriteFile: VirtualFile,
) : UserDataHolderBase(), FileEditor, DumbAware {
    private val myFile: VirtualFile = file
    private var myProject: Project = project
    private val variant: CaosVariant = getInitialVariant(project, file)

    private val controller by lazy {
        AttEditorController(
            project,
            file,
            spriteFile,
            variant,
            this::showFooterNotification
        )
    }

    override fun getComponent(): JComponent {
        return controller.getComponent() as JComponent
    }

    override fun getFile(): VirtualFile {
        return myFile
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return component
    }

    override fun getName(): String {
        return NAME
    }

    override fun setState(state: FileEditorState) {}

    override fun isModified(): Boolean {
        return false
    }

    override fun isValid(): Boolean {
        return !myProject.isDisposed && myFile.isValid
    }

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
    override fun getCurrentLocation(): FileEditorLocation? {
        return null
    }

    override fun deselectNotify() {
        if (myProject.isDisposed || DumbService.isDumb(myProject) || !controller.isInitialized) {
            return
        }
        controller.clearPose()
    }

    override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? {
        return null
    }

    override fun dispose() {
        this.controller.dispose()
    }

    override fun <T> getUserData(key: Key<T>): T? {
        return myFile.getUserData(key)
    }

    override fun <T> putUserData(key: Key<T>, value: T?) {
        myFile.putUserData(key, value)
    }


    /**
     * Callback called when custom editor window gains focus
     * Is not called if file is selected but in text view
     */
    override fun selectNotify() {
        if (myProject.isDisposed) {
            return
        }
        if (controller.isInitialized) {
            if (DumbService.isDumb(myProject)) {
                DumbService.getInstance(myProject).runWhenSmart(::selectNotify)
            }
            controller.view.refresh()
            controller.view.scrollCellIntoView()
        }
    }


    private fun showFooterNotification(message: String, messageType: MessageType) {
        PopupUtil.showBalloonForComponent(controller.getPopupMessageTarget(), message, messageType, true, null)
    }


    companion object {
        private const val NAME = "ATTEditor"
    }
}

