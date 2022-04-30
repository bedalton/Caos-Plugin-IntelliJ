@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.intellij.icons.AllIcons
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.openapi.vfs.newvfs.FileAttribute
import com.intellij.psi.FileViewProvider
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.impl.file.PsiBinaryFileImpl
import com.intellij.psi.impl.file.impl.FileManagerImpl
import com.intellij.psi.search.GlobalSearchScope
import icons.CaosScriptIcons
import java.io.DataInputStream
import java.io.DataOutputStream
import javax.swing.Icon


object VirtualFileUtil {

    fun childrenWithExtensions(
        virtualFile: VirtualFile,
        recursive: Boolean,
        vararg extensionsIn: String,
    ): List<VirtualFile> {
        val extensions = extensionsIn.toList()
        if (!recursive) {
            return virtualFile.children.filter {
                it.extension likeAny extensions
            }
        }
        return virtualFile.children.flatMap {
            if (it.isDirectory) {
                childrenWithExtensions(it, true, *extensionsIn)
            } else if (it.extension likeAny extensions) {
                listOf(it)
            } else
                emptyList()
        }
    }

    /**
     * Collects non-directory child files within a virtual file directory
     */
    fun collectChildFiles(virtualFile: VirtualFile, recursive: Boolean): List<VirtualFile> {
        if (!recursive) {
            return virtualFile.children.filterNot { it.isDirectory }
        }
        return virtualFile.children.flatMap {
            if (it.isDirectory) {
                collectChildFiles(it, true)
            } else {
                listOf(it)
            }
        }
    }

    fun findChildIgnoreCase(
        parent: VirtualFile?,
        ignoreExtension: Boolean = false,
        directory: Boolean?,
        vararg path: String,
        scope: GlobalSearchScope? = null,
    ): VirtualFile? {

        val components = (if (path.any { it.contains("\\") })
            path.flatMap { it.split("\\") }
        else
            path.flatMap { it.split("/") }).filter { it.isNotNullOrBlank() }

        if (components.isEmpty())
            return null

        var file = parent
            ?: return null

        for (component in components.dropLast(1)) {
            if (component == ".")
                continue
            if (component == "..") {
                file = file.parent
                    ?: return null
                continue
            }
            file = file.children?.firstOrNull {
                (scope == null || scope.accept(it)) && it.name.equals(component,
                    true) && it.isDirectory
            }
                ?: return null
        }

        return if (ignoreExtension) {
            val last = FileNameUtils.getNameWithoutExtension(components.last())
            file.children?.firstOrNull {
                (scope == null || scope.accept(it)) &&
                        it.nameWithoutExtension.equals(last, true) &&
                        (directory == null || it.isDirectory == directory)
            }
        } else {
            val last = components.last()
            file.children?.firstOrNull {
                (scope == null || scope.accept(it)) &&
                        it.name.equals(last, true) &&
                        (directory == null || it.isDirectory == directory)
            }
        }
    }

    fun findChildrenIfDirectoryOrSiblingsIfLeaf(virtualFile: VirtualFile?, vararg path: String): List<VirtualFile>? {
        if (virtualFile == null)
            return null
        val components = if (path.any { it.contains("\\") })
            path.flatMap { it.split("\\") }
        else
            path.flatMap { it.split("/") }
        if (components.isEmpty()) {
            return null
        }
        if (components.size == 1) {
            val onlyComponent = components[0]
            if (FileNameUtils.getExtension(onlyComponent)?.nullIfEmpty() == null) {
                virtualFile.children.firstOrNull { it.name.equals(onlyComponent, ignoreCase = true) }
                    ?.let {
                        if (it.isDirectory)
                            return it.children.toList()
                    }
            }
            return if (virtualFile.isDirectory)
                virtualFile.children.toList()
            else
                virtualFile.parent?.children?.toList()

        }

        var file: VirtualFile = virtualFile
        for (component in components.dropLast(1)) {

            if (component == ".")
                continue

            if (component == "..")
                file = file.parent
                    ?: return null

            file = file.children?.firstOrNull { it.name.equals(component, true) }
                ?: return null
        }
        val lastComponent = components.last()
        var last = file.children?.firstOrNull { it.name.equals(lastComponent, true) }
        if (last == null) {
            if (FileNameUtils.getExtension(lastComponent).nullIfEmpty() == null)
                return null
            else
                last = file
        }
        return if (last.isDirectory) {
            last.children
        } else {
            file.children
        }.filter { !it.isDirectory }
    }

}

