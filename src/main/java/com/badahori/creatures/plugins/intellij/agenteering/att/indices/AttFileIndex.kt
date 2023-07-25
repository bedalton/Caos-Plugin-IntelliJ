@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.att.indices

import com.badahori.creatures.plugins.intellij.agenteering.att.lang.AttFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.VariantKeyDescriptor
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedKeyIndexer
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartDescriptor
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.badahori.creatures.plugins.intellij.agenteering.indices.VariantIndexer
import com.badahori.creatures.plugins.intellij.agenteering.utils.lowercase
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.*
import com.intellij.util.io.KeyDescriptor

class AttFilesByVariantIndex : ScalarIndexExtension<CaosVariant>() {
    override fun getName(): ID<CaosVariant, Void> = NAME

    override fun getIndexer(): DataIndexer<CaosVariant, Void, FileContent> = VariantIndexer

    override fun getKeyDescriptor(): KeyDescriptor<CaosVariant> = VariantKeyDescriptor

    override fun getVersion(): Int = VERSION + VariantKeyDescriptor.VERSION + VariantIndexer.VERSION

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return DefaultFileTypeSpecificInputFilter(AttFileType)
    }

    override fun dependsOnFileContent(): Boolean = true

    companion object {
        // Index name identifier
        val NAME: ID<CaosVariant, Void> =
            ID.create("com.badahori.creatures.plugins.intellij.agenteering.att.AttByVariantIndex")

        // Update this index's version number, NOT const "VERSION"
        private const val THIS_VERSION = 2

        // DO NOT ALTER "VERSION" DIRECTLY
        const val VERSION = THIS_VERSION + BreedPartKey.VERSION + BreedKeyIndexer.VERSION + BreedPartDescriptor.VERSION

//        private var didIndexOnce = false

        @JvmStatic
        fun findMatching(
            project: Project,
            key: CaosVariant,
            searchScope: GlobalSearchScope? = null,
        ): Collection<VirtualFile> {

            if (project.isDisposed) {
                return emptyList()
            }
            val scope = GlobalSearchScope.projectScope(project).let {
                if (searchScope != null) it.intersectWith(searchScope) else it
            }
            val fudgedKey = if (key == CaosVariant.DS)
                CaosVariant.C3
            else
                key
            return FileBasedIndex
                .getInstance()
                .getContainingFiles(NAME, fudgedKey, scope)
        }
    }
}


class AttFilesIndex : ScalarIndexExtension<BreedPartKey>() {
    override fun getName(): ID<BreedPartKey, Void> = NAME

    override fun getIndexer(): DataIndexer<BreedPartKey, Void, FileContent> = BreedKeyIndexer

    override fun getKeyDescriptor(): KeyDescriptor<BreedPartKey> = BreedPartDescriptor

    override fun getVersion(): Int = VERSION

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return DefaultFileTypeSpecificInputFilter(AttFileType)
    }

    override fun dependsOnFileContent(): Boolean = true

    companion object {
        private const val THIS_VERSION = 8
        const val VERSION = THIS_VERSION + BreedPartKey.VERSION + BreedKeyIndexer.VERSION + BreedPartDescriptor.VERSION
        val NAME: ID<BreedPartKey, Void> =
            ID.create("com.badahori.creatures.plugins.intellij.agenteering.att.indices.AttFilesIndex")

        @JvmStatic
        fun findMatching(
            project: Project,
            key: BreedPartKey,
            searchScope: GlobalSearchScope? = null,
            progressIndicator: ProgressIndicator? = null
        ): Collection<VirtualFile> {
            if (project.isDisposed) {
                return emptyList()
            }
            val scope = GlobalSearchScope.projectScope(project).let {
                if (searchScope != null) it.intersectWith(searchScope) else it
            }
            val fudgedKey = if (key.variant == CaosVariant.DS)
                key.copy(variant = CaosVariant.C3)
            else
                key

            return FileBasedIndex.getInstance().getAllKeys(NAME, project)
                .filter { other ->
                    progressIndicator?.checkCanceled()
                    BreedPartKey.isGenericMatch(fudgedKey, other)
                }
                .flatMap { aKey ->
                    if (project.isDisposed) {
                        return emptyList()
                    }
                    progressIndicator?.checkCanceled()
                    FileBasedIndex.getInstance().getContainingFiles(NAME, aKey, scope)
                }
                .let { files ->
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
//            return FileBasedIndex.getInstance().getContainingFiles(NAME, fudgedKey, scope)

        /**
         * Get all keys stored in this index
         */
        fun getAllKeys(project: Project): Collection<BreedPartKey> {
            if (project.isDisposed) {
                return emptyList()
            }
            return FileBasedIndex.getInstance().getAllKeys(NAME, project)
        }

        /**
         * Get all keys stored in this index with scope
         * Possibly very, very slow
         */
        fun getAllKeys(project: Project, scope: GlobalSearchScope): Collection<BreedPartKey> {

            if (project.isDisposed) {
                return emptyList()
            }
            return FileBasedIndex.getInstance().getAllKeys(NAME, project)
                .filter { aKey ->
                    if (project.isDisposed) {
                        return emptyList()
                    }
                    FileBasedIndex.getInstance().getContainingFiles(NAME, aKey, scope)
                        .any { file -> scope.accept(file) }
                }
        }
    }
}


