package com.badahori.creatures.plugins.intellij.agenteering.bundles.general

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.actions.CompileCaos2CobAction
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.compiler.CompilePrayFileAction
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayFileDetector
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.files
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.contents
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vfs.VirtualFile
import icons.CaosScriptIcons

class CompileCAOS2Action: AnAction(
    "Compile Agent",
    "Compiles selected CAOS2Cob, CAOS2Pray and Pray files into agents and cobs",
    AllIcons.Actions.Compile
) {

    var file: VirtualFile? = null

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project
        e.presentation.isVisible = project != null && (file != null || e.files.any { isCompilable(it) })
    }
    override fun actionPerformed(e: AnActionEvent) {

        val project = e.project
            ?: return

        val files = e.files + file?.let { arrayOf(it) }.orEmpty()

        val caosFiles = files
            .distinct()
            .mapNotNull {
                it.getPsiFile(project) as? CaosScriptFile
            }

        val caos2Cob = caosFiles
            .filter { it.isCaos2Cob }

        try {
            if (caos2Cob.isNotEmpty())
                CompileCaos2CobAction.compile(project, caos2Cob)
        } catch (e: Exception) {

        }

        val caos2Pray = caosFiles
            .filter { it.isCaos2Pray }

        val prayFiles = e.files
            .filter {
                PrayFileDetector.isPrayFile(it.contents)
            }
        try {
            val allPrayFiles: List<VirtualFile> = caos2Pray.map { it.virtualFile!! } + prayFiles
            if (allPrayFiles.isNotEmpty()) {
                CompilePrayFileAction.compile(project, allPrayFiles.toTypedArray())
            }
        } catch (e: Exception) {

        }
    }

    private fun isCompilable(file: VirtualFile): Boolean {
        if (file.fileType == CaosScriptFileType.INSTANCE)
            return file.contents.contains("*#")
        if (file.fileType == PrayFileType)
            return true
        return PrayFileDetector.isPrayFile(file.contents)
    }
}