private class VirtualFileNavigationElement(
    private val myProject: Project,
    private val myVirtualFile: VirtualFile,
    viewProvider: FileViewProvider,
) : PsiBinaryFileImpl(PsiManagerImpl.getInstance(myProject) as PsiManagerImpl, viewProvider), NavigatablePsiElement {

    override fun getPresentation(): ItemPresentation {
        return object : ItemPresentation {
            override fun getPresentableText(): String {
                return virtualFile.name
            }

            override fun getLocationString(): String {
                return virtualFile.parent?.path ?: ""
            }

            override fun getIcon(unused: Boolean): Icon? {
                return fileType.icon
            }
        }
    }

    override fun getProject(): Project {
        return myProject
    }

    override fun canNavigate(): Boolean {
        return myVirtualFile.isValid
    }

    override fun navigate(requestFocus: Boolean) {
        val manager = FileEditorManager.getInstance(project)
        manager.openFile(myVirtualFile, true, true)
    }


    companion object {
        fun create(project: Project, virtualFile: VirtualFile): NavigatablePsiElement? {
            val viewProvider = getFileViewProvider(project, virtualFile)
                ?: return null
            return VirtualFileNavigationElement(
                project,
                virtualFile,
                viewProvider
            )
        }
    }
}


private fun getFileViewProvider(project: Project, virtualFile: VirtualFile): FileViewProvider? {
    val fileManager = PsiManagerEx.getInstanceEx(project).fileManager as FileManagerImpl
    return try {
        fileManager.findViewProvider(virtualFile)
    } catch (e: Exception) {
        LOGGER.severe("Failed to get view provider for artificial navigation item: ${virtualFile.path}")
        null
    }
}

internal fun VirtualFile.toNavigableElement(project: Project): NavigatablePsiElement? {
    return PsiManager.getInstance(project).findFile(this) ?: VirtualFileNavigationElement.create(project, this)
}

data class FileAttributeWithKey<T>(
    val key: Key<T>,
    val attribute: FileAttribute = FileAttribute(key.toString()),
    val version: Int = 1,
) {
    constructor(key: String, version: Int = 1) : this(
        Key.create(key),
        FileAttribute(key),
        version
    )
}


val NULL_PLACEHOLDER_FILE by lazy { CaosVirtualFile("NULL.null", "__NULL FILE PLACEHOLDER__") }

internal inline fun <T> readFromStorage(
    file: VirtualFile,
    key: FileAttributeWithKey<T>,
    convert: (String?) -> T?,
) {
    readFromStorage(
        file,
        key.attribute,
        key.key,
        convert
    )
}

internal inline fun <T> readFromStorage(
    file: VirtualFile,
    attribute: FileAttribute,
    key: Key<T>,
    convert: (String?) -> T?,
): T? {

    file.getUserData(key)?.let {
        return it
    }

    if (file !is VirtualFileWithId) {
        return null
    }

    val attributeStream = attribute.readAttribute(file)
        ?: return null
    val out = attributeStream.use { stream ->
        stream.readString()
    }
    return convert(out.toString())
}

internal inline fun <T> readFromStorageStream(
    file: VirtualFile,
    attribute: FileAttribute,
    key: Key<T>,
    safe: Boolean = false,
    convert: DataInputStream.() -> T?,
): T? {
    return if (safe) {
        try {
            readFromStorageActual(file, attribute, key, convert)
        } catch (e: Exception) {
            null
        }
    } else {
        readFromStorageActual(file, attribute, key, convert)
    }
}

private inline fun <T> readFromStorageActual(
    file: VirtualFile,
    attribute: FileAttribute,
    key: Key<T>,
    convert: DataInputStream.() -> T?,
): T? {
    file.getUserData(key)?.let {
        return it
    }

    if (file !is VirtualFileWithId) {
        return null
    }

    val stream = attribute.readAttribute(file)
        ?: return null
    return try {
        convert(stream)
    } catch (e: Exception) {
        throw e
    } finally {
        stream.close()
    }
}


internal inline fun <T> writeToStorage(
    file: VirtualFile,
    key: FileAttributeWithKey<T>,
    value: T?,
    convert: (T) -> String?,
) {
    writeToStorage(
        file,
        key.attribute,
        key.key,
        value,
        convert
    )
}

internal inline fun <T> writeToStorage(
    file: VirtualFile,
    attribute: FileAttribute,
    key: Key<T>,
    value: T?,
    convert: (T) -> String?,
): Boolean {
    file.putUserData(key, value)
    if (file !is VirtualFileWithId) {
        return false
    }
    val attributeStream = attribute.writeAttribute(file)
    return attributeStream.use { stream ->
        val string = if (value != null) convert(value) else null
        if (string == null) {
            stream.writeInt(-1)
        } else {
            stream.writeInt(string.length)
            stream.writeChars(string)
        }
        true
    }
}


internal inline fun <T> writeToStorageStream(
    file: VirtualFile,
    attribute: FileAttribute,
    key: Key<T>,
    value: T?,
    convert: DataOutputStream.(T?) -> Unit?,
): Boolean {
    file.putUserData(key, value)
    if (file !is VirtualFileWithId) {
        return false
    }
    val attributeStream = attribute.writeAttribute(file)
    return attributeStream.use { stream ->
        stream.convert(value)
        stream.close()
        true
    }
}


internal fun DataOutputStream.writeString(string: String?) {
    if (string == null) {
        this.writeInt(-1)
    } else {
        this.writeInt(string.length)
        this.writeChars(string)
    }
}


internal fun DataInputStream.readString(): String? {
    val length = this.readInt()
    if (length < 0) {
        return null
    }
    val out = StringBuilder()
    (0 until length).forEach { _ ->
        out.append(this.readChar())
    }
    return out.toString()
}

internal inline fun <T> VirtualFile.asWritable(work: (file: VirtualFile) -> T): T {
    val writable = this.isWritable
    this.isWritable = true
    val out = work(this)
    isWritable = writable
    return out
}

internal inline fun VirtualFile.applyWritable(work: VirtualFile.() -> Unit) {
    val writable = this.isWritable
    this.isWritable = true
    work(this)
    isWritable = writable
}


/**
 * Get the swing icon for a given file name
 */
internal fun getFileIcon(fileName: String, nonNullDefault: Boolean = true): Icon? {
    val extension = (FileNameUtils.getExtension(fileName) ?: fileName)
    return when (extension.uppercase()) {
        "COS" -> CaosScriptIcons.CAOS_FILE_ICON
        "SPR" -> CaosScriptIcons.SPR_FILE_ICON
        "S16" -> CaosScriptIcons.S16_FILE_ICON
        "C16" -> CaosScriptIcons.C16_FILE_ICON
        "ATT" -> CaosScriptIcons.ATT_FILE_ICON
        "AGENT", "AGENTS" -> CaosScriptIcons.AGENT_FILE_ICON
        "COB" -> CaosScriptIcons.COB_FILE_ICON
        "PRAY" -> CaosScriptIcons.PRAY_FILE_ICON
        "BLK" -> CaosScriptIcons.BLK_FILE_ICON
        "RCB" -> CaosScriptIcons.RCB_FILE_ICON
        "WAV" -> CaosScriptIcons.WAV_FILE_ICON
        "MNG" -> CaosScriptIcons.MNG_FILE_ICON
        "SFC" -> CaosScriptIcons.SFC_FILE_ICON
        "CAOSDEF" -> CaosScriptIcons.CAOS_DEF_FILE_ICON
        else -> {
            if (nonNullDefault) {
                AllIcons.FileTypes.Any_type
            } else {
                null
            }
        }
    }
}