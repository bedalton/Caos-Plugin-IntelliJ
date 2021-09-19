package com.badahori.creatures.plugins.intellij.agenteering.indices

import com.badahori.creatures.plugins.intellij.agenteering.utils.FileNameUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.*
import com.intellij.util.indexing.FileBasedIndex.InputFilter
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import java.io.DataInput
import java.io.DataOutput

class CaseInsensitiveFileIndex : ScalarIndexExtension<FileNameInfo>() {
    override fun getName(): ID<FileNameInfo, Void> = NAME

    override fun getIndexer(): DataIndexer<FileNameInfo, Void, FileContent> = Indexer

    override fun getKeyDescriptor(): KeyDescriptor<FileNameInfo> = Descriptor

    override fun getVersion(): Int = 0

    override fun getInputFilter(): InputFilter = INPUT_FILTER

    override fun dependsOnFileContent(): Boolean = false

    companion object {
        val NAME = ID.create<FileNameInfo,Void>("com.badahori.creatures.plugins.intellij.agenteering.att.indices.CaseInsensitiveFileIndex")
        private val INPUT_FILTER = InputFilter { true }

        fun findWithExtension(
            project:Project,
            extension:String,
            searchScope: GlobalSearchScope? = null
        ) : Collection<VirtualFile> {
            val scope = GlobalSearchScope.projectScope(project).let {
                if (searchScope != null) it.intersectWith(searchScope) else it
            }
            return FileBasedIndex.getInstance().getContainingFiles(NAME, FileNameInfo(null, null, extension), scope)
        }

        fun findWithoutExtension(
            project:Project,
            nameWithoutExtension:String,
            searchScope: GlobalSearchScope? = null
        ) : Collection<VirtualFile> {
            val scope = GlobalSearchScope.projectScope(project).let {
                if (searchScope != null) it.intersectWith(searchScope) else it
            }
            return FileBasedIndex.getInstance().getContainingFiles(NAME, FileNameInfo(null, nameWithoutExtension, null), scope)
        }


        fun findWithFileName(
            project:Project,
            fileName:String,
            searchScope: GlobalSearchScope? = null
        ) : Collection<VirtualFile> {
            val scope = GlobalSearchScope.projectScope(project).let {
                if (searchScope != null) it.intersectWith(searchScope) else it
            }
            return FileBasedIndex.getInstance().getContainingFiles(NAME, FileNameInfo(fileName, null, null), scope)
        }
    }
}

data class FileNameInfo(val fileName:String?, val nameWithoutExtension:String?, val extension:String?)

private object Indexer : DataIndexer<FileNameInfo, Void, FileContent> {
    override fun map(fileInfo: FileContent): Map<FileNameInfo, Void?> {
        val file = fileInfo.file
        return mapOf(
            FileNameInfo(file.name.toLowerCase(), file.nameWithoutExtension.toLowerCase(), file.extension?.toLowerCase() ?: "") to null
        )
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
        if (info1.fileName != null && info2.fileName != null && info1.fileName.toLowerCase() != info2.fileName.toLowerCase())
            return false
        if (info1.nameWithoutExtension != null && info2.nameWithoutExtension != null && info1.nameWithoutExtension.toLowerCase() != info2.nameWithoutExtension.toLowerCase())
            return false
        if (info1.extension != null && info2.extension != null && info1.extension.toLowerCase() != info2.extension.toLowerCase())
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