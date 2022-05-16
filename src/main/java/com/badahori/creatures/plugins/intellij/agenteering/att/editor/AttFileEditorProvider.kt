package com.badahori.creatures.plugins.intellij.agenteering.att.editor

import com.badahori.creatures.plugins.intellij.agenteering.att.actions.getAnyPossibleSprite
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.badahori.creatures.plugins.intellij.agenteering.utils.lowercase
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile


/**
 * Editor provider for sprite files. Wires up Sprite files to the viewer
 */
class AttFileEditorProvider : FileEditorProvider {

    override fun accept(project: Project, file: VirtualFile): Boolean {
        if (project.isDisposed) {
            return false
        }

//        val lines = file.contents.split("\r?\n".toRegex()).filter { it.isNotBlank() }.size;
        if (file.extension?.lowercase() != "att")
            return false
        if (file.name.getOrNull(0)?.lowercase() == 'z' && !BreedPartKey.allowZ) {
            return false
        }
        val spriteFileNameBase = file.nameWithoutExtension
        file.getUserData(CACHED_SPRITE_KEY)?.let {
            return true
        }
        if (DumbService.isDumb(project)) {
            return false
        }
        val correspondingSprite = getAnyPossibleSprite(project, file, spriteFileNameBase)
        file.putUserData(CACHED_SPRITE_KEY, correspondingSprite)
        if (correspondingSprite != null) {
            return true
        }
        return false
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        if (project.isDisposed) {
            throw Exception("Cannot create editor. Project already disposed")
        }
        val spriteFile = file.nameWithoutExtension
        val correspondingSprite = file.getUserData(CACHED_SPRITE_KEY)
            ?: getAnyPossibleSprite(project, file, spriteFile)

        return AttEditorImpl(project, file, correspondingSprite!!)
    }


    override fun disposeEditor(editor: FileEditor) {
        Disposer.dispose(editor)
        if (editor is AttEditorImpl) {
            editor.dispose()
        }

    }

    override fun getEditorTypeId(): String {
        return EDITOR_TYPE_ID
    }

    override fun getPolicy(): FileEditorPolicy {
        return FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR
    }

    companion object {
        private const val EDITOR_TYPE_ID = "creature.spr"
    }
}

internal val CACHED_SPRITE_KEY = Key<VirtualFile>("creatures.att.CACHED_SPRITE")