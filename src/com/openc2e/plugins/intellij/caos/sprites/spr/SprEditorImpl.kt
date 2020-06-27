package com.openc2e.plugins.intellij.caos.sprites.spr

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import java.beans.PropertyChangeListener
import java.util.*
import javax.swing.JComponent


class SprEditorImpl : UserDataHolderBase, FileEditor {
    private var myFile: VirtualFile
    private var myProject: Project? = null

    constructor(file: VirtualFile) {
        myFile = file
    }

    constructor(project: Project?, file: VirtualFile) {
        myFile = file
        myProject = project
    }

    override fun getComponent(): JComponent {
        return SprFileEditor(myFile).`$$$getRootComponent$$$`()
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return null
    }

    override fun getName(): String {
        return NAME
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

    override fun dispose() {}
    override fun <T> getUserData(key: Key<T>): T? {
        return null
    }

    override fun <T> putUserData(key: Key<T>, value: T?) {}

    companion object {
        private const val NAME = "SPREditor"
    }
}
