package com.badahori.creatures.plugins.intellij.agenteering.bundles.actions

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler.Caos2Cob
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler.Caos2CobC1
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler.Caos2CobC2
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler.Caos2CobException
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobFileData
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.decompiler.CobToDataObjectDecompiler
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
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.writeChild
import icons.CaosScriptIcons
import java.nio.ByteBuffer

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

        // Run compile phase in background
        // Requires read access though, so will have to move back onto ui thread I think
        runBackgroundableTask("Compiling $numFiles Caos2Cob files", e.project) { progressIndicator ->

            var success = 0
            var failure = 0
            files.forEachIndexed { i, file ->
                // Run on pooled event dispatch thread
                ApplicationManager.getApplication().invokeLater {

                    // Ensure in read action
                    runWriteAction action@{
                        // Try to get Caos2Cob manifest from CAOS file.
                        val compilerData:Caos2Cob = try {
                            getCobManifest(project, file)?.apply {
                                LOGGER.info("Got CAOS2Cob manifest data: $this")
                            }
                                ?: throw Caos2CobException("COB compiler manifest could not be built")
                        } catch(e:Caos2CobException) {
                            // Failed to convert CAOS script file to COB data struct
                            CaosNotifications.showError(
                                project,
                                "CAOS2Cob Failure",
                                "Failed to compile ${file.name} with error: ${e.message}"
                            )
                            failure++
                            return@action
                        } catch(e:Exception) {
                            LOGGER.severe(e.message)
                            e.printStackTrace()
                            return@action
                        }
                        // Update progress indicator
                        val type = if (compilerData is Caos2CobC1) "C1" else "C2"
                        progressIndicator.fraction = i / numFiles.toDouble()
                        progressIndicator.text = "Compile $i/$numFiles COBS"
                        progressIndicator.text2 = "Compiling $type COB: ${compilerData.targetFile}"
                        val parent = (file.virtualFile ?: file.originalFile.virtualFile).parent
                        if (parent == null) {
                            CaosNotifications.showError(
                                project,
                                "COAS2Cob Plugin Error",
                                "Failed to find parent directory of CAOS2Cob file '${file.name}'")
                            failure++
                            return@action
                        } else {
                            LOGGER.info("Parent file for COB is not null")
                        }
                        val dataOut = compile(project, file, compilerData)
                        if (dataOut == null) {
                            failure++
                            LOGGER.severe("Failed to compile data")
                            return@action
                        } else {
                            LOGGER.severe("Compiled COB data")
                        }
                        if (!writeCob(project, parent, compilerData, dataOut)) {
                            failure++
                            LOGGER.severe("Failed write COB data")
                            return@action
                        } else {
                            LOGGER.info("Wrote COB data")
                        }
                        if (compilerData is Caos2CobC1) {
                            // If C1 COB, create remover COB
                            compilerData.removerCob?.let { removerCob ->
                                val error = CaosBundle.message(
                                    "cob.caos2cob.compile.auto-remover-name",
                                    removerCob.targetFile
                                )
                                if (compilerData.removerName.nullIfEmpty() == null) {
                                    CaosNotifications.showWarning(
                                        project,
                                        "CAOS2Cob Removal Script Warning",
                                        error
                                    )
                                }
                                val removerData = compile(project, file, removerCob)
                                if (removerData == null) {
                                    failure++
                                    LOGGER.severe("Failed to compile remover COB data")
                                    return@action
                                } else {
                                    LOGGER.info("Compiled remover COB data")
                                }
                                if (!writeCob(project, parent,removerCob, removerData)) {
                                    failure++
                                    LOGGER.severe("Failed to write remover COB data")
                                    return@action
                                } else {
                                    LOGGER.info("Wrote remover COB data")
                                }
                            }
                        }
                        success++
                    }
                }
            }
            val message = when {
                failure == 0 && success > 0 -> "Compiled all $numFiles CAOS2Cob cobs successfully"
                failure > 0 && success == 0 -> "Failed to compile any of the $numFiles CAOS2Cob files successfully"
                failure == 0 && success == 0 -> "Compiler failed to run without error"
                else -> "Failed to compile $failure out of $numFiles CAOS2Cob files"
            }
            CaosNotifications.showInfo(
                project,
                "CAOS2Cob Result",
                message
            )
        }
    }

    private fun compile(project: Project, file:CaosScriptFile, cob: Caos2Cob) : ByteArray? {
        return try {
            /*val bytes = cob.compile()
            return if (validateBytes(cob, bytes))
                bytes
            else
                null*/
            cob.compile()
        } catch (e: Caos2CobException) {
            CaosNotifications.showError(
                project,
                "CAOS2Cob Failure",
                "Failed to compile ${file.name} with error: ${e.message}"
            )
            null
        } catch(e:Exception) {
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

    private fun validateBytes(cob:Caos2Cob, bytes:ByteArray) : Boolean {
        val output = try {
            CobToDataObjectDecompiler.decompile(ByteBuffer.wrap(bytes))
        } catch(e:Exception) {
            throw Caos2CobException("Compilation result was invalid. ${e.message}")
        }
        if (cob is Caos2CobC1) {
            assert (output is CobFileData.C1CobData)  { "Output COB data does not match input" }
            val cobBlock = (output as CobFileData.C1CobData).cobBlock
            assert (cob.agentName == cobBlock.name) { "Output COB name '${cobBlock.name}' does not match input '${cob.agentName}'"}
            assert ( cob.quantityAvailable == cobBlock.quantityAvailable) { "Output COB name '${cobBlock.quantityAvailable}' does not match input '${cob.quantityAvailable}'"}
            assert ( cob.quantityUsed == cobBlock.quantityUsed) { "Output COB name '${cobBlock.quantityUsed}' does not match input '${cob.quantityUsed}'"}
        }
        return true
    }

    private fun getCobManifest(project: Project, mainFile: CaosScriptFile): Caos2Cob? {
        // Get parent directory for all read and write operations
        val directory = mainFile.virtualFile.parent

        // Get variant
        val variant = mainFile.caos2CobVariant
            ?: throw Caos2CobException("Failed to determine CAOS2Cob variant")

        // Convert raw tags to enum map
        val block = PsiTreeUtil.getChildOfType(mainFile, CaosScriptCaos2Block::class.java)
            ?: throw Caos2CobException("No CAOS2Cob directive block found")

        // Parse Caos2Cpb directives
        val cobTags = getCobTags(variant, block)
        val cobCommands = getCobCommands(variant, block)

        // Take find all scripts in this file and all linked files.
        val linkedFiles = mainFile + collectLinkedFiles(project,variant, directory, cobCommands)
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
        val installScripts:List<String> = scripts.filterIsInstance<CaosScriptInstallScript>().map(stripIscr)

        // Get removal script
        val removalScripts = scripts.filterIsInstance<CaosScriptRemovalScript>()
        val removalScript = getRemovalScript(project, mainFile.name, directory, cobTags, removalScripts)

        // Format C1/C2 cobs respectively
        return if (variant == C1) {
            Caos2CobC1(
                cobData = cobTags.map { (key, value) ->
                    if (key == CobTag.THUMBNAIL && value != null) {
                        key to directory.findChild(value)?.path
                    } else {
                        key to value
                    }
                }.toMap(),
                objectScripts =
                objectScripts,
                installScripts = installScripts,
                removalScript = removalScript
            )
        } else {
            val installScript = installScripts.joinToString(",").let { script ->
                "$script,endm"
            }
            val attachments:Set<String> = cobCommands.filter { it.first == CobCommand.ATTACH }.flatMap { it.second }.toSet()
            val dependencies:Set<String> = attachments + cobCommands
                .filter { it.first == CobCommand.DEPEND }
                .flatMap { it.second }
                .toSet()

            val inlineFileNames:Set<String> = attachments +  cobCommands
                .filter { it.first == CobCommand.INLINE }
                .flatMap { it.second }
                .toSet()

            val inlineFiles = inlineFileNames.map map@{ fileName ->
                val virtualFile = directory.findChild(fileName)
                    ?: throw Caos2CobException("Failed to locate inline/attach file: '$fileName' for Caos2Cob script: '${mainFile.name}'")
                virtualFile
            }
            Caos2CobC2(cobTags, installScript = installScript, objectScripts = objectScripts, removalScript = removalScript, depends = dependencies, inline = inlineFiles)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun collectLinkedFiles(
        project: Project,
        variant: CaosVariant,
        directory: VirtualFile,
        cobCommands: List<Pair<CobCommand, List<String>>>,
        depth:Int = 0
    ): List<CaosScriptFile> {
        val links = cobCommands.flatMap { (command, args) ->
            if (command != CobCommand.LINK)
                emptyList()
            else
                args
        }
        /*return links.flatMap map@{ relativePath ->
            val file = directory.findChild(relativePath)
                ?: return@map emptyList()
            val caosFile = (file.getPsiFile(project) as? CaosScriptFile)
                ?: return@map emptyList()
            val out = mutableListOf(caosFile)
            if (caosFile.isCaos2Cob) {
                val thisFileVariant = caosFile.caos2CobVariant
                if (thisFileVariant != variant) {
                    val error = "Linked file '${file.name}' has conflicting CAOS variant at a link depth of $depth. " +
                            "Expected variant ${variant.code}. Found: ${thisFileVariant.code}"
                    throw Caos2CobException(file.name, error)
                }
                val fileCommands = caosFile.getChildOfType(CaosScriptCaos2Block::class.java)?.let { block ->
                    getCobCommands(variant, block).nullIfEmpty()
                } ?: return@map listOf(caosFile)
                out.addAll(collectLinkedFiles(project, variant, directory, fileCommands, depth + 1))
            }
            out
        }*/
        return links.mapNotNull map@{ relativePath ->
            val file = directory.findChild(relativePath)
                ?: return@map null
            (file.getPsiFile(project) as? CaosScriptFile)?.apply {
                if (isCaos2Cob)
                    CaosNotifications.showWarning(project,
                        "CAOS2Cob",
                        "CAOS2Cob directives are ignored in Linked files. Only scripts imported from $name")
            }
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
        private val isCaos2CobRegex = "^[*]{2}[Cc][Aa][Oo][Ss][2][Cc][Oo][Bb]".toRegex()

        private fun hasCaos2Cob(file:VirtualFile) : Boolean {
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

        private val stripRscr:(script:CaosScriptScriptElement)-> String  = { script:CaosScriptScriptElement ->
            stripScriptStartEnd(script, "rscr")
        }

        private val stripIscr:(script:CaosScriptScriptElement)-> String  = { script:CaosScriptScriptElement ->
            stripScriptStartEnd(script, "iscr")
        }

        private fun stripScriptStartEnd(script:CaosScriptScriptElement, prefix:String) : String {
            var text = script.text
            text.toLowerCase().let { asLower ->
                if (asLower.startsWith(prefix))
                    text = text.substring(4)
                if (asLower.endsWith("endm"))
                    text.substringFromEnd(0, 4)
            }
            return text.trim('\t', '\r', '\n', ' ', ',')
        }

        private fun writeCob(project:Project, directory: VirtualFile, cob:Caos2Cob, data:ByteArray) : Boolean {
            val targetFile = cob.targetFile.nullIfEmpty()
                ?: throw Caos2CobException("Cannot write COB for agent: '${cob.agentName}' without target file.")
            if (!directory.isDirectory)
                throw Caos2CobException("Cannot write COB '${targetFile}'. File '${directory.name}' is not a directory")
            try {
                directory.writeChild(targetFile, data)
                return true
            } catch(e:Exception) {
                CaosNotifications.showError(
                    project,
                    "CAOS2Cob Write Error",
                    "Failed to write cob '$targetFile' from CAOS2Cob script. Error: ${e.message}"
                )
            }
            return false
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
            project:Project,
            mainFileName: String,
            directory: VirtualFile,
            cobData: Map<CobTag, String?>,
            removalScripts: List<CaosScriptRemovalScript>
        ): String? {

            cobData[CobTag.RSCR]?.let { removalScriptPath ->
                return getRemovalScriptFromCobDataPath(project, mainFileName, directory, removalScriptPath)?.let {
                    it.trim('\t', '\r', '\n', ' ', ',')
                        .nullIfEmpty()
                        ?.let {
                            "$it,endm"
                        }
                }
            }
            return when(removalScripts.size) {
                0 -> null
                1 -> {
                    stripRscr(removalScripts.first())
                        .nullIfEmpty()
                        ?.let {
                            "$it,endm"
                        }
                }
                else -> {
                    val base = removalScripts.first().containingFile.name.let { if (it == mainFileName) "root file" else it}
                    // Show a warning letting the user know additional scripts are ignored.
                    CaosNotifications.showWarning(
                        project,
                        "Caos2COB",
                        CaosBundle.message("cob.caos2cob.compile.too-many-removal-scripts", mainFileName, base)
                    )
                    null
                }
            }
        }

        private fun getRemovalScriptFromCobDataPath(
            project:Project,
            mainFileName:String,
            directory: VirtualFile,
            removalScriptPath:String
        ) : String? {
            // Get the RSCR child virtual file from the directory
            val child = directory.findChild(removalScriptPath)
                ?: throw Caos2CobException("Failed to find RSCR file '$removalScriptPath'")
            // Get the virtual file as a CAOS Script PSI file
            val removalScriptFile = child.getPsiFile(project)
                ?: throw Caos2CobException("Removal script '$removalScriptPath' is not a valid CAOS script file.")

            // Find all script elements inside the file for use in finding the right removal scripts
            val scripts = PsiTreeUtil.collectElementsOfType(removalScriptFile, CaosScriptScriptElement::class.java)
            if (scripts.isEmpty()) {
                CaosNotifications.showWarning(
                    project,
                    "CAOS2Cob Removal Script",
                    "Removal script '$removalScriptPath' is empty. Skipping removal script generation"
                )
                return null
            }
            val trueRemovalScript = scripts.filterIsInstance<CaosScriptRemovalScript>()
            // Check if more than one removal script is found inside the reference CAOS file.
            if (trueRemovalScript.size > 1) {
                val base = trueRemovalScript.first().containingFile.name.let { if (it == mainFileName) "root file" else it}
                // Show a warning letting the user know additional scripts are ignored.
                CaosNotifications.showWarning(
                    project,
                    "Caos2COB",
                    CaosBundle.message("cob.caos2cob.compile.too-many-removal-scripts", mainFileName, base)
                )
                return stripRscr(trueRemovalScript.first())
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
            }.trim('\t', '\r', '\n', ' ', ',').nullIfEmpty()

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
                    "Parser failed to recognize internal script class [${scripts.map{ it.className}.toSet() }]." +
                            " Please let plugin author know."
                )
            // Show users a warning about how the scripts inside the RSCR file are ignored.
            CaosNotifications.showWarning(project, "CAOS2Cob RSCR","${types.joinToString(" and ")} scripts are ignored in RSCR imported file.")
            return null
        }
    }
}