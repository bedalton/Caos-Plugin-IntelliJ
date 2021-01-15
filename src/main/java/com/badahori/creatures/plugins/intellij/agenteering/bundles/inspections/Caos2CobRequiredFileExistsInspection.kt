package com.badahori.creatures.plugins.intellij.agenteering.bundles.inspections

import com.badahori.creatures.plugins.intellij.agenteering.bundles.fixes.Caos2CobRemoveFileFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

class Caos2CobRequiredFileExistsInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = "Source file does not exist"
    override fun getGroupDisplayName(): String = CaosBundle.message("cob.caos2cob.inspections.group")
    override fun getGroupPath(): Array<String> {
        return arrayOf(CaosBundle.message("caos.intentions.family"))
    }
    override fun getShortName(): String = "Caos2CobSourceFileDoesNotExist"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitCaos2Command(o: CaosScriptCaos2Command) {
                super.visitCaos2Command(o)
                val commandType = CobCommand.fromString(o.commandName)
                    ?: return
                if (commandType != CobCommand.INLINE && commandType != CobCommand.ATTACH && commandType != CobCommand.LINK)
                    return
                val directory = (o.containingFile.virtualFile?.parent ?: o.containingFile.originalFile.virtualFile?.parent)
                    ?: return
                val fileNames = o.commandArgs
                val badFileNameIndices = fileNames.mapIndexedNotNull { i, file ->
                    if (file.isNotBlank() && directory.findChild(file) != null)
                        i
                    else
                        null
                }
                val fileNameElements = o.caos2CommentValueList
                for(index in badFileNameIndices) {
                    fileNameElements[index]?.let { fileNameElement ->
                        val error = CaosBundle.message(
                            "cob.caos2cob.inspections.required-file-exists.error",
                            fileNames[index]?:"???",
                            commandType.keyString
                        )
                        holder.registerProblem(fileNameElement, error, Caos2CobRemoveFileFix(fileNameElement))
                    }
                }
            }

            override fun visitCaos2Tag(o: CaosScriptCaos2Tag) {
                super.visitCaos2Tag(o)
                val fileName = o.value.nullIfEmpty()
                    ?: return
                val tag = CobTag.fromString(o.tagName)
                    ?: return
                val check = when (tag) {
                    CobTag.THUMBNAIL -> true
                    CobTag.RSCR -> true
                    CobTag.ATTACH -> true
                    CobTag.INLINE -> true
                    CobTag.LINK -> false
                    CobTag.DEPENDS -> false
                    else -> false
                }
                if (!check)
                    return
                val directory = (o.containingFile.virtualFile?.parent ?: o.containingFile.originalFile.virtualFile?.parent)
                    ?: return
                if (directory.findChild(fileName) != null)
                    return
                val element = o.caos2CommentValue
                    ?: return
                val error = CaosBundle.message(
                    "cob.caos2cob.inspections.required-file-exists.error",
                    fileName,
                    "${tag.keys.first()}property"
                )
                holder.registerProblem(element, error, Caos2CobRemoveFileFix(element))
            }
        }
    }
}