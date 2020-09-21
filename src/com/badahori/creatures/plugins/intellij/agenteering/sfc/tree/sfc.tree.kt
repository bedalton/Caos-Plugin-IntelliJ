package com.badahori.creatures.plugins.intellij.agenteering.sfc.tree

import com.badahori.creatures.plugins.intellij.agenteering.sfc.SfcFile
import com.badahori.creatures.plugins.intellij.agenteering.sfc.reader.SfcReader
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import icons.CaosScriptIcons

class SfcFileTreeNode(project:Project, virtualFile: VirtualFile) : AbstractTreeNode<VirtualFile>(project, virtualFile) {
    override fun update(presentation: PresentationData) {
        presentation.presentableText = virtualFile.name
        presentation.setIcon(CaosScriptIcons.SFC_FILE_ICON)
        presentation.locationString = null
    }

    val sfc:SfcFile? by lazy {
        try {
            virtualFile.getUserData(SFC_FILE_KEY) ?: SfcReader.readFile(virtualFile.contentsToByteArray())
        } catch(e:Exception) {
            null
        }
    }

    override fun getChildren(): MutableCollection<out AbstractTreeNode<Any>> {
        val scripts = sfc?.allScripts.orEmpty()
        return scripts.map {
            
        }.toMutableList()
    }
}

private val SFC_FILE_KEY = Key<SfcFile?>("caos.sfc.SFC_FILE")