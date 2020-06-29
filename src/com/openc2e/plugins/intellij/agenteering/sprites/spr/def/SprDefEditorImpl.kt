package com.openc2e.plugins.intellij.agenteering.sprites.spr.def

import com.intellij.openapi.editor.EditorDropHandler
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.openc2e.plugins.intellij.agenteering.caos.utils.contents
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.beans.PropertyChangeListener
import java.io.File
import javax.swing.JComponent


class SprDefEditorImpl(private val project: Project, private val myFile: VirtualFile) : UserDataHolderBase(), FileEditor, EditorDropHandler {

    var getImages: (() -> List<SprDefImageData>) = { emptyList() }
    var addImages: ((data: List<SprDefImageData>) -> Unit) = { }
    var originalContents = myFile.contents
    override fun getComponent(): JComponent {
        return SprDefFileEditor(project, myFile).let {
            getImages = {
                it.imageData
            }
            addImages = { data ->
                it.addImages(data)
            }
            it.`$$$getRootComponent$$$`()
        }
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return null
    }

    override fun getName(): String {
        return NAME
    }

    override fun setState(state: FileEditorState) {
    }

    override fun isModified(): Boolean {
        val module = ModuleUtil.findModuleForFile(myFile, project)?.moduleFile
        val contents = getImages().joinToString("\n") { data ->
            data.virtualFile?.let {
                if (module != null)
                    VfsUtil.findRelativePath(module, it, File.separator.first())
                else
                    "::" + VfsUtil.findRelativePath(myFile, it, File.separator.first())
            } ?: data.relativePath
        }
        return contents == originalContents
    }

    override fun isValid(): Boolean {
        return myFile.isValid
    }

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
    override fun getCurrentLocation(): FileEditorLocation? {
        return null
    }

    override fun dispose() {
        getImages = { emptyList() }
        addImages = { }
    }

    override fun <T> getUserData(key: Key<T>): T? {
        return myFile.getUserData(key)
    }

    override fun <T> putUserData(key: Key<T>, value: T?) {
        myFile.putUserData(key, value)
    }

    companion object {
        private const val NAME = "SPREditor"
    }

    override fun canHandleDrop(flavor: Array<out DataFlavor>): Boolean {
        if (flavor.isEmpty())
            return false
        return SprImageDataFlavor in flavor || DataFlavor.javaFileListFlavor in flavor
    }

    override fun handleDrop(transferable: Transferable?, project: Project, editor: EditorWindow) {
        if (transferable == null)
            return
        val data = SprDefEditorUtil.parseTransferable(project, myFile, transferable, getImages().size)
                ?: return
        addImages(data)
    }
}
