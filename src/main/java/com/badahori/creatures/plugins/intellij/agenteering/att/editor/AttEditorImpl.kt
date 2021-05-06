package com.badahori.creatures.plugins.intellij.agenteering.att.editor

import com.badahori.creatures.plugins.intellij.agenteering.att.lang.getInitialVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.formatting.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.sprites.flipHorizontal
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser.parse
import com.badahori.creatures.plugins.intellij.agenteering.utils.VariantFilePropertyPusher
import com.badahori.creatures.plugins.intellij.agenteering.utils.notLike
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
internal class AttEditorImpl(
    project: Project?,
    file: VirtualFile,
    private val spriteFile: VirtualFile
) : UserDataHolderBase(), FileEditor {
    private val myFile: VirtualFile = file
    private var myProject: Project? = project
    private val variant: CaosVariant?

    private lateinit var panel: AttEditorPanel

    init {
        variant = getInitialVariant(project, file)
    }
    override fun getComponent(): JComponent {
        panel = AttEditorPanel(myProject, variant, myFile, spriteFile)
        return panel.`$$$getRootComponent$$$`()
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return panel.scrollPane
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
        if (this::panel.isInitialized)
            this.panel.dispose()
    }

    override fun <T> getUserData(key: Key<T>): T? {
        return myFile.getUserData(key)
    }

    override fun <T> putUserData(key: Key<T>, value: T?) {
        myFile.putUserData(key, value)
    }

    override fun selectNotify() {

    }

    companion object {
        private const val NAME = "ATTEditor"

        @JvmStatic
        fun assumedLinesAndPoints(variant: CaosVariant, part: String): Pair<Int, Int> {
            val lines = if (variant.isOld) 10 else 16
            val columns = when (part.toUpperCase()) {
                "A" -> when {
                    variant.isOld -> 2
                    else -> 5
                }
                "B" -> 6
                "Q" -> 1
                else -> 2
            }
            return Pair(lines, columns)
        }

        @JvmStatic
        fun getImages(variant: CaosVariant, part: String, spriteFile: VirtualFile): List<BufferedImage?> {
            val images = parse(spriteFile).images
            if (part notLike "a" || variant != CaosVariant.CV)
                return images
            return images.mapIndexed { i, image ->
                var out: BufferedImage? = image
                if (i % 16 in 4..7) {
                    val repeated =
                        image == null || (image.width == 32 && image.height == 32) || image.isCompletelyTransparent
                    if (repeated) {
                        try {
                            out = images[i - 4]?.flipHorizontal() ?: image
                            if (out?.width != image?.width || out?.height != image?.height) {
                                LOGGER.severe("Failed to properly maintain scale in image.")
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
                out
            }
        }

        @JvmStatic
        fun cacheVariant(virtualFile: VirtualFile, variant: CaosVariant) {
            virtualFile.putUserData(CaosScriptFile.VariantUserDataKey, variant)
            VariantFilePropertyPusher.writeToStorage(virtualFile, variant)
        }

    }
}

private val BufferedImage.isCompletelyTransparent: Boolean
    get() {
        for (i in 0 until this.height) {
            for (j in 0 until this.width) {
                if (this.isTransparent(j, i)) {
                    return false
                }
            }
        }
        return true
    }

fun BufferedImage.isTransparent(x: Int, y: Int): Boolean {
    val pixel = this.getRGB(x, y)
    return pixel shr 24 == 0x00
}