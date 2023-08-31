package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.actions

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler.Caos2CobCompiler
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler.Caos2CobCompiler.CompilationResults
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.files
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.AgentMessages
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFileType
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Cob
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.utils.contents
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import icons.CaosScriptIcons
import kotlinx.coroutines.runBlocking

/**
 * Creates a file
 * @todo implement multiple file types (ie. implementations or protocols)
 */
class CompileCaos2CobAction : AnAction(
    /* text = */ AgentMessages.message("cob.caos2cob.compile.title"),
    /* description = */ AgentMessages.message("cob.caos2cob.compile.description"),
    /* icon = */ CaosScriptIcons.C1_COB_FILE_ICON
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
            ?: return
        val files = e.files.flatMap { file -> getCaosFiles(project, file) }
            .filter {
                it.isCaos2Cob
            }
        compile(project, files)
    }


    override fun update(event: AnActionEvent) {
        val enabled = event.files.any { file ->
            hasCaos2Cob(file)
        }
        val presentation = event.presentation
        presentation.isVisible = enabled
        presentation.text = AgentMessages.message("cob.caos2cob.compile.title")
        presentation.description = AgentMessages.message("cob.caos2cob.compile.description")
    }

    // Static Methods
    companion object {
        private val isCaos2CobRegex =
            "[*]{2}[Cc][Aa][Oo][Ss]2[Cc][Oo][Bb]|[*]#\\s*(C1-Name|C2-Name)".toRegex(RegexOption.IGNORE_CASE)

        private fun hasCaos2Cob(file: VirtualFile): Boolean {
            if (file.isDirectory) {
                ProgressIndicatorProvider.checkCanceled()
                return file.children.any(::hasCaos2Cob)
            }
            return file.fileType == CaosScriptFileType.INSTANCE && isCaos2CobRegex.containsMatchIn(file.contents)
        }

        internal fun compile(project: Project, file: CaosScriptFile) {

            val compilationResult = CompilationResults(1)
            // Run compile phase in background
            // Requires read-access though, so will have to move back onto ui thread I think
            runBackgroundableTask("Compile 1 Caos2Cob Files") { progressIndicator ->
                invokeLater {
                    progressIndicator.checkCanceled()
                    // Ensure in read action
                    runWriteAction action@{
                        runBlocking {
                            // Update progress indicator
                            Caos2CobCompiler.compile(project, compilationResult, file, progressIndicator)
                            printResult(project, compilationResult)
                        }
                    }
                }
            }
        }


        private fun getCaosFiles(project: Project, file: VirtualFile): List<CaosScriptFile> {
            if (file.isDirectory) {
                @Suppress("UnsafeVfsRecursion")
                return file.children.flatMap { child -> getCaosFiles(project, child) }
            }
            return (file.getPsiFile(project) as? CaosScriptFile)?.let { script ->
                listOf(script)
            } ?: emptyList()
        }

        internal fun compile(project: Project, files: List<CaosScriptFile>) {
            val numFiles = files.size
            if (numFiles == 0) {
                CaosNotifications.showWarning(
                    project,
                    "CAOS2Cob",
                    "No CAOS2Cob files passed to compiler."
                )
                return
            }

            val compilationResult = CompilationResults(numFiles)
            // Run compile phase in background
            // Requires read-access though, so will have to move back onto ui thread I think
            runBackgroundableTask("Compile $numFiles Caos2Cob files") { progressIndicator ->
                files.forEach { file ->
                    // Run on pooled event dispatch thread
                    invokeLater {
                        progressIndicator.checkCanceled()
                        // Ensure in read action
                        runWriteAction action@{
                            // Update progress indicator
                            runBlocking {
                                Caos2CobCompiler.compile(
                                    project = project,
                                    compilationResult = compilationResult,
                                    file = file,
                                    progressIndicator = progressIndicator
                                )
                            }
                            if (compilationResult.index == numFiles) {
                                printResult(project, compilationResult)
                            }
                        }
                    }
                }
            }
        }
    }
}


private fun printResult(project: Project, compilationResult: CompilationResults) {
    val successes = compilationResult.success
    val failures = compilationResult.failures
    val warningText = if (compilationResult.warnings > 0)
        " with ${compilationResult.warnings} warnings"
    else
        ""
    val numFiles = compilationResult.caos2CobFiles
    when {
        failures == 0 && successes > 0 ->
            CaosNotifications.showInfo(
                project,
                "CAOS2Cob Result",
                "Compiled $numFiles CAOS2Cob cobs successfully$warningText"
            )
        failures > 0 && successes == 0 ->
            CaosNotifications.showError(
                project,
                "CAOS2Cob Result",
                "Failed to compile any of the $numFiles CAOS2Cob files successfully"
            )
        failures == 0 && successes == 0 ->
            CaosNotifications.showError(
                project,
                "CAOS2Cob Result",
                "Compiler failed to run without error"
            )
        else ->
            CaosNotifications.showError(
                project,
                "CAOS2Cob Result",
                "Failed to compile $failures out of $numFiles CAOS2Cob files"
            )
    }
}