package com.badahori.creatures.plugins.intellij.agenteering.indices

import com.badahori.creatures.plugins.intellij.agenteering.sprites.indices.BreedSpriteIndex
import com.badahori.creatures.plugins.intellij.agenteering.utils.FileNameUtils
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.*
import com.intellij.util.indexing.FileBasedIndex.InputFilter
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import org.apache.batik.dom.svg12.Global
import java.io.DataInput
import java.io.DataOutput
import java.util.*

class CaseInsensitiveFileIndex : ScalarIndexExtension<FileNameInfo>() {
    override fun getName(): ID<FileNameInfo, Void> = NAME

    override fun getIndexer(): DataIndexer<FileNameInfo, Void, FileContent> = Indexer

    override fun getKeyDescriptor(): KeyDescriptor<FileNameInfo> = Descriptor

    override fun getVersion(): Int = 3

    override fun getInputFilter(): InputFilter = INPUT_FILTER

    override fun dependsOnFileContent(): Boolean = false

    companion object {
        val NAME = ID.create<FileNameInfo,Void>("com.badahori.creatures.plugins.intellij.agenteering.indices.CaseInsensitiveFileIndex")
        private val INPUT_FILTER = InputFilter { true }

        fun findWithExtension(
            project:Project,
            extension:String,
            searchScope: GlobalSearchScope? = null
        ) : Collection<VirtualFile> {
            val key = FileNameInfo(null, null, extension)
            return findMatching(project, key, searchScope)
        }

        fun findWithoutExtension(
            project:Project,
            nameWithoutExtension:String,
            searchScope: GlobalSearchScope? = null
        ) : Collection<VirtualFile> {
            val key = FileNameInfo(null, nameWithoutExtension, null)
            return findMatching(project, key, searchScope)
        }


        fun findWithFileName(
            project:Project,
            fileName:String,
            searchScope: GlobalSearchScope? = null
        ) : Collection<VirtualFile> {
            val key = FileNameInfo(fileName, null, null)
            return findMatching(project, key, searchScope)
        }

        fun keys(project: Project): Set<FileNameInfo> {
            return FileBasedIndex.getInstance()
                .getAllKeys(NAME, project)
                .toSet()
        }

        private fun findMatching(project: Project, key: FileNameInfo, scope: GlobalSearchScope? = null): List<VirtualFile> {
            return FileBasedIndex.getInstance()
                .getAllKeys(NAME, project)
                .toSet()
                .filter {
                    Descriptor.isEqual(it, key)
                }
                .flatMap {
                    FileBasedIndex.getInstance().getContainingFiles(NAME, it, scope ?:  GlobalSearchScope.everythingScope(project))
                }
        }

    }
}

data class FileNameInfo(
    val fileName:String?,
    val nameWithoutExtension:String?,
    val extension:String?
) {
    override fun toString(): String {
        return fileName ?: nameWithoutExtension?.let { "$it.*"} ?: extension?.let { "*.$it" } ?: "<empty>"
    }
}

private object Indexer : DataIndexer<FileNameInfo, Void, FileContent> {
    override fun map(fileInfo: FileContent): Map<FileNameInfo, Void?> {
        val file = fileInfo.file
        val key = FileNameInfo(
            file.name.lowercase(),
            file.nameWithoutExtension.lowercase(),
            file.extension?.lowercase() ?: ""
        )
        return Collections.singletonMap(key, null)
    }
}

private object Descriptor : KeyDescriptor<FileNameInfo> {

    private const val NULL_KEY = "::://:::"

    override fun getHashCode(info: FileNameInfo?): Int {
        return info.hashCode()
    }


    override fun isEqual(info1: FileNameInfo?, info2: FileNameInfo?): Boolean {
        if (info1 == null && info2 == null)
            return true
        if (info1 == null || info2 == null)
            return false
        if (info1.fileName != null && info2.fileName != null && info1.fileName.lowercase() != info2.fileName.lowercase())
            return false
        if (info1.nameWithoutExtension != null && info2.nameWithoutExtension != null && info1.nameWithoutExtension.lowercase() != info2.nameWithoutExtension.lowercase())
            return false
        if (info1.extension != null && info2.extension != null && info1.extension.lowercase() != info2.extension.lowercase())
            return false
        return true
    }

    override fun save(output: DataOutput, info: FileNameInfo) {
        IOUtil.writeUTF(output, info.fileName ?: NULL_KEY)
        IOUtil.writeUTF(output, info.nameWithoutExtension ?: NULL_KEY)
        IOUtil.writeUTF(output, info.extension ?: NULL_KEY)
    }

    override fun read(input: DataInput): FileNameInfo {
        val fileName = IOUtil.readUTF(input)?.let {
            if (it == NULL_KEY) null else it
        }
        val fileBase = IOUtil.readUTF(input)?.let {
            if (it == NULL_KEY) null else it
        }
        val extension = IOUtil.readUTF(input)?.let {
            if (it == NULL_KEY) null else it
        }
        return FileNameInfo(
            fileName,
            fileBase,
            extension
        )
    }

}