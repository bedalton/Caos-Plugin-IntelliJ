package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.compiler

import bedalton.creatures.pray.compiler.cli.PrayCliOptions
import bedalton.creatures.pray.compiler.cli.compilePrayAndWrite
import bedalton.creatures.pray.compiler.pray.PrayParseValidationFailException
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayFile
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayFileDetector
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.files
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Pray
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.utils.className
import com.badahori.creatures.plugins.intellij.agenteering.utils.getPsiFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.invokeLater
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.vfs.VirtualFile
import icons.CaosScriptIcons
import java.io.File

class CompilePrayFileAction(private val transient: Boolean = true): AnAction("Compile PRAY Agent") {

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project
        e.presentation.icon = CaosScriptIcons.BUILD
        e.presentation.isVisible = !transient || (project != null && e.files.any { file -> isPrayOrCaos2Pray(project, file) })
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project
            ?: return
        val files = e.files.ifEmpty { null }
            ?: return
        compile(project, files)
    }

    companion object {
        private var lastCLIOptions: PrayCliOptions? = null
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

        internal fun getOpts(defaultOpts: PrayCliOptions? = lastCLIOptions, apply:(PrayCliOptions?)->Unit) {
            val panel = CompilerOptions(defaultOpts)
            val builder = DialogBuilder()
            builder.setCenterPanel(panel.component)
            builder.addCancelAction()
            builder.setCancelOperation {
                builder.window.isVisible = false
                builder.window.dispose()
                apply(null)
            }
            val okay = builder.addOkAction()
            okay.setText("Compile")
            builder.setOkOperation {
                val options = panel.options
                lastCLIOptions = options
                builder.window.isVisible = false
                builder.window.dispose()
                apply(options)
            }
            builder.showAndGet()
        }

        internal fun compile(project: Project, files: Array<VirtualFile>) {
            getOpts { opts ->
                if (opts == null) {
                    return@getOpts
                }
                runBackgroundableTask("Compile Pray ${ if(files.isNotEmpty()) "Files" else "File"}") { indicator ->
                    compile(project, files, opts, indicator)
                }
            }
        }

        internal fun compile(project: Project, filePath: String, opts: PrayCliOptions): String? {
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
            val fileOpts = opts.copy(
                inputFile = ioFile.path
            )
            try {
                return compilePrayAndWrite(fileOpts, ioFile.parent, false)
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




        private fun compile(project: Project, files: Array<VirtualFile>, opts: PrayCliOptions, indicator: ProgressIndicator) {
            val success = mutableListOf<String>()
            var errors = 0

            val size = files.size
            var i = 0
            for (file in files) {
                if (size > 1) {
                    indicator.text2 = " (${i++}/$size): ${file.name}"
                } else {
                    indicator.text2 = ": ${file.name}"
                }
                val outputFile = compile(project, file.path, opts)
                if (outputFile != null) {
                    success.add("${file.name} -> ${File(outputFile).name}")
                } else {
                    errors++
                }

            }
            invokeLater {
                if (errors == files.size) {
                    CaosNotifications.showError(project, "Pray Compile Error", "Failed to compile any PRAY files")
                } else if (errors > 0) {
                    CaosNotifications.showInfo(
                        project,
                        "Pray Compile",
                        "Compiled ${success.size}/${files.size} successfully. \n${success.joinToString("\n\t-")}")
                } else {
                    CaosNotifications.showInfo(
                        project,
                        "Pray Compile",
                        "Compiled ${success.size} PRAY files successfully\n${success.joinToString("\n\t-")}")
                }
            }
        }


    }


}