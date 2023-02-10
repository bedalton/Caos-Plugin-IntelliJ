@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.*
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.PathUtil
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.security.DigestInputStream
import java.security.MessageDigest


val VirtualFile.contents: String
    get() {
        return this.inputStream.reader().readText()
    }

fun VirtualFile.getModule(project: Project): Module? {
    if (project.isDisposed) {
        return null
    }
    return ModuleUtil.findModuleForFile(this, project)
}

val Document.virtualFile
    get() = FileDocumentManager.getInstance().getFile(this)

val Editor.virtualFile
    get() = document.virtualFile

fun VirtualFile.getPsiFile(project: Project): PsiFile? {
    if (project.isDisposed) {
        return null
    }
    val file = PsiManager.getInstance(project).findFile(this)
    if (file.isInvalid) {
        return null
    }
    return file
}


private const val PLUGIN_ID = "com.badahori.creatures.plugins.intellij.agenteering"
val PLUGIN: IdeaPluginDescriptor?
    get() {
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
            val libFolder = VfsUtil.findFileByIoFile(file, false)?.findChild("lib")
                ?: return DEBUG_PLUGIN_HOME_DIRECTORY
            val jar = libFolder.children.firstOrNull {
                it.name.startsWith("CaosPlugin") && it.extension == "jar"
            } ?: return DEBUG_PLUGIN_HOME_DIRECTORY
            return JarFileSystem.getInstance().getJarRootForLocalFile(jar)
                ?: DEBUG_PLUGIN_HOME_DIRECTORY
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

    /**
     * Copy a file from source to destination.
     *
     * @param source
     * the source
     * @param destination
     * the destination
     * @return True if succeeded , False if not
     */
    fun copyStreamToFile(source: InputStream, destination: String, throws: Boolean = false): Boolean {
        var success = true
        try {
            Files.copy(source, Paths.get(destination))
        } catch (se: SecurityException) {
            LOGGER.severe("Failed to copy resource to $destination with Security exception error: ${se.message}")
            se.printStackTrace()
            if (throws)
                throw se
        } catch (ex: IOException) {
            LOGGER.severe("Failed to copy resource to $destination with error: ${ex.message}")
            ex.printStackTrace()
            if (throws)
                throw ex
            ex.printStackTrace()
            success = false
        }
        return success
    }

    /**
     * Copy a file from source to destination.
     *
     * @param source
     * the source
     * @param destination
     * the destination
     * @return True if succeeded , False if not
     */
    fun copyStreamToFile(source: InputStream, destination: File, throws: Boolean = false): Boolean {

        val directory = if (destination.isDirectory) {
            destination
        } else {
            destination.parentFile
        }
        try {
            val made = directory.mkdirs()
            if (!made && !directory.exists()) {
                throw IOException("Failed to create all parent directories for path: ${directory.absolutePath}")
            }
        } catch (e: Exception) {
            LOGGER.severe("Failed to create destination parent folders.")
            e.printStackTrace()
            if (throws)
                throw e
        }
        return copyStreamToFile(source, destination.path, throws)
    }

    fun writeChild(root: VirtualFile, relativePath: String, data: String): VirtualFile {
        return createFileOrDir(root, relativePath, VfsUtil.toByteArray(root, data), false)
    }

    fun writeChild(root: VirtualFile, relativePath: String, data: ByteArray): VirtualFile {
        return createFileOrDir(root, relativePath, data, false)
    }

    /**
     * Taken from com.intellij.testFramework.TemporaryDirectory
     * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
     */
    private fun createFileOrDir(root: VirtualFile, relativePath: String, data: ByteArray, dir: Boolean): VirtualFile {
        return try {
            WriteAction.computeAndWait<VirtualFile, IOException> {
                var parent = root
                for (name in StringUtil.tokenize(
                    PathUtil.getParentPath(
                        relativePath
                    ), "/"
                )) {
                    var child = parent.findChild(name!!)
                    if (child == null || !child.isValid) {
                        child = parent.createChildDirectory(CaosFileUtil::class.java, name)
                    }
                    parent = child
                }
                parent.children //need this to ensure that fileCreated event is fired
                val name = PathUtil.getFileName(relativePath)
                var file: VirtualFile?
                if (dir) {
                    file = parent.createChildDirectory(CaosFileUtil::class.java, name)
                } else {
                    file = parent.findChild(name)
                    if (file == null) {
                        file = parent.createChildData(CaosFileUtil::class.java, name)
                    }
                    file.setBinaryContent(data)
                    // update the document now, otherwise MemoryDiskConflictResolver will do it later at unexpected moment of time
                    FileDocumentManager.getInstance().reloadFiles(file)
                }
                file
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

}

fun copyToClipboard(string: String) {
    val stringSelection = StringSelection(string)
    val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(stringSelection, null)
}

fun findFileBySharedModuleAndRelativePath(
    project: Project,
    baseFile: VirtualFile,
    fileRelativePath: String
): VirtualFile? {
    val module = ModuleUtil.findModuleForFile(baseFile, project)
        ?: return null
    val file = module.myModuleFile?.findFileByRelativePath(fileRelativePath)
    if (file != null) {
        return file
    }
    val path = module.myModulePath + fileRelativePath
    return LocalFileSystem.getInstance().findFileByPath(path)
}

fun findFileByRelativePath(baseFile: VirtualFile, fileRelativePath: String): VirtualFile? {
    val relativePath = if (fileRelativePath.startsWith("/")) fileRelativePath.substring(1) else fileRelativePath
    return VfsUtilCore.findRelativeFile(relativePath, baseFile) ?: VfsUtilCore.findRelativeFile(
        "/$relativePath",
        baseFile
    )
}

fun VirtualFile.md5(): String? {
    val md = MessageDigest.getInstance("MD5")
    inputStream.use { input ->
        DigestInputStream(
            input,
            md
        ).use {
            @Suppress("ControlFlowWithEmptyBody")
            while (input.read() > 0) {
                // Do nothing, just need to read file
            }
        }
    }
    return md.digest()?.contentToString()
}

fun VirtualFile.writeChild(relativePath: String, text: String): VirtualFile {
    return CaosFileUtil.writeChild(this, relativePath, text)
}

fun VirtualFile.writeChild(relativePath: String, data: ByteArray): VirtualFile {
    return CaosFileUtil.writeChild(this, relativePath, data)
}