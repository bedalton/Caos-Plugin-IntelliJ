package com.badahori.creatures.plugins.intellij.agenteering.sprites.editor

import com.badahori.creatures.plugins.intellij.agenteering.utils.md5
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import java.awt.image.BufferedImage
import java.beans.PropertyChangeListener
import javax.swing.JComponent


/**
 * Sprite viewer (eventually will be editor) for various Creatures file types
 */
internal class SpriteEditorImpl : UserDataHolderBase, FileEditor {
    private var myFile: VirtualFile
    private var myProject: Project? = null
    private lateinit var editor:SprFileEditor

    constructor(file: VirtualFile) {
        myFile = file
    }

    constructor(project: Project?, file: VirtualFile) {
        myFile = file
        myProject = project
    }

    override fun getComponent(): JComponent {
        if (this::editor.isInitialized)
            return editor.component
        val editor = SprFileEditor(myFile)
        editor.init()
        this.editor = editor
        return editor.component
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

    override fun deselectNotify() {

    }

    override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? {
        return null
    }

    override fun dispose() {}

    override fun <T> getUserData(key: Key<T>): T? {
        return myFile.getUserData(key);
    }

    override fun <T> putUserData(key: Key<T>, value: T?) {
        myFile.putUserData(key, value)
    }

    override fun selectNotify() {
        myFile.getUserData(CACHE_MD5_KEY)?.let { cachedMD5 ->
            if (cachedMD5 == myFile.md5())
                return
        }
        if (this::editor.isInitialized)
            editor.init()
    }

    override fun getFile(): VirtualFile {
        return myFile
    }

    companion object {
        private const val NAME = "SPREditor"
        private val CACHE_MD5_KEY = Key<String>("creatures.sprites.PARSED_IMAGES_MD5")
        private val CACHE_KEY = Key<List<BufferedImage>>("creatures.sprites.PARSED_IMAGES")

        @JvmStatic
        fun cache(virtualFile: VirtualFile, images:List<BufferedImage>) {
            virtualFile.putUserData(CACHE_MD5_KEY, null)
            virtualFile.putUserData(CACHE_KEY, null)
            if (images.isEmpty())
                return
            val md5 = virtualFile.md5()
                ?: return
            virtualFile.putUserData(CACHE_KEY, images)
            virtualFile.putUserData(CACHE_MD5_KEY, md5)
        }

        @JvmStatic
        fun fromCache(virtualFile: VirtualFile) : List<BufferedImage>? {
            val cachedMD5 = virtualFile.getUserData(CACHE_MD5_KEY)
                ?: return null
            if (cachedMD5 != virtualFile.md5())
                return null
            return virtualFile.getUserData(CACHE_KEY).nullIfEmpty()
        }
    }
}
