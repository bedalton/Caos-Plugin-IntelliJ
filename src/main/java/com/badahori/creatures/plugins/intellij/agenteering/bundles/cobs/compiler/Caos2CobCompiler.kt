package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler

import bedalton.creatures.util.FileNameUtil
import bedalton.creatures.util.className
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptCollapseNewLineIntentionAction
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CollapseChar
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.injector.CaosNotifications
import com.badahori.creatures.plugins.intellij.agenteering.sprites.sprite.SpriteParser
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.io.write
import java.nio.file.Paths

object Caos2CobCompiler {

    fun compile(
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
        progressIndicator.isIndeterminate = false
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
        /*
        *** Originally this Check alerted the user to the fact that a remover cob was automatically generated
        * But I think now it should be assumed that the remover cob should be generated unless set to an empty string

        if (false && !didShowAutoRemoverCobWarning && compilerData.removerName.nullIfEmpty() == null) {
            val error = AgentMessages.message(
                "cob.caos2cob.compile.auto-remover-name",
                removerCob.targetFile
            )
            didShowAutoRemoverCobWarning = true
            CaosNotifications.showWarning(
                project,
                "CAOS2Cob Removal Script Warning",
                error
            )
        }*/
        if (FileNameUtil.getFileNameWithoutExtension(removerCob.targetFile).let { it.isNotNullOrBlank() && it notLike "false" }) {
            val removerData = compile(project, file, removerCob)
            if (removerData == null) {
                ++compilationResult.failures
                LOGGER.severe("Failed to compile remover COB data")
                return false
            }
            return if (!writeCob(project, parent, removerCob, removerData)) {
                ++compilationResult.failures
                LOGGER.severe("Failed to write remover COB data")
                false
            } else {
                true
            }
        } else
            return true
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

    /**
     * Gets the COB manifest from a CAOS2Cob file
     * @see Caos2Cob
     * @see Caos2CobC1
     * @see Caos2CobC2
     */
    private fun getCobManifest(
        project: Project,
        compilationResults: CompilationResults,
        mainFile: CaosScriptFile
    ): Caos2Cob {
        // Get parent directory for all read and write operations
        val directory = mainFile.virtualFile!!.parent

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
                key to directory.findFileByRelativePath(parts.first)?.let {
                    it.parent.path + "/" + value
                }
            } else {
                key to value
            }
        }.toMap().toMutableMap()
        val cobCommands = getCobCommands(variant, block)
        cobCommands.firstOrNull { it.first == CobCommand.COBFILE }
            ?.second
            ?.nullIfEmpty()
            ?.let { cobFileNames ->
                if (cobTags.containsKey(CobTag.COB_NAME) || cobFileNames.size != 1) {
                    throw Caos2CobException("Conflicting cob file name tags/commands. Only one tag or command for cob file is allowed")
                }
                cobTags[CobTag.COB_NAME] = cobFileNames.firstOrNull()
        }
        val agentNameFromTags = cobTags[CobTag.AGENT_NAME]
        if (block.agentBlockNames.map { it.first }.size > 1) {
            throw Caos2CobException("CAOS2Cob allows only 1 Agent Name tag. Found ${block.agentBlockNames}")
        }
        val agentNameFromCommand = block.agentBlockNames.firstOrNull { it.first == variant.code }?.second
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
                scripts.addAll(getFileScripts(fileIn))
            }
        }

        // Find Object scripts
        val objectScripts = scripts.filterIsInstance<CaosScriptEventScript>().map { it.text }

        val missingInstallFile = mutableListOf<String>()
        var isNotCaosFile = false
        val installScripts:List<String> = cobCommands
            .filter { it.first == CobCommand.INSTALL_SCRIPTS }
            .flatMap { (_, files) ->
                files.flatMap files@{ fileName ->
                    val file = directory.findChild(fileName)?.getPsiFile(project)
                    if (file == null) {
                        missingInstallFile.add(fileName)
                        return@files emptyList()
                    }
                    if (file !is CaosScriptFile) {
                        isNotCaosFile = true
                        return@files emptyList()
                    }
                    getFileScripts(file)
                        .filter { it is CaosScriptMacro || it is CaosScriptInstallScript }
                        .map {
                            stripIscr(it)
                        }
                }
            } + scripts.filterIsInstance<CaosScriptInstallScript>().map(stripIscr)

        if (isNotCaosFile) {
            throw Caos2CobException("Linked install script is not a CAOS file")
        }
        if (missingInstallFile.isNotEmpty()) {
            throw Caos2CobException("Failed to located ISCR files: [${missingInstallFile.joinToString()}")
        }

        if (scripts.any {
                // Ensure no scripts are macros, and if they are, that they are not blocks of comments only
                it is CaosScriptMacro && it.text.split('\n')
                    .filterNot { line -> line.startsWith("*") }
                    .isNotEmpty()
            }) {
            compilationResults.warnings++
            CaosNotifications
                .showWarning(project, "CAOS2Cob", "Body scripts in CAOS2Cob files are ignored")
        }

        // Get removal script
        val removalScripts = scripts.filterIsInstance<CaosScriptRemovalScript>()
        val removalScript =
            getRemovalScript(
                project,
                compilationResults,
                mainFile.name,
                directory,
                cobCommands,
                removalScripts
            )


        // Format C1/C2 cobs respectively
        return if (variant == CaosVariant.C1) {
            Caos2CobC1(
                cobData = cobTags,
                objectScripts = objectScripts,
                installScripts = installScripts,
                removalScript = removalScript
            )
        } else {
            val installScript = installScripts.joinToString(",")

            // Attached files are added to both the "depends" and the "inline" files list
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


    /**
     * Pulls all event scripts from all linked files
     * Does not pull in ISCR or RSCR scripts.
     * Issues a warning if LINKed scripts contain ISCR or RSCR script blocks
     */
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
            val extension = FileNameUtil.getExtension(relativePath)?.lowercase()
            if (extension == "wav" || extension in SpriteParser.VALID_SPRITE_EXTENSIONS) {
                throw Caos2CobException("Linked file was not a CAOS file. Did you mean Attach or Inline?")
            }
            (file.getPsiFile(project) as? CaosScriptFile)?.let { caosFile ->
                if (caosFile.hasCaos2Tags) {
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

    /**
     * Gets all tags within the CAOS2Cob block
     */
    private fun getCobTags(variant: CaosVariant, block: CaosScriptCaos2Block): Map<CobTag, String?> {
        return block.tags.mapNotNull { (tagString, value) ->
            CobTag.fromString(tagString, variant)?.let { tag ->
                tag to value
            }
        }.toMap()
    }

    /**
     * Gets all cob commands inside the CAOS2Cob block
     */
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

    /**
     * Convenience method to remove RSCR and ENDM from a script
     */
    private val stripRscr: (script: CaosScriptScriptElement) -> String = { script: CaosScriptScriptElement ->
        stripScriptStartEnd(script, "rscr")
    }

    /**
     * Convenience method to remove ISCR and ENDM from a script
     */
    private val stripIscr: (script: CaosScriptScriptElement) -> String = { script: CaosScriptScriptElement ->
        stripScriptStartEnd(script, "iscr")
    }

    /**
     * Strips a given string from the script and removes ENDM
     * Used to remove ISCR and RSCR, as these are invalid in C1 and C2
     */
    private fun stripScriptStartEnd(script: CaosScriptScriptElement, prefix: String): String {
        var text = script.text
        text.lowercase().let { asLower ->
            if (asLower.startsWith(prefix))
                text = text.substring(4)
            if (asLower.endsWith("endm"))
                text.substringFromEnd(0, 4)
        }
        return text.trim('\t', '\r', '\n', ' ', ',')
    }

    /**
     * Actually writes the COB to a file
     */
    private fun writeCob(project: Project, directory: VirtualFile, cob: Caos2Cob, data: ByteArray): Boolean {
        var targetFile = cob.targetFile.nullIfEmpty()
            ?: throw Caos2CobException("Cannot write COB for agent: '${cob.agentName}' without target file.")
        if (FileNameUtil.getExtension(targetFile).isNullOrBlank()) {
            targetFile += ".cob"
        }
        if (!directory.isDirectory)
            throw Caos2CobException("Cannot write COB '${targetFile}'. File '${directory.name}' is not a directory")
        return try {
            val targetIoFile = Paths.get(VfsUtil.virtualToIoFile(directory).path, targetFile)
            targetIoFile.write(data)
            try {
                VfsUtil.findFile(targetIoFile, true)?.let { thisFile ->
                    VfsUtil.markDirtyAndRefresh(true, false, true, thisFile)
                }
            } catch (_: Exception) {
            }
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


    /**
     * Gets the removal scripts by combining all of them if needed
     */
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

        val removalScriptStrings: List<String> = removalScriptPaths
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

    /**
     * Reads in Removal script from Caos2Cob directive
     */
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
        val removalScriptFile = child.getPsiFile(project) as? CaosScriptFile
            ?: throw Caos2CobException("Removal script '$removalScriptPath' is not a valid CAOS script file.")

        // Find all script elements inside the file for use in finding the right removal scripts
        val scripts = getFileScripts(removalScriptFile)
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
                AgentMessages.message("cob.caos2cob.compile.too-many-removal-scripts", mainFileName, base)
            )
            return trueRemovalScript.map { stripRscr(it) }
        }
        // Create a remover script for all macros in the file.
        // This ignores install files as they should not be marked as such in a CAOS2Cob file
        val removerScriptFromMacros = scripts.filterIsInstance<CaosScriptMacro>().joinToString(",") {
            it.text.lowercase().let { text ->
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
     * Flattens a file and gets its scripts
     */
    private fun getFileScripts(fileIn: CaosScriptFile) : List<CaosScriptScriptElement> {
        val scripts = mutableListOf<CaosScriptScriptElement>()
        val pointer = SmartPointerManager.createPointer(fileIn)
        WriteCommandAction.writeCommandAction(fileIn.project)
            .shouldRecordActionForActiveDocument(false)
            .withGroupId("CAOS2Cob")
            .withName("Collapse CAOS2Cob script with commas")
            .withUndoConfirmationPolicy(UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION)
            .run<Exception> {
                val file = pointer.element
                    ?: throw Exception("Failed to get file in runner for collapse")
                val collapsed = CaosScriptCollapseNewLineIntentionAction.collapseLinesInCopy(file, CollapseChar.COMMA)
                    ?: throw Exception("Failed to collapse script")
                val scriptsInFile = PsiTreeUtil.collectElementsOfType(collapsed, CaosScriptScriptElement::class.java)
                scripts.addAll(scriptsInFile)
            }
        return scripts
    }

    /**
     * Class to hold compilation results as it takes place in a separate thread
     * and needs to be passed by reference
     */
    data class CompilationResults(
        val caos2CobFiles: Int,
        var success: Int = 0,
        var failures: Int = 0,
        var warnings: Int = 0
    ) {
        val index: Int get() = success + failures
    }
}



