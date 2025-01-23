package com.badahori.creatures.plugins.intellij.agenteering.bundles.general

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.actions.CompileCaos2CobAction
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.compiler.CompilePrayFileAction
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayFileDetector
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.files
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Cob
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Pray
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.contents
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.rethrowAnyCancellationException
import com.bedalton.common.util.className
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.vfs.VirtualFile

class CompileCAOS2Action: AnAction(
    { "Compile Agent" },
    { "Compiles selected CAOS2Cob, CAOS2Pray and Pray files into agents and cobs" },
    AllIcons.Actions.Compile
) {

    var file: VirtualFile? = null


    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isVisible = isVisible(e)
    }

    private fun isVisible(e: AnActionEvent): Boolean  = runReadAction visible@{
        val project = e.project
                ?: return@visible false
        if (project.isDisposed) {
            return@visible false
        }
        val files = e.files
        val fileCount = files.size
        when {
            fileCount <= 7 -> files.any { isCompilable(it) }
            else -> true
        }
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
            if (caos2Cob.isNotEmpty()) {
                CompileCaos2CobAction.compile(project, caos2Cob)
            }
        } catch (e: Exception) {
            e.rethrowAnyCancellationException()
            LOGGER.severe("Failed to compile CAOS2Cob; ${e.className}(${e.message}); Files: ${caos2Cob.joinToString { it.name }}")
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
            e.rethrowAnyCancellationException()
            LOGGER.severe("Failed to compile CAOS2Pray; ${e.className}(${e.message}) files: ${caos2Pray.joinToString { it.name }}")
        }
    }

    private fun isCompilable(file: VirtualFile): Boolean {
        if (file.fileType == CaosScriptFileType.INSTANCE) {
            return file.contents.contains("*#")
        }
        if (file.fileType == PrayFileType) {
            return true
        }
        return PrayFileDetector.isPrayFile(file.contents)
    }
}