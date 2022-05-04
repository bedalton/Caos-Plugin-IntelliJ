package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.inspections

import bedalton.creatures.util.FileNameUtil
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.fixes.Caos2CobMoveFileToCommandFix
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.fixes.Caos2CobRemoveFileFix
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Cob
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Path
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.AgentMessages
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Cob
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCaos2Command
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptVisitor
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CobCommand
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CobCommand.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor

class Caos2CobIncludedFileIsCorrectTypeInspection : LocalInspectionTool() {

    override fun getDisplayName(): String =
        AgentMessages.message("inspections.caos-to-compiler.file-type-check.display-name")

    override fun getGroupDisplayName(): String = CAOS2Cob
    override fun getGroupPath(): Array<String> = CAOS2Path

    override fun getShortName(): String = "Caos2CobIncludedFileIsValidType"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitCaos2Command(commandElement: CaosScriptCaos2Command) {
                super.visitCaos2Command(commandElement)
                if (!commandElement.containingCaosFile?.isCaos2Cob.orFalse())
                    return
                validateCommand(commandElement, holder)
            }

        }
    }
}


private val RESOURCE_COMMANDS = listOf(INLINE, ATTACH, DEPEND)
private val SCRIPT_COMMANDS = listOf(LINK, REMOVAL_SCRIPTS, INSTALL_SCRIPTS)
private val EMPTY_COMMANDS = emptyList<CobCommand>()

private val HAS_FILES = listOf(
    INLINE,
    ATTACH,
    DEPEND,
    LINK,
    INSTALL_SCRIPTS,
    REMOVAL_SCRIPTS
)


private fun validateCommand(commandElement: CaosScriptCaos2Command, holder: ProblemsHolder) {
    val commandType = CobCommand.fromString(commandElement.commandName)
        ?: return
    if (commandType !in HAS_FILES)
        return

    // Collect FileName and File element items
    val fileNames = commandElement.commandArgs
    val fileNameElements = commandElement.caos2ValueList

    val expectedExtensions = if (commandType.cosFiles)
        listOf("cos", "caos")
    else
        listOf("s16", "wav")

    // Find bad file references and build Element data pairs
    val badFileNameData = fileNames.indices.filter { i ->
        fileNames[i].let { FileNameUtil.getExtension(it)?.lowercase() !in expectedExtensions }
    }.mapNotNull { i ->
        fileNameElements.getOrNull(i)?.let { element -> Pair(element, fileNames[i]) }
    }

    // Add error messages to bad file references
    for ((fileNameElement, fileName) in badFileNameData) {
        val extension = FileNameUtil.getExtension(fileName)?.lowercase()
        val fixes = getFixes(fileNameElement, extension)
        val parent = fileNameElement.getParentOfType(CaosScriptCaos2Command::class.java)!!.commandName
        val error = if (commandType in SCRIPT_COMMANDS) {
            AgentMessages.message(
                "cob.caos2cob.inspections.included-file-type-valid.expects-caos-file",
                parent
            )
        } else {
            AgentMessages.message(
                "cob.caos2cob.inspections.included-file-type-valid.expects-sprite-or-wav",
                parent
            )
        }
        holder.registerProblem(fileNameElement, error, *fixes.toTypedArray())
    }
}

private fun getFixes(fileNameElement: PsiElement, extension: String?): List<LocalQuickFix> {
    val moveTo = when (extension) {
        "s16", "wav" -> RESOURCE_COMMANDS
        "cos", "caos" -> SCRIPT_COMMANDS
        else -> EMPTY_COMMANDS
    }
    return moveTo.map { command ->
        Caos2CobMoveFileToCommandFix(
            fileNameElement,
            command
        )
    } + Caos2CobRemoveFileFix(
        fileNameElement,
        AgentMessages.message("fixes.caos-to-compiler.delete-invalid-file")
    )
}