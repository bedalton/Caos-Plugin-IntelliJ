package com.badahori.creatures.plugins.intellij.agenteering.att.indices

import com.badahori.creatures.plugins.intellij.agenteering.att.lang.AttFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.indices.VariantKeyDescriptor
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedKeyIndexer
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartDescriptor
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.badahori.creatures.plugins.intellij.agenteering.indices.VariantIndexer
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.*
import com.intellij.util.io.KeyDescriptor

class AttFileByVariantIndex : ScalarIndexExtension<CaosVariant>() {
    override fun getName(): ID<CaosVariant, Void> = NAME

    override fun getIndexer(): DataIndexer<CaosVariant, Void, FileContent> = VariantIndexer

    override fun getKeyDescriptor(): KeyDescriptor<CaosVariant> = VariantKeyDescriptor

    override fun getVersion(): Int = VERSION + VariantKeyDescriptor.VERSION + VariantIndexer.VERSION

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return DefaultFileTypeSpecificInputFilter(AttFileType)
    }

    override fun dependsOnFileContent(): Boolean = true

    companion object {
        const val VERSION = 1
        val NAME: ID<CaosVariant, Void> =
            ID.create("com.badahori.creatures.plugins.intellij.agenteering.att.AttByVariantIndex")

//        private var didIndexOnce = false

        @JvmStatic
        fun findMatching(
            project: Project,
            key: CaosVariant,
            searchScope: GlobalSearchScope? = null
        ): Collection<VirtualFile> {
            val scope = GlobalSearchScope.projectScope(project).let {
                if (searchScope != null) it.intersectWith(searchScope) else it
            }
            val fudgedKey = if (key == CaosVariant.DS)
                CaosVariant.C3
            else
                key
            //indexOnce(project, searchScope)
            return FileBasedIndex
                .getInstance()
                .getContainingFiles(NAME, fudgedKey, scope)
        }

        fun indexOnce(project: Project) {
            val runnable = Runnable@{
//                if (didIndexOnce)
//                    return@Runnable

                FileBasedIndex.getInstance().requestRebuild(NAME)
//                didIndexOnce = true
            }
            if (DumbService.isDumb(project)) {
                DumbService.getInstance(project).runWhenSmart(runnable)
            } else {
                runnable()
            }
        }
    }
}


class AttFilesIndex : ScalarIndexExtension<BreedPartKey>() {
    override fun getName(): ID<BreedPartKey, Void> = NAME

    override fun getIndexer(): DataIndexer<BreedPartKey, Void, FileContent> = BreedKeyIndexer

    override fun getKeyDescriptor(): KeyDescriptor<BreedPartKey> = BreedPartDescriptor

    override fun getVersion(): Int = VERSION + BreedPartKey.VERSION + BreedKeyIndexer.VERSION + BreedPartDescriptor.VERSION

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return DefaultFileTypeSpecificInputFilter(AttFileType)
    }

    override fun dependsOnFileContent(): Boolean = true

    companion object {
        const val VERSION = 6
        val NAME: ID<BreedPartKey, Void> =
            ID.create("com.badahori.creatures.plugins.intellij.agenteering.att.indices.AttFilesIndex")

        @JvmStatic
        fun findMatching(
            project: Project,
            key: BreedPartKey,
            searchScope: GlobalSearchScope? = null
        ): Collection<VirtualFile> {
            val scope = GlobalSearchScope.projectScope(project).let {
                if (searchScope != null) it.intersectWith(searchScope) else it
            }
            val fudgedKey = if (key.variant == CaosVariant.DS)
                key.copy(variant = CaosVariant.C3)
            else
                key
            //indexOnce(project, searchScope)
            return FileBasedIndex.getInstance().getAllKeys(NAME, project)
                .filter { other -> BreedPartKey.isGenericMatch(fudgedKey, other) }
                .flatMap { aKey -> FileBasedIndex.getInstance().getContainingFiles(NAME, aKey, scope) }
        }

        fun indexOnce(project: Project) {
            val runnable = runnable@{
//                if (didIndexOnce)
//                    return@runnable
                FileBasedIndex.getInstance().requestRebuild(NAME)
//                didIndexOnce = true
            }
            if (DumbService.isDumb(project))
                DumbService.getInstance(project).runWhenSmart(runnable)
            else
                runnable()

        }
    }
}


