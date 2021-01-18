package com.badahori.creatures.plugins.intellij.agenteering.bundles.actions

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.files
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptCollapseNewLineIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CollapseChar
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.caos2CobVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Cob
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant.C1
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.writeChild
import icons.CaosScriptIcons
import java.util.*

/**
 * Creates a file
 * @todo implement multiple file types (ie. implementations or protocols)
 */
class CompileCaos2CobAction : AnAction(
    CaosBundle.message("cob.caos2cob.compile.title"),
    CaosBundle.message("cob.caos2cob.compile.description"),
    CaosScriptIcons.C1_COB_FILE_ICON
), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
            ?: return
        val files = e.files.flatMap { file -> getCaosFiles(project, file) }
            .filter {
                it.isCaos2Cob
            }
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
        // Requires read access though, so will have to move back onto ui thread I think
        runBackgroundableTask("Compile $numFiles Caos2Cob files") { progressIndicator ->
            files.forEach { file ->
                // Run on pooled event dispatch thread
                ApplicationManager.getApplication().invokeLater {
                    progressIndicator.checkCanceled()
                    // Ensure in read action
                    runWriteAction action@{
                        processFile(project, compilationResult, file, progressIndicator)
                        if (compilationResult.index == numFiles) {
                            printResult(project, compilationResult)
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
        val message = when {
            failures == 0 && successes > 0 -> "Compiled $numFiles CAOS2Cob cobs successfully$warningText"
            failures > 0 && successes == 0 -> "Failed to compile any of the $numFiles CAOS2Cob files successfully"
            failures == 0 && successes == 0 -> "Compiler failed to run without error"
            else -> "Failed to compile $failures out of $numFiles CAOS2Cob files"
        }
        CaosNotifications.showInfo(
            project,
            "CAOS2Cob Result",
            message
        )
    }

    private fun processFile(
        project: Project,
        compilationResult: CompilationResults,
        file: CaosScriptFile,
        progressIndicator: ProgressIndicator
    ): Boolean {
        // Try to get Caos2Cob manifest from CAOS file.
        val compilerData: Caos2Cob = try {
            getCobManifest(project, compilationResult, file)
        } catch (e: Caos2CobException) {
            // Failed to convert CAOS script file to COB data struct
            CaosNotifications.showError(
                project,
                "CAOS2Cob Failure",
                "Failed to compile ${file.name} with error: ${e.message}"
            )
            compilationResult.failures++
            return false
        } catch (e: Exception) {
            LOGGER.severe(e.message)
            e.printStackTrace()
            return false
        }
        // Update progress indicator
        val type = if (compilerData is Caos2CobC1) "C1" else "C2"
        val i = compilationResult.failures + compilationResult.success
        val numFiles = compilationResult.caos2CobFiles
        progressIndicator.fraction = i / numFiles.toDouble()
        progressIndicator.text = "Compile $i/$numFiles COBS"
        progressIndicator.text2 = "Compiling $type COB: ${compilerData.targetFile}"
        val parent = (file.virtualFile ?: file.originalFile.virtualFile).parent
        if (parent == null) {
            CaosNotifications.showError(
                project,
                "CAOS2Cob Plugin Error",
                "Failed to find parent directory of CAOS2Cob file '${file.name}'"
            )
            ++compilationResult.failures
            return false
        }
        val dataOut = compile(project, file, compilerData)
        if (dataOut == null) {
            ++compilationResult.failures
            LOGGER.severe("Failed to compile data for file: ${file.name}")
            return false
        }
        if (!writeCob(project, parent, compilerData, dataOut)) {
            ++compilationResult.failures
            LOGGER.severe("Failed write COB data for file: ${file.name}")
            return false
        }
        if (compilerData is Caos2CobC1) {
            if (!writeRemoverCob(project, compilationResult, parent, file, compilerData)) {
                ++compilationResult.failures
                return false
            }
        }
        ++compilationResult.success
        return true
    }

    private fun writeRemoverCob(
        project: Project,
        compilationResult: CompilationResults,
        parent: VirtualFile,
        file: CaosScriptFile,
        compilerData: Caos2CobC1
    ): Boolean {
        // If C1 COB, create remover COB
        val removerCob = compilerData.removerCob
            ?: return true
        val error = CaosBundle.message(
            "cob.caos2cob.compile.auto-remover-name",
            removerCob.targetFile
        )
        if (!didShowAutoRemoverCobWarning && compilerData.removerName.nullIfEmpty() == null) {
            didShowAutoRemoverCobWarning = true
            CaosNotifications.showWarning(
                project,
                "CAOS2Cob Removal Script Warning",
                error
            )
        }
        val removerData = compile(project, file, removerCob)
        if (removerData == null) {
            ++compilationResult.failures
            LOGGER.severe("Failed to compile remover COB data")
            return false
        }
        if (!writeCob(project, parent, removerCob, removerData)) {
            ++compilationResult.failures
            LOGGER.severe("Failed to write remover COB data")
            return false
        } else {
            return true
        }
    }

    private fun compile(project: Project, file: CaosScriptFile, cob: Caos2Cob): ByteArray? {
        return try {
            cob.compile()
        } catch (e: Caos2CobException) {
            CaosNotifications.showError(
                project,
                "CAOS2Cob Failure",
                "Failed to compile ${file.name} with error: ${e.message}"
            )
            null
        } catch (e: Exception) {
            CaosNotifications.showError(
                project,
                "CAOS2Cob Failure",
                "Failed to compile ${file.name} with error: ${e.message}"
            )
            LOGGER.severe("Failed to compile COB. Error: " + e.message)
            e.printStackTrace()
            null
        }
    }

    private fun getCobManifest(
        project: Project,
        compilationResults: CompilationResults,
        mainFile: CaosScriptFile
    ): Caos2Cob {
        // Get parent directory for all read and write operations
        val directory = mainFile.virtualFile.parent

        // Get variant
        val variant = mainFile.caos2CobVariant
            ?: throw Caos2CobException("Failed to determine CAOS2Cob variant")

        // Convert raw tags to enum map
        val block = PsiTreeUtil.getChildOfType(mainFile, CaosScriptCaos2Block::class.java)
            ?: throw Caos2CobException("No CAOS2Cob directive block found")

        // Parse Caos2Cpb directives
        val cobTags = getCobTags(variant, block).map { (key, value) ->
            if (key == CobTag.THUMBNAIL && value != null) {
                val parts = Caos2CobUtil.getSpriteFrameInformation(value)
                key to directory.findChild(parts.first)?.let {
                    it.parent.path + "/" + value
                }
            } else {
                key to value
            }
        }.toMap().toMutableMap()
        val cobCommands = getCobCommands(variant, block)

        val agentNameFromTags = cobTags[CobTag.AGENT_NAME]
        val agentNameFromCommand = block.agentBlockNames.filter { it.first == variant.code }.firstOrNull()?.second
        if (agentNameFromCommand != null && agentNameFromTags != null && agentNameFromCommand != agentNameFromTags) {
            throw Caos2CobException("Conflicting use of '${variant.code}-Name' command and 'Agent Name' property")
        }
        val agentName = agentNameFromCommand ?: agentNameFromTags
        cobTags[CobTag.AGENT_NAME] = agentName
        // Take find all scripts in this file and all linked files.
        val linkedFiles = mainFile + collectLinkedFiles(project, compilationResults, variant, directory, cobCommands)
        val scripts = mutableListOf<CaosScriptScriptElement>()
        runWriteAction {
            linkedFiles.forEach { fileIn ->
                WriteCommandAction.writeCommandAction(project)
                    .shouldRecordActionForActiveDocument(false)
                    .withGroupId("CAOS2Cob")
                    .withName("Collapse CAOS2Cob script with commas")
                    .withUndoConfirmationPolicy(UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION)
                    .run<Exception> {
                        val file = fileIn.copy()
                        val collapsed = CaosScriptCollapseNewLineIntentionAction.collapseLines(file, CollapseChar.COMMA)
                        val items = PsiTreeUtil.collectElementsOfType(collapsed, CaosScriptScriptElement::class.java)
                        scripts.addAll(items)
                    }
            }
        }

        // Find Object scripts
        val objectScripts = scripts.filterIsInstance<CaosScriptEventScript>().map { it.text }

        // Get all install scripts
        val installScripts: List<String> = scripts.filterIsInstance<CaosScriptInstallScript>().map(stripIscr)

        if (scripts.any { it is CaosScriptMacro }) {
            compilationResults.warnings++
            CaosNotifications
                .showWarning(project, "CAOS2Cob", "Body scripts in CAOS2Cob files are ignored")
        }

        // Get removal script
        val removalScripts = scripts.filterIsInstance<CaosScriptRemovalScript>()
        val removalScript =
            getRemovalScript(project, compilationResults, mainFile.name, directory, cobCommands, removalScripts)


        // Format C1/C2 cobs respectively
        return if (variant == C1) {
            Caos2CobC1(
                cobData = cobTags,
                objectScripts = objectScripts,
                installScripts = installScripts,
                removalScript = removalScript
            )
        } else {
            val installScript = installScripts.joinToString(",")

            // Attach files are added to both depends and inline files list
            val attachments: Set<String> =
                cobCommands.filter { it.first == CobCommand.ATTACH }.flatMap { it.second }.toSet()

            val dependencies: Set<String> = attachments + cobCommands
                .filter { it.first == CobCommand.DEPEND }
                .flatMap { it.second }
                .toSet()

            val inlineFileNames: Set<String> = attachments + cobCommands
                .filter { it.first == CobCommand.INLINE }
                .flatMap { it.second }
                .toSet()

            val inlineFiles = inlineFileNames.map map@{ fileName ->
                val virtualFile = directory.findChild(fileName)
                    ?: throw Caos2CobException("Failed to locate inline/attach file: '$fileName' for Caos2Cob script: '${mainFile.name}'")
                virtualFile
            }
            Caos2CobC2(
                cobTags,
                installScript = installScript,
                objectScripts = objectScripts,
                removalScript = removalScript,
                depends = dependencies,
                inline = inlineFiles
            )
        }
    }

    private fun collectLinkedFiles(
        project: Project,
        compilationResults: CompilationResults,
        variant: CaosVariant,
        directory: VirtualFile,
        cobCommands: List<Pair<CobCommand, List<String>>>
    ): List<CaosScriptFile> {
        val links = cobCommands.flatMap { (command, args) ->
            if (command != CobCommand.LINK)
                emptyList()
            else
                args
        }
        return links.map map@{ relativePath ->
            val file = directory.findChild(relativePath)
                ?: throw Caos2CobException("Failed to locate linked file: at '${directory.path + "/" + relativePath}'")
            val extension = FileNameUtils.getExtension(relativePath).toLowerCase()
            if (extension == "wav" || extension in SpriteParser.VALID_SPRITE_EXTENSIONS) {
                throw Caos2CobException("Linked file was not a CAOS file. Did you mean Attach or Inline?")
            }
            (file.getPsiFile(project) as? CaosScriptFile)?.let { caosFile ->
                if (caosFile.isCaos2Cob) {
                    compilationResults.warnings++
                    CaosNotifications.showWarning(
                        project,
                        "CAOS2Cob",
                        "CAOS2Cob directives are ignored in linked files. Only scripts were imported from '$relativePath'"
                    )
                }
                val thisVariant = caosFile.variant
                if (thisVariant != null && thisVariant != variant) {
                    throw Caos2CobException("Linked file '$relativePath' has conflicting variant. Expected ${variant.code}. Found ${thisVariant.code}")
                } else {
                    caosFile
                }
            } ?: throw Caos2CobException("Linked file is not valid CAOS file.")
        }
    }

    override fun update(event: AnActionEvent) {
        val enabled = event.files.any { file ->
            hasCaos2Cob(file)
        }
        val presentation = event.presentation
        presentation.isVisible = enabled
        presentation.text = CaosBundle.message("cob.caos2cob.compile.title")
        presentation.description = CaosBundle.message("cob.caos2cob.compile.description")
    }

    // Static Methods
    companion object {
        private val isCaos2CobRegex =
            "^[*]{2}[Cc][Aa][Oo][Ss][2][Cc][Oo][Bb]|[*][#]\\s*(C1-Name|C2-Name)".toRegex(RegexOption.IGNORE_CASE)
        var didShowAutoRemoverCobWarning: Boolean = false
        private fun hasCaos2Cob(file: VirtualFile): Boolean {
            if (file.isDirectory) {
                return file.children.any(::hasCaos2Cob)
            }
            return isCaos2CobRegex.containsMatchIn(file.contents)
        }

        private fun getCobTags(variant: CaosVariant, block: CaosScriptCaos2Block): Map<CobTag, String?> {
            return block.tags.mapNotNull { (tagString, value) ->
                CobTag.fromString(tagString, variant)?.let { tag ->
                    tag to value
                }
            }.toMap()
        }

        private fun getCobCommands(
            variant: CaosVariant,
            block: CaosScriptCaos2Block
        ): List<Pair<CobCommand, List<String>>> {
            return block.commands.mapNotNull commands@{ (commandString, args) ->
                val commandName = CobCommand.fromString(commandString, variant)
                    ?: return@commands null
                Pair(commandName, args)
            }
        }

        private val stripRscr: (script: CaosScriptScriptElement) -> String = { script: CaosScriptScriptElement ->
            stripScriptStartEnd(script, "rscr")
        }

        private val stripIscr: (script: CaosScriptScriptElement) -> String = { script: CaosScriptScriptElement ->
            stripScriptStartEnd(script, "iscr")
        }

        private fun stripScriptStartEnd(script: CaosScriptScriptElement, prefix: String): String {
            var text = script.text
            text.toLowerCase().let { asLower ->
                if (asLower.startsWith(prefix))
                    text = text.substring(4)
                if (asLower.endsWith("endm"))
                    text.substringFromEnd(0, 4)
            }
            return text.trim('\t', '\r', '\n', ' ', ',')
        }

        private fun writeCob(project: Project, directory: VirtualFile, cob: Caos2Cob, data: ByteArray): Boolean {
            val targetFile = cob.targetFile.nullIfEmpty()
                ?: throw Caos2CobException("Cannot write COB for agent: '${cob.agentName}' without target file.")
            if (!directory.isDirectory)
                throw Caos2CobException("Cannot write COB '${targetFile}'. File '${directory.name}' is not a directory")
            return try {
                directory.writeChild(targetFile, data)
                true
            } catch (e: Exception) {

                CaosNotifications.showError(
                    project,
                    "CAOS2Cob Write Error",
                    "Failed to write cob '$targetFile' from CAOS2Cob script. Error: ${e.message}"
                )
                //throw Caos2CobException("Failed to write cob '$targetFile' from CAOS2Cob script. Error: ${e.message}")
                false
            }
        }


        private fun getCaosFiles(project: Project, file: VirtualFile): List<CaosScriptFile> {
            if (file.isDirectory) {
                return file.children.flatMap { child -> getCaosFiles(project, child) }
            }
            return (file.getPsiFile(project) as? CaosScriptFile)?.let { script ->
                listOf(script)
            } ?: emptyList()
        }

        private fun getRemovalScript(
            project: Project,
            compilationResults: CompilationResults,
            mainFileName: String,
            directory: VirtualFile,
            cobData: List<Pair<CobCommand, List<String?>>>,
            removalScripts: List<CaosScriptRemovalScript>
        ): String? {

            val removalScriptPaths = cobData
                .filter { it.first == CobCommand.REMOVAL_SCRIPTS }
                .flatMap { it.second }
                .filterNotNull()

            val removalScriptStrings:List<String> = removalScriptPaths
                .flatMap { removalScriptPath ->
                    getRemovalScriptFromCobDataPath(
                        project,
                        compilationResults,
                        mainFileName,
                        directory,
                        removalScriptPath
                    )
                } + removalScripts.map { stripRscr(it) }
            if (removalScriptStrings.size > 1) {
                CaosNotifications.showWarning(
                    project,
                    "CAOS2Cob Removal Script",
                    "Combined ${removalScriptStrings.size} removal scripts into one"
                )
            }
            return removalScriptStrings.joinToString(",").nullIfEmpty()
        }

        private fun getRemovalScriptFromCobDataPath(
            project: Project,
            compilationResults: CompilationResults,
            mainFileName: String,
            directory: VirtualFile,
            removalScriptPath: String
        ): List<String> {
            // Get the RSCR child virtual file from the directory
            val child = directory.findChild(removalScriptPath)
                ?: throw Caos2CobException("Failed to find RSCR file '$removalScriptPath'")
            // Get the virtual file as a CAOS Script PSI file
            val removalScriptFile = child.getPsiFile(project)
                ?: throw Caos2CobException("Removal script '$removalScriptPath' is not a valid CAOS script file.")

            // Find all script elements inside the file for use in finding the right removal scripts
            val scripts = PsiTreeUtil.collectElementsOfType(removalScriptFile, CaosScriptScriptElement::class.java)
            if (scripts.isEmpty()) {
                compilationResults.warnings++
                CaosNotifications.showWarning(
                    project,
                    "CAOS2Cob Removal Script",
                    "Removal script '$removalScriptPath' is empty. Skipping removal script generation"
                )
                return emptyList()
            }
            val trueRemovalScript = scripts.filterIsInstance<CaosScriptRemovalScript>()
            // Check if more than one removal script is found inside the reference CAOS file.
            if (trueRemovalScript.size > 1) {
                val base =
                    trueRemovalScript.first().containingFile.name.let { if (it == mainFileName) "root file" else it }
                ++compilationResults.warnings
                // Show a warning letting the user know additional scripts are ignored.
                CaosNotifications.showWarning(
                    project,
                    "Caos2COB",
                    CaosBundle.message("cob.caos2cob.compile.too-many-removal-scripts", mainFileName, base)
                )
                return trueRemovalScript.map { stripRscr(it) }
            }
            // Create a remover script for all macros in the file.
            // This ignores install files as they should not be marked as such in a CAOS2Cob file
            val removerScriptFromMacros = scripts.filterIsInstance<CaosScriptMacro>().joinToString(",") {
                it.text.toLowerCase().let { text ->
                    val outText = if (text.endsWith("endm"))
                        text.substringFromEnd(0, 4)
                    else
                        text
                    outText.trim('\t', '\r', '\n', ' ', ',')
                }
            }.trim('\t', '\r', '\n', ' ', ',')
                .nullIfEmpty()
                ?.let { listOf(it) }

            // If Macro concatenation yields a script. Return it
            if (removerScriptFromMacros != null)
                return removerScriptFromMacros

            // Find other kinds of scripts in file
            val types = mutableListOf<String>()
            if (scripts.any { it is CaosScriptInstallScript })
                types.add("Install")
            if (scripts.any { it is CaosScriptEventScript })
                types.add("Event Scripts")

            // If types is empty, it means there is a Script Element type I have not accounted for.
            // THIS SHOULD NOT HAPPEN
            if (types.isEmpty())
                throw Caos2CobException(
                    "Parser failed to recognize internal script class [${scripts.map { it.className }.toSet()}]." +
                            " Please let plugin author know."
                )
            ++compilationResults.warnings
            // Show users a warning about how the scripts inside the RSCR file are ignored.
            CaosNotifications.showWarning(
                project,
                "CAOS2Cob RSCR",
                "${types.joinToString(" and ")} scripts are ignored in RSCR imported file."
            )
            return emptyList()
        }

        /**
         * Class to hold compilation results as it takes place in a separate thread
         * and needs to be passed by reference
         */
        private data class CompilationResults(
            val caos2CobFiles: Int,
            var success: Int = 0,
            var failures: Int = 0,
            var warnings: Int = 0
        ) {
            val index: Int get() = success + failures
        }
    }
}