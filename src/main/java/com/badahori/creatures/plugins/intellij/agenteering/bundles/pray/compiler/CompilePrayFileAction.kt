package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.compiler

import bedalton.creatures.agents.pray.compiler.PrayCompileOptions
import bedalton.creatures.agents.pray.compiler.PrayCompilerTask
import bedalton.creatures.agents.pray.compiler.compilePrayAndWrite
import bedalton.creatures.agents.pray.compiler.pray.PrayParseValidationFailException
import com.bedalton.log.logProgress
import bedalton.creatures.common.util.className
import bedalton.creatures.common.util.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayFile
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayFileDetector
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.files
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Pray
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.closeWithOkExitCode
import com.bedalton.vfs.LocalFileSystem
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import icons.CaosScriptIcons
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class CompilePrayFileAction(private val transient: Boolean = true) : AnAction("Compile PRAY Agent") {


    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project
        e.presentation.icon = CaosScriptIcons.BUILD
        val visible = !transient || (project != null && e.files.any { file -> isPrayOrCaos2Pray(project, file) })
        e.presentation.isVisible = visible

    }

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project
            ?: return
        val files = e.files.nullIfEmpty()
            ?: return
        compile(project, files)
    }

    companion object {
        private var lastCLIOptions: PrayCompileOptions? = null
        private fun isPrayOrCaos2Pray(project: Project, virtualFile: VirtualFile): Boolean {
            val psiFile = virtualFile.getPsiFile(project)
                ?: return false
            return when (psiFile) {
                is CaosScriptFile -> psiFile.isCaos2Pray
                is PrayFile -> true
                else -> {
                    val text = psiFile.text.nullIfEmpty()
                        ?: return false
                    PrayFileDetector.isPrayFile(text)
                }
            }
        }

        internal fun getOpts(
            defaultOpts: PrayCompileOptions? = lastCLIOptions
        ): PrayCompileOptions? {
            val panel = CompilerOptions(defaultOpts)
            val builder = DialogBuilder()
            builder.setCenterPanel(panel.component)
            builder.addCancelAction()
            var options: PrayCompileOptions? = null
            builder.setCancelOperation {
                builder.closeWithCancelExitCode()
            }
            val okay = builder.addOkAction()
            okay.setText("Compile")
            builder.setOkOperation {
                options = panel.options
                lastCLIOptions = options
                builder.closeWithOkExitCode()
            }
            return if (builder.showAndGet()) {
                LOGGER.info("Got Compiler options")
                options
            } else {
                LOGGER.info("Failed to get compiler options")
                null
            }
        }

        internal fun compile(project: Project, files: Array<VirtualFile>) {
            val opts = getOpts()
            if (opts == null) {
                LOGGER.severe("Failed to get opts for compile PRAY file")
                return
            }
            runBackgroundableTask(
                "Compile Pray ${if (files.isNotEmpty()) "Files" else "File"}",
                project
            ) { indicator ->
                compile(project, files, opts, indicator)
            }
        }

        internal suspend fun compile(project: Project, filePath: String, opts: PrayCompileOptions): String? {
            val ioFile = File(filePath)
            if (!ioFile.exists()) {
                invokeLater {
                    CaosNotifications.showError(
                        project,
                        "Pray Compile Error",
                        "Cannot compile files without physical backing"
                    )
                }
                return null
            }
            val fileOpts = PrayCompilerTask(
                inputFile = ioFile.path,
                outputDirectory = ioFile.parent,
                pure = false
            )
                .withValidate(opts.validate)
                .withMergeRscr(opts.mergeRscr)
                .withMergeScripts(opts.mergeScripts)
                .withIsJoin(opts.isJoin)
                .withJoins(opts.joins)
                .withGenerateAgentRemovers(opts.generateAgentRemovers)
                .withGenerateScriptRemovers(opts.generateScriptRemovers)
                .withLogProgress(false)
                .withPrintStats(false)
            logProgress(true)
            try {
                return compilePrayAndWrite(LocalFileSystem!!, fileOpts, false)
            } catch (e: Exception) {
                invokeLater {
                    if (e is PrayParseValidationFailException) {
                        CaosNotifications.showError(
                            project,
                            "Pray Compile Error",
                            "PRAY file failed validation with errors:\n\t- ${e.errors.joinToString("\n\t- ") { "${it.message} @ Line#${it.lineNumber}" }}"
                        )
                    } else {
                        CaosNotifications.showError(
                            project,
                            "Pray Compile Error",
                            "Compilation failed with error: ${e.className}(${e.message})"
                        )
                    }
                }
                e.printStackTrace()
            }
            return null
        }


        private fun compile(
            project: Project,
            files: Array<VirtualFile>,
            opts: PrayCompileOptions,
            indicator: ProgressIndicator
        ) {
            val success = mutableListOf<String>()
            var errors = 0

            val size = files.size
            var i = 0
            var done = 0
            val onDone = {
                if (errors == files.size) {
                    CaosNotifications.showError(project, "Pray Compile Error", "Failed to compile any PRAY files")
                } else if (errors > 0) {
                    CaosNotifications.showInfo(
                        project,
                        "Pray Compile",
                        "Compiled ${success.size}/${files.size} successfully. \n${success.joinToString("\n\t-")}"
                    )
                } else {
                    CaosNotifications.showInfo(
                        project,
                        "Pray Compile",
                        "Compiled ${success.size} PRAY files successfully\n${success.joinToString("\n\t-")}"
                    )
                }
            }
            for (file in files) {
                if (size > 1) {
                    indicator.text2 = " (${i++}/$size): ${file.name}"
                } else {
                    indicator.text2 = ": ${file.name}"
                }
                GlobalScope.launch {
                    val outputFile = compile(project, file.path, opts)
                    done++
                    if (outputFile != null) {
                        success.add("${file.name} -> ${File(outputFile).name}")
                        invokeLater {
                            VfsUtil.markDirtyAndRefresh(true, true, true, file.parent)
                        }
                    } else {
                        errors++
                    }
                    if (done == size) {
                        invokeLater { onDone() }
                    }
                }

            }
        }


    }


}