package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.impl.file.PsiBinaryFileImpl
import com.intellij.psi.impl.file.impl.FileManagerImpl
import javax.swing.Icon


object VirtualFileUtil {

    fun childrenWithExtensions(
        virtualFile: VirtualFile,
        recursive: Boolean,
        vararg extensionsIn: String
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
        vararg path: String
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
            file = file.children?.firstOrNull { it.name.equals(component, true) && it.isDirectory }
                ?: return null
        }

        return if (ignoreExtension) {
            val last = FileNameUtils.getNameWithoutExtension(components.last())
            file.children?.firstOrNull { it.nameWithoutExtension.equals(last, true) && (directory == null || it.isDirectory == directory)}
        } else {
            val last = components.last()
            file.children?.firstOrNull { it.name.equals(last, true) && (directory == null || it.isDirectory == directory) }
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

        var file:VirtualFile = virtualFile
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
    viewProvider: FileViewProvider
): PsiBinaryFileImpl(PsiManagerImpl.getInstance(myProject) as PsiManagerImpl, viewProvider), NavigatablePsiElement {

    override fun getPresentation(): ItemPresentation {
        return object: ItemPresentation {
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