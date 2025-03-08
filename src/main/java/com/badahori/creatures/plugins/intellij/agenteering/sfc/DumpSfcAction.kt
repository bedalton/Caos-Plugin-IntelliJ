package com.badahori.creatures.plugins.intellij.agenteering.sfc

import com.badahori.creatures.plugins.intellij.agenteering.caos.action.files
import com.badahori.creatures.plugins.intellij.agenteering.sfc.lang.SfcFileType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.vfs.VirtualFile
import icons.CaosScriptIcons

class DumpSfcAction: AnAction() {

    override fun update(e: AnActionEvent) {
        super.update(e)
        val isSFC = e.files.any { it.fileType == SfcFileType }
        e.presentation.isEnabled = isSFC
        e.presentation.isVisible = isSFC
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent)   {
        val project = e.project
            ?: return
        val sfcFiles = e.files.filter { it.fileType == SfcFileType }
        WriteCommandAction.runWriteCommandAction(project) {
            for (file in sfcFiles) {
                dumpSFC(file)
            }
        }
    }

    private fun dumpSFC(virtualFile: VirtualFile) {
        val json = decompileSFCToJson(virtualFile)
        runWriteAction {
            val out = virtualFile.parent.createChildData(virtualFile, virtualFile.name + ".json")
            out.setBinaryContent(json.toByteArray())
        }
    }

}

