package com.openc2e.plugins.intellij.caos.utils

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
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotificationPanel
import com.openc2e.plugins.intellij.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFile
import com.openc2e.plugins.intellij.caos.project.CaosScriptEditorToolbar
import com.openc2e.plugins.intellij.caos.project.EditorToolbar
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.io.File
import java.util.logging.Logger


private val LOGGER:Logger = Logger.getLogger("#"+ CaosFileUtil::class.java.canonicalName)

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


object CaosFileUtil {

    private const val PLUGIN_ID = "com.openc2e.plugins.intellij.caos"

    private const val RESOURCES_FOLDER = "classes"

    private val PLUGIN_HOME_FILE: File?
        get() = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))?.path

    val PLUGIN_HOME_DIRECTORY: VirtualFile?
        get() {
            val file = PLUGIN_HOME_FILE ?: return null
            val libFolder = VfsUtil.findFileByIoFile(file, true)?.findChild("lib")
                    ?: return DEBUG_PLUGIN_HOME_DIRECTORY
            val jar = libFolder.findChild("Creatures Caos Script.jar")
                    ?: return DEBUG_PLUGIN_HOME_DIRECTORY
            return JarFileSystem.getInstance().getJarRootForLocalFile(jar) ?: DEBUG_PLUGIN_HOME_DIRECTORY
        }

    private val DEBUG_PLUGIN_HOME_DIRECTORY:VirtualFile?
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

fun copyToClipboard(string:String) {
    val stringSelection = StringSelection(string)
    val clipboard: Clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
    clipboard.setContents(stringSelection, null)
}

fun CaosScriptFile.trimErrorSpaces() {
    ApplicationManager.getApplication().runWriteAction run@{
        CommandProcessor.getInstance().executeCommand(project, run@{
            val text = text
            var trimmedText = text.replace("\\s*,[ \t]*".toRegex(), ",")
            trimmedText = trimmedText.replace("[ ]+\n".toRegex(), "\n").trim()
            if (trimmedText == text)
                return@run
            val document = document
                    ?: return@run
            document.replaceString(0, text.length, trimmedText)
            //document.setText(trimmedText)
            //if (document.text == trimmedText)
            // todo
        }, "Strip Error Spaces", "Strip Error Spaces")
    }
}

fun CaosScriptFile.copyAsOneLine() {
    ApplicationManager.getApplication().runWriteAction run@{
        val text = text
        var trimmedText = text.split("\n").map {
            val line = it.trim()
            if (line.startsWith("*"))
                ""
            else
                line
        }.joinToString(" ")
        trimmedText = trimmedText.replace("\\s+".toRegex(), " ")
        trimmedText = trimmedText.replace("\\s*,\\s*".toRegex(), ",").trim()
        copyToClipboard(trimmedText)
    }
}