package com.badahori.creatures.plugins.intellij.agenteering.sprites.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile


/**
 * Editor provider for sprite files. Wires up Sprite files to the viewer
 */
class SpriteFileEditorProvider : FileEditorProvider, DumbAware {


    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.extension?.lowercase() in spriteFileTypes
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return SpriteEditorImpl(project, file)
    }

    override fun disposeEditor(editor: FileEditor) {
        Disposer.dispose(editor)
    }

    override fun getEditorTypeId(): String {
        return EDITOR_TYPE_ID
    }

    override fun getPolicy(): FileEditorPolicy {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR
    }
    companion object {
        private val spriteFileTypes = listOf("spr", "back",  "blk", "c16", "s16")
        private const val EDITOR_TYPE_ID = "creature.spr"
    }
}