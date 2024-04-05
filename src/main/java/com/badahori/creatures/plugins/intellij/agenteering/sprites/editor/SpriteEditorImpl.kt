@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.sprites.editor

import com.bedalton.creatures.sprite.parsers.PhotoAlbum
import com.bedalton.creatures.sprite.parsers.SPR_SHORT_DEBUG_LOGGING
import com.bedalton.creatures.sprite.parsers.image
import com.bedalton.log.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.ensureMacOsCopyLib
import com.badahori.creatures.plugins.intellij.agenteering.utils.md5
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import korlibs.image.awt.toAwt
import korlibs.image.bitmap.Bitmap32
import java.awt.image.BufferedImage
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JLabel


/**
 * Sprite viewer (eventually will be editor) for various Creatures file types
 */
internal class SpriteEditorImpl(project: Project?, file: VirtualFile) : UserDataHolderBase(), FileEditor, DumbAware {
    private var myFile: VirtualFile = file
    private var myProject: Project? = project
    private lateinit var editor:SprFileEditor

    private val invalidatedLabel: JComponent by lazy {
        JLabel("Sprite file has been invalidated")
    }

    override fun getComponent(): JComponent {
        if (!isValid) {
            return invalidatedLabel
        }
        if (this::editor.isInitialized) {
            return editor.component
        }
        Log.setMode(SPR_SHORT_DEBUG_LOGGING, true)
        try {
            ensureMacOsCopyLib()
        } catch (_: Exception) {
        }
        val editor = SprFileEditor(myProject, myFile)
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

    override fun dispose() {
        myFile.putUserData(CACHE_MD5_KEY, null)
        myProject = null
    }

    override fun <T> getUserData(key: Key<T>): T? {
        if (!isValid) {
            return null
        }
        return myFile.getUserData(key)
    }

    override fun <T> putUserData(key: Key<T>, value: T?) {
        if (!isValid) {
            return
        }
        myFile.putUserData(key, value)
    }

    override fun selectNotify() {
        if (!isValid) {
            return
        }

        if (this::editor.isInitialized) {
            editor.init()
        }

        val cachedMD5: String? = myFile.getUserData(CACHE_MD5_KEY)
        val currentMD5 = myFile.md5()
        if (cachedMD5 == currentMD5) {
            return
        }

        clearCache(myFile)
        if (this::editor.isInitialized) {
            editor.reloadSprite()
        }
    }

    override fun getFile(): VirtualFile {
        return myFile
    }

    companion object {
        private const val NAME = "SPREditor"
        private val CACHE_MD5_KEY = Key<String>("creatures.sprites.PARSED_IMAGES_MD5")
        private val CACHE_KEY = Key<List<List<Bitmap32>>>("creatures.sprites.PARSED_IMAGES")

        @JvmStatic
        fun cache(virtualFile: VirtualFile, images:List<List<Bitmap32>>) {
            if (!virtualFile.isValid || virtualFile.extension?.lowercase() == "blk") {
                return
            }
            virtualFile.putUserData(CACHE_MD5_KEY, null)
            virtualFile.putUserData(CACHE_KEY, null)
            if (images.isEmpty()) {
                return
            }
            val md5 = virtualFile.md5()
                ?: return
            virtualFile.putUserData(CACHE_KEY, images)
            virtualFile.putUserData(CACHE_MD5_KEY, md5)
        }

        @JvmStatic
        fun fromCache(virtualFile: VirtualFile) : List<List<Bitmap32>>? {
            val cachedMD5 = virtualFile.getUserData(CACHE_MD5_KEY)
                ?: return null
            if (cachedMD5 != virtualFile.md5())
                return null
            return virtualFile.getUserData(CACHE_KEY).nullIfEmpty()
        }

        @JvmStatic
        fun fromCacheAsAwt(virtualFile: VirtualFile) : List<List<BufferedImage>>? {
            if (!virtualFile.isValid) {
                return null
            }
            return fromCache(virtualFile)?.map { file ->
                file.map(Bitmap32::toAwt)
            }
        }

        @JvmStatic
        fun toBitmapList(album: PhotoAlbum): List<Bitmap32> {
            return album.photos.map { it.image }
        }

        @JvmStatic
        fun toBufferedImages(album: PhotoAlbum): List<BufferedImage> {
            return album.photos.map { it.image.toAwt() }
        }

        @JvmStatic
        fun clearCache(virtualFile: VirtualFile) {
            if (!virtualFile.isValid) {
                return
            }
            virtualFile.putUserData(CACHE_MD5_KEY, null)
            virtualFile.putUserData(CACHE_KEY, null)
        }
    }
}
