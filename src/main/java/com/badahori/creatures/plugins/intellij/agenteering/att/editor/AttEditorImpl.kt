package com.badahori.creatures.plugins.intellij.agenteering.att.editor

import com.badahori.creatures.plugins.intellij.agenteering.att.lang.AttFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.cachedVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
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
    private val variant:CaosVariant?


    init {
        variant = getInitialVariant(null, file)
    }

    private fun getInitialVariant(project: Project?, file: VirtualFile) : CaosVariant? {
        val breed = file.name.substring(3, 4)
        val part = file.name.substring(0,1)
        return file.cachedVariant ?: if (breed.toIntSafe()?.let { it >= 0 && it <= 7}.orFalse()) {
            CaosVariant.C1
        } else if (project != null) {
            file.getModule(project)?.variant
                ?: (file.getPsiFile(project) as? AttFile)?.variant
        } else {
            null
        } ?: getVariantByAttLengths(file, part)
    }

    private fun getVariantByAttLengths(file:VirtualFile, part:String) : CaosVariant {
        val contents = file.contents
        val lines = contents.split("[\r\n]+".toRegex())
        if (lines.size == 16) {
            if (part like "a") {
                val longestLine = lines.maxBy { it.length }
                    ?: return CaosVariant.C3
                val points = longestLine.split("\\s+".toRegex()).filter { it.isNotBlank() }
                if (points.lastOrNull()?.toIntSafe().orElse(0) > 0)
                    return CaosVariant.CV
                return CaosVariant.C3
            }
            return CaosVariant.C3
        }
        return CaosVariant.C2
    }

    override fun getComponent(): JComponent {
        return AttEditorPanel(myProject, variant, myFile, spriteFile).`$$$getRootComponent$$$`()
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

    }

    companion object {
        private const val NAME = "ATTEditor"

        @JvmStatic
        fun assumedLinesAndPoints(variant:CaosVariant, part:String) : Pair<Int,Int> {
            val lines = if (variant.isOld) 10 else 16
            val columns = when(part.toUpperCase()) {
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
    }
}
