package com.badahori.creatures.plugins.intellij.agenteering.sprites.indices

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedFileInputFilter
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedKeyIndexer
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartDescriptor
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.badahori.creatures.plugins.intellij.agenteering.sprites.c16.C16FileType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.s16.S16FileType
import com.badahori.creatures.plugins.intellij.agenteering.sprites.spr.SprFileType
import com.intellij.openapi.project.DumbService
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
        5 + BreedPartKey.VERSION + BreedKeyIndexer.VERSION + BreedPartDescriptor.VERSION + BreedFileInputFilter.VERSION

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return SpriteInputFilter
    }

    override fun dependsOnFileContent(): Boolean = false

    companion object {
        val NAME: ID<BreedPartKey, Void> =
            ID.create("com.badahori.creatures.plugins.intellij.agenteering.sprites.indices.BreedSpriteIndex")

        private var didIndexOnce: Boolean = false

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
            variant: CaosVariant,
            searchScope: GlobalSearchScope? = null
        ): Collection<VirtualFile> {
            val scope = GlobalSearchScope.everythingScope(project).let {
                if (searchScope != null) it.intersectWith(searchScope) else it
            }
            val variantKeys = keys(project)
                .filter { it.variant == null || it.variant == variant || (it.variant.isC3DS && variant.isC3DS) }
            return variantKeys.flatMap { aKey -> FileBasedIndex.getInstance().getContainingFiles(NAME, aKey, scope) }
        }

        @JvmStatic
        fun findMatching(
            project: Project,
            key: BreedPartKey,
            searchScope: GlobalSearchScope? = null
        ): Collection<VirtualFile> {
            val scope = GlobalSearchScope.everythingScope(project).let {
                if (searchScope != null) it.intersectWith(searchScope) else it
            }
            val fudgedKey = if (key.variant == CaosVariant.DS)
                key.copy(variant = CaosVariant.C3)
            else
                key
            //indexOnce(project, scope)
            return FileBasedIndex.getInstance().getAllKeys(NAME, project)
                .filter { other ->
                    BreedPartKey.isGenericMatch(fudgedKey, other)
                }
                .flatMap { aKey ->
                    FileBasedIndex.getInstance().getContainingFiles(NAME, aKey, scope)
                }
        }

        @JvmStatic
        fun allFiles(project: Project, searchScope: GlobalSearchScope? = null): List<VirtualFile> {
            val scope = GlobalSearchScope.everythingScope(project).let {
                if (searchScope != null) it.intersectWith(searchScope) else it
            }
            return FileBasedIndex.getInstance().getAllKeys(NAME, project)
                .flatMap { aKey -> FileBasedIndex.getInstance().getContainingFiles(NAME, aKey, scope) }.toList()
        }

        @JvmStatic
        fun keys(project: Project): Collection<BreedPartKey> {
            return FileBasedIndex.getInstance().getAllKeys(NAME, project)
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
            //indexOnce(project, scope)
            val key = BreedPartKey.fromFileName(fileName)
                ?: return emptyList()
            return FileBasedIndex.getInstance().getAllKeys(NAME, project)
                .filter { other -> BreedPartKey.isGenericMatch(key, other) }
                .flatMap { aKey -> FileBasedIndex.getInstance().getContainingFiles(NAME, aKey, scope) }
        }

        fun indexOnce(project: Project, searchScope: GlobalSearchScope? = null) {
            val runnable = run@{
//                if (didIndexOnce)
//                    return@run
                FileBasedIndex.getInstance().requestRebuild(NAME)
                if (DumbService.isDumb(project)) {
                    DumbService.getInstance(project).runWhenSmart {
                        FileBasedIndex.getInstance().ensureUpToDate(NAME, project, searchScope)
                    }
                } else
                    FileBasedIndex.getInstance().ensureUpToDate(NAME, project, searchScope)
                didIndexOnce = true
            }
            if (DumbService.isDumb(project))
                DumbService.getInstance(project).runWhenSmart(runnable)
            else
                runnable()
        }
    }
}