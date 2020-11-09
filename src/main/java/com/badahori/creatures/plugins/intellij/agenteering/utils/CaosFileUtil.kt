@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.*
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.io.File


val VirtualFile.contents: String
    get() {
        return VfsUtilCore.loadText(this)
    }

fun VirtualFile.getModule(project: Project): Module? {
    return ModuleUtil.findModuleForFile(this, project)
}

val Document.virtualFile
    get() = FileDocumentManager.getInstance().getFile(this)

val Editor.virtualFile
    get() = document.virtualFile

fun VirtualFile.getPsiFile(project: Project): PsiFile? = PsiManager.getInstance(project).findFile(this)


private const val PLUGIN_ID = "com.badahori.creatures.plugins.intellij.agenteering"
val PLUGIN: IdeaPluginDescriptor? get() {
    val pluginId = PluginId.getId(PLUGIN_ID)
    return PluginManagerCore.getPlugins().firstOrNull { it.pluginId == pluginId }
}

object CaosFileUtil {

    private const val RESOURCES_FOLDER = "classes"

    private val PLUGIN_HOME_FILE: File?
        get() = PLUGIN?.path


    val PLUGIN_HOME_DIRECTORY: VirtualFile?
        get() {
            val file = PLUGIN_HOME_FILE ?: return null
            val libFolder = VfsUtil.findFileByIoFile(file, true)?.findChild("lib")
                    ?: return DEBUG_PLUGIN_HOME_DIRECTORY
            val jar = libFolder.children.filter {
                it.name.startsWith("CaosPlugin") && it.extension == "jar"
            }.firstOrNull()
                    ?: return DEBUG_PLUGIN_HOME_DIRECTORY
            return JarFileSystem.getInstance().getJarRootForLocalFile(jar) ?: DEBUG_PLUGIN_HOME_DIRECTORY
        }

    private val DEBUG_PLUGIN_HOME_DIRECTORY: VirtualFile?
        get() {
            val file = PLUGIN_HOME_FILE ?: return null
            return VfsUtil.findFileByIoFile(file, true)?.findChild(RESOURCES_FOLDER)
        }

    private val PLUGIN_RESOURCES_DIRECTORY: VirtualFile?
        get() = PLUGIN_HOME_DIRECTORY

    fun getPluginResourceFile(relativePath: String): VirtualFile? {
        return PLUGIN_RESOURCES_DIRECTORY?.findFileByRelativePath(relativePath)
    }

}

fun copyToClipboard(string: String) {
    val stringSelection = StringSelection(string)
    val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(stringSelection, null)
}

fun findFileBySharedModuleAndRelativePath(project: Project, baseFile: VirtualFile, fileRelativePath: String): VirtualFile? {
    val module = ModuleUtil.findModuleForFile(baseFile, project)
            ?: return null
    val file= module.moduleFile?.findFileByRelativePath(fileRelativePath)
    if (file != null)
        return file
    val path = module.moduleFile?.path + fileRelativePath
    return LocalFileSystem.getInstance().findFileByPath(path)
}

fun findFileByRelativePath(baseFile: VirtualFile, fileRelativePath: String): VirtualFile? {
    val relativePath = if (fileRelativePath.startsWith("/")) fileRelativePath.substring(1) else fileRelativePath
    return VfsUtilCore.findRelativeFile(relativePath, baseFile) ?: VfsUtilCore.findRelativeFile("/$relativePath", baseFile)
}