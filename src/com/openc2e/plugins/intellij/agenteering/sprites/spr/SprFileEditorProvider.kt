package com.openc2e.plugins.intellij.agenteering.sprites.spr

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.openc2e.plugins.intellij.agenteering.sprites.spr.def.SprDefEditorImpl


class SprFileEditorProvider : FileEditorProvider, DumbAware {

    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.extension == "spr" || file.extension == "sprdef"
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return if (file.extension == "sprdef") SprDefEditorImpl(project, file) else SprEditorImpl(project, file)
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
        private const val EDITOR_TYPE_ID = "creature.spr"
    }
}