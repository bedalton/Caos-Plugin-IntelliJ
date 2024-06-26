@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.sprites.indices

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.indices.*
import com.badahori.creatures.plugins.intellij.agenteering.sprites.c16.C16FileType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.s16.S16FileType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.spr.SprFileType
import com.badahori.creatures.plugins.intellij.agenteering.utils.lowercase
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.*
import com.intellij.util.io.KeyDescriptor

class BreedSpriteIndex : ScalarIndexExtension<BreedPartKey>() {
    override fun getName(): ID<BreedPartKey, Void> = NAME

    override fun getIndexer(): DataIndexer<BreedPartKey, Void, FileContent> = BreedKeyIndexer

    override fun getKeyDescriptor(): KeyDescriptor<BreedPartKey> = BreedPartDescriptor

    override fun getVersion(): Int =
        VERSION + BreedPartKey.VERSION + BreedKeyIndexer.VERSION + BreedPartDescriptor.VERSION + BreedFileInputFilter.VERSION

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return SpriteInputFilter
    }

    override fun dependsOnFileContent(): Boolean = false

    companion object {
        val NAME: ID<BreedPartKey, Void> by lazy {
            ID.create("com.badahori.creatures.plugins.intellij.agenteering.sprites.indices.BreedSpriteIndex")
        }

        private const val THIS_VERSION = 4

        const val VERSION = BREED_INDEX_VERSION_BASE + THIS_VERSION + BreedPartKey.VERSION + BreedKeyIndexer.VERSION + BreedPartDescriptor.VERSION + BreedFileInputFilter.VERSION
        private var didIndexOnce: Boolean = false

        fun acceptInput(virtualFile: VirtualFile): Boolean {
            return SpriteInputFilter.acceptInput(virtualFile)
        }

        private val SpriteInputFilter by lazy {
            BreedFileInputFilter(
                listOf(
                    SprFileType,
                    S16FileType,
                    C16FileType
                )
            )
        }

        @JvmStatic
        fun findMatching(
            project: Project,
            variant: CaosVariant,
            searchScope: GlobalSearchScope? = null,
            progressIndicator: ProgressIndicator?
        ): Collection<VirtualFile> {

            if (project.isDisposed) {
                return emptyList()
            }
            val scope = GlobalSearchScope.everythingScope(project).let {
                if (searchScope != null) it.intersectWith(searchScope) else it
            }
            val variantKeys = keys(project)
                .filter { it.variant == null || it.variant == variant || (it.variant.isC3DS && variant.isC3DS) }
            return variantKeys.flatMap { aKey ->
                if (project.isDisposed) {
                    return emptyList()
                }
                progressIndicator?.checkCanceled()
                FileBasedIndex.getInstance().getContainingFiles(NAME, aKey, scope)
            }
        }

        @JvmStatic
        fun findMatching(
            project: Project,
            key: BreedPartKey,
            searchScope: GlobalSearchScope? = null,
            progressIndicator: ProgressIndicator? = null
        ): Collection<VirtualFile> {
            progressIndicator?.checkCanceled()
            if (project.isDisposed) {
                return emptyList()
            }
            val scope = GlobalSearchScope.everythingScope(project).let {
                if (searchScope != null) it.intersectWith(searchScope) else it
            }
            val fudgedKey = if (key.variant == CaosVariant.DS)
                key.copy(variant = CaosVariant.C3)
            else
                key
//            val old = FileBasedIndex.getInstance().getAllKeys(NAME, project)
//                .filter { other ->
//                    BreedPartKey.isGenericMatch(fudgedKey, other)
//                }
//                .flatMap { aKey ->
//                    FileBasedIndex.getInstance().getContainingFiles(NAME, aKey, scope)
//                }
            //            if (new.size < old.size) {
//                LOGGER.severe("Failed to find the same number of new files as old. Expected: ${old.size}; Actual: ${new.size}")
//                return old
//            }
            if (project.isDisposed) {
                return emptyList()
            }
            return FileBasedIndex.getInstance().getContainingFiles(NAME, fudgedKey, scope).let { files ->
                progressIndicator?.checkCanceled()
                fudgedKey.part
                    ?.let { part ->
                        files
                            .filter {
                                it.name[0].lowercase() == part
                            }
                            .filterNotNull()
                    } ?: files
            }
        }

        @JvmStatic
        fun allFiles(project: Project, searchScope: GlobalSearchScope? = null): List<VirtualFile> {
            if (project.isDisposed) {
                return emptyList()
            }
            val scope = GlobalSearchScope.everythingScope(project).let {
                if (searchScope != null) it.intersectWith(searchScope) else it
            }
            return FileBasedIndex.getInstance().getAllKeys(NAME, project)
                .flatMap { aKey ->
                    if (project.isDisposed) {
                        return emptyList()
                    }
                    FileBasedIndex.getInstance().getContainingFiles(NAME, aKey, scope)
                }.toList()
        }

        @JvmStatic
        fun keys(project: Project): Collection<BreedPartKey> {
            if (project.isDisposed) {
                return emptyList()
            }
            return FileBasedIndex.getInstance().getAllKeys(NAME, project)
        }

        @Suppress("UNUSED_PARAMETER")
        @JvmStatic
        fun findMatching(
            project: Project,
            fileName: String,
            searchScope: GlobalSearchScope? = null,
            progressIndicator: ProgressIndicator? = null
        ): Collection<VirtualFile> {

            if (project.isDisposed) {
                return emptyList()
            }

            val scope = GlobalSearchScope.projectScope(project).let {
                if (searchScope != null) it.intersectWith(searchScope) else it
            }

            val key = BreedPartKey.fromFileName(fileName)
                ?: return emptyList()
            return FileBasedIndex.getInstance().getContainingFiles(NAME, key, scope)
        }
    }
}


