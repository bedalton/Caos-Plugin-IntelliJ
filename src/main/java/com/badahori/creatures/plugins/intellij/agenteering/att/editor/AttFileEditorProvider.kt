package com.badahori.creatures.plugins.intellij.agenteering.att.editor

import com.badahori.creatures.plugins.intellij.agenteering.caos.formatting.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.getModule
import com.badahori.creatures.plugins.intellij.agenteering.utils.like
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopes


/**
 * Editor provider for sprite files. Wires up Sprite files to the viewer
 */
class AttFileEditorProvider : FileEditorProvider, DumbAware {


    override fun accept(project: Project, file: VirtualFile): Boolean {
        if (file.extension?.toLowerCase() != "att")
            return false
        val spriteFileNameBase = file.nameWithoutExtension
        file.getUserData(CACHED_SPRITE_KEY)?.let {
            return true
        }
        val correspondingSprite = getAnyPossibleSprite(project, file, spriteFileNameBase)
        file.putUserData(CACHED_SPRITE_KEY, correspondingSprite)
        if (correspondingSprite != null) {
            return true
        }
        return false
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val spriteFile = file.nameWithoutExtension
        val correspondingSprite = file.getUserData(CACHED_SPRITE_KEY) ?: getAnyPossibleSprite(project, file, spriteFile)
        return AttEditorImpl(project, file, correspondingSprite!!)
    }

    private fun getAnyPossibleSprite(project: Project, attFile: VirtualFile, spriteFileNameBase: String): VirtualFile? {
        var parent = attFile.parent
        var spriteFile: VirtualFile? = null
        while (spriteFile == null && parent != null) {
            spriteFile = searchParentRecursive(project, parent, spriteFileNameBase)
            parent = parent.parent
        }
        if (spriteFile != null)
            return spriteFile;
        val module = attFile.getModule(project)
            ?: return null
        val searchScope = GlobalSearchScope.moduleScope(module)
        return getVirtualFilesByName(project, spriteFileNameBase, "spr", searchScope).firstOrNull()
            ?: getVirtualFilesByName(project, spriteFileNameBase, "c16", searchScope).firstOrNull()
            ?: getVirtualFilesByName(project, spriteFileNameBase, "s16", searchScope).firstOrNull()
    }


    private fun searchParentRecursive(project: Project, parent: VirtualFile, spriteFile: String): VirtualFile? {
        getAnySpriteMatching(parent, spriteFile)?.let {
            return it
        }
        val searchScope = GlobalSearchScopes.directoriesScope(project, true, parent)
        return getVirtualFilesByName(project, spriteFile, "spr", searchScope).firstOrNull()
            ?: getVirtualFilesByName(project, spriteFile, "c16", searchScope).firstOrNull()
            ?: getVirtualFilesByName(project, spriteFile, "s16", searchScope).firstOrNull()
    }

    private fun getVirtualFilesByName(project:Project, spriteFile: String, extension:String, searchScope: GlobalSearchScope) : List<VirtualFile> {
        val rawFiles = (FilenameIndex.getAllFilesByExt(project, extension, searchScope) + FilenameIndex.getAllFilesByExt(project, extension.toUpperCase(), searchScope)).toSet()
        return rawFiles.filter {
            it.nameWithoutExtension like spriteFile
        }
    }

    private fun getAnySpriteMatching(parent: VirtualFile, spriteFile: String): VirtualFile? {
        return parent.findChild("$spriteFile.spr")
            ?: parent.findChild("$spriteFile.c16")
            ?: parent.findChild("$spriteFile.s16")
    }

    override fun disposeEditor(editor: FileEditor) {
        Disposer.dispose(editor)
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

private val CACHED_SPRITE_KEY = Key<VirtualFile>("com.badahori.creatures.plugins.intellij.agenteering.att.CACHED_SPRITE")