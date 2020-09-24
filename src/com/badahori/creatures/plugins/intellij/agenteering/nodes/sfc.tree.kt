package com.badahori.creatures.plugins.intellij.agenteering.nodes

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosInjectorNotifications
import com.badahori.creatures.plugins.intellij.agenteering.sfc.SfcFile
import com.badahori.creatures.plugins.intellij.agenteering.sfc.SfcFileDataHolder
import com.badahori.creatures.plugins.intellij.agenteering.sfc.lang.SfcDecompiledFilePropertyPusher
import com.badahori.creatures.plugins.intellij.agenteering.sfc.reader.SfcReader
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import icons.CaosScriptIcons
import java.util.concurrent.atomic.AtomicInteger

internal class SfcFileTreeNode(project: Project, private val myVirtualFile: VirtualFile) : AbstractTreeNode<VirtualFile>(project, myVirtualFile) {

    override fun update(presentation: PresentationData) {
        presentation.presentableText = myVirtualFile.name
        presentation.setIcon(CaosScriptIcons.SFC_FILE_ICON)
        presentation.locationString = null
    }

    private val rootDirectory = getSfcAsFolder(myVirtualFile)

    val sfc: SfcFile? by lazy {
        try {
            // Caching is handled by read file method in SfcReader
            SfcReader.readFile(myVirtualFile).data
        } catch (e:Exception) {
            val error = "Failed to parse SFC file: '${myVirtualFile.path}' with error:\n\t${e.message}"
            SfcDecompiledFilePropertyPusher.writeToStorage(myVirtualFile, SfcFileDataHolder(error = error))
            LOGGER.severe(error)
            e.printStackTrace()
            CaosInjectorNotifications.show(project, "SFC Error", "Failed to parse SFC file '${myVirtualFile.name}'. Non-Eden.sfc files are un-parsable.", NotificationType.ERROR)
            null
        }
    }

    private val eventScriptRegex = "^\\s*[Ss][Cc][Rr][Pp]\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+).*".toRegex()

    val scripts: List<CaosScriptFile> by lazy {
        val sfc = sfc ?: return@lazy emptyList<CaosScriptFile>()
        val sfcVariant = sfc.variant
        val scripts = sfc.allScripts
        val maxIndexLength = "${scripts.size}".length
        scripts.mapIndexed { index, script ->
            val fileName = getScriptName(index, maxIndexLength, script)
            rootDirectory.createChildCaosScript(project, sfcVariant, fileName, script)
        }
    }

    fun getScriptName(index: Int, maxIndexLength: Int, script: String): String {
        val matches = eventScriptRegex.matchEntire(script)
                ?: return "Macro ${"$index".padStart(maxIndexLength, '0')}"
        return matches.groupValues.drop(1).dropLast(0).joinToString(" ")
    }

    override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
        return scripts.mapIndexed { index, file ->
            CaosScriptFileTreeNode(
                    file,
                    index,
                    file.containingFile.name,
                    true
            )
        }.toMutableList()
    }
}


private fun getSfcAsFolder(virtualFile: VirtualFile): CaosVirtualFile {
    val folderName = virtualFile.getUserData(SFC_FOLDER_KEY)
            ?: "${virtualFile.name}.(${decompiledId.incrementAndGet()})"
    return CaosVirtualFileSystem.instance.getOrCreateRootChildDirectory(folderName)
}

private val SFC_FOLDER_KEY = Key<String?>("caos.sfc.SFC_FILE_AS_DIRECTORY_NAME")
private val decompiledId = AtomicInteger(0)
