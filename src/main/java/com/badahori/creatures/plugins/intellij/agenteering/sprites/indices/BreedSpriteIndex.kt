package com.badahori.creatures.plugins.intellij.agenteering.sprites.indices

import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedFileInputFilter
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedKeyIndexer
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartDescriptor
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.badahori.creatures.plugins.intellij.agenteering.sprites.c16.C16FileType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.s16.S16FileType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.spr.SprFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.*
import com.intellij.util.io.KeyDescriptor

class BreedSpriteIndex : ScalarIndexExtension<BreedPartKey>() {
    override fun getName(): ID<BreedPartKey, Void> = NAME

    override fun getIndexer(): DataIndexer<BreedPartKey, Void, FileContent> = BreedKeyIndexer

    override fun getKeyDescriptor(): KeyDescriptor<BreedPartKey> = BreedPartDescriptor

    override fun getVersion(): Int = 1

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return SpriteInputFilter
    }

    override fun dependsOnFileContent(): Boolean = false

    companion object {
        val NAME: ID<BreedPartKey, Void> =
            ID.create("com.badahori.creatures.plugins.intellij.agenteering.sprites.indices.BreedSpriteIndex")

        private val SpriteInputFilter = BreedFileInputFilter(
            listOf(
                SprFileType,
                S16FileType,
                C16FileType
            )
        )

        @JvmStatic
        fun findMatching(
            project: Project,
            key: BreedPartKey,
            searchScope: GlobalSearchScope? = null
        ): Collection<VirtualFile> {
            val scope = GlobalSearchScope.projectScope(project).let {
                if (searchScope != null) it.intersectWith(searchScope) else it
            }
            return FileBasedIndex.getInstance().getContainingFiles(NAME, key, scope)
        }

        @JvmStatic
        fun findMatching(
            project: Project,
            fileName: String,
            searchScope: GlobalSearchScope? = null
        ): Collection<VirtualFile> {
            val scope = GlobalSearchScope.projectScope(project).let {
                if (searchScope != null) it.intersectWith(searchScope) else it
            }
            val key = BreedPartKey.fromFileName(fileName)
                ?: return emptyList()
            return FileBasedIndex.getInstance().getContainingFiles(NAME, key, scope)
        }
    }
}