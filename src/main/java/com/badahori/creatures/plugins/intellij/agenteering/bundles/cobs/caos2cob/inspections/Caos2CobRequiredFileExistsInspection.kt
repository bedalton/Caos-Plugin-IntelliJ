package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.inspections

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler.Caos2CobUtil.ARRAY_ACCESS_BEFORE_EXTENSION_REGEX
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler.Caos2CobUtil.ARRAY_ACCESS_REGEX
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.fixes.Caos2CobRemoveFileFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Cob
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
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
            override fun visitCaos2Command(commandElement: CaosScriptCaos2Command) {
                super.visitCaos2Command(commandElement)

                if (!commandElement.containingCaosFile?.isCaos2Cob.orFalse())
                    return
                val commandType = CobCommand.fromString(commandElement.commandName)
                    ?: return
                if (commandType !in HAS_FILES)
                    return
                val directory = commandElement.directory
                    ?: return

                // Collect FileName and File element items
                val fileNames = commandElement.commandArgs
                val fileNameElements = commandElement.caos2CommentValueList

                // Find bad file references and build Element data pairs
                val badFileNameData = fileNames.indices.filter { i ->
                    fileNames[i].let { it.isBlank() || directory.findChild(it) == null }
                }.mapNotNull { i ->
                    fileNameElements.getOrNull(i)?.let { element -> Pair(element, fileNames[i]) }
                }

                // Add error messages to bad file references
                for ((fileNameElement, fileName) in badFileNameData) {
                    val error = CaosBundle.message(
                        "cob.caos2cob.inspections.required-file-exists.error",
                        fileName,
                        commandType.keyString
                    )
                    holder.registerProblem(fileNameElement, error, Caos2CobRemoveFileFix(fileNameElement))
                }
            }

            override fun visitCaos2Tag(tagElement: CaosScriptCaos2Tag) {
                super.visitCaos2Tag(tagElement)
                var fileNameRaw:String = tagElement.value.nullIfEmpty()
                    ?: return
                val tag = CobTag.fromString(tagElement.tagName)
                    ?: return
                val tagValueElement = tagElement.caos2CommentValue
                    ?: return
                if (tag == CobTag.THUMBNAIL) {
                    val thumbnailFileName = getFileName(fileNameRaw)
                    if (thumbnailFileName == null) {
                        holder.registerProblem(
                            tagElement,
                            CaosBundle.message("cob.caos2cob.inspections.file-name-inspections.invalid-format")
                        )
                        return
                    }
                    annotateSpriteReference(tagValueElement, fileNameRaw, holder)
                    fileNameRaw = thumbnailFileName
                }
                val check = when (tag) {
                    CobTag.THUMBNAIL -> true
                    else -> false
                }
                if (!check)
                    return
                val directory = tagElement.directory
                    ?: return
                if (directory.findChild(fileNameRaw) != null)
                    return
                val error = CaosBundle.message(
                    "cob.caos2cob.inspections.required-file-exists.error",
                    fileNameRaw,
                    "${tag.keys.first()} property"
                )
                holder.registerProblem(tagValueElement, error, Caos2CobRemoveFileFix(tagValueElement))
            }
        }
    }

    companion object {


        private val HAS_FILES = listOf(
            CobCommand.INLINE,
            CobCommand.ATTACH,
            CobCommand.LINK
        )

        private fun getFileName(path: String): String? {
            if (!path.contains('['))
                return path
            ARRAY_ACCESS_REGEX.matchEntire(path)?.groupValues?.let { groupValues ->
                return "${groupValues[1]}.${groupValues[2]}"
            }
            ARRAY_ACCESS_BEFORE_EXTENSION_REGEX.matchEntire(path)?.groupValues?.let { groupValues ->
                return "${groupValues[1]}.${groupValues[3]}"
            }
            return null
        }

        private fun annotateSpriteReference(element: PsiElement, path: String, problemsHolder: ProblemsHolder) {
            // Does not have array syntax
            if (!path.contains('['))
                return
            if (ARRAY_ACCESS_REGEX.matches(path))
                return
            if (ARRAY_ACCESS_BEFORE_EXTENSION_REGEX.matches(path))
                return
            problemsHolder.registerProblem(
                element,
                CaosBundle.message("cob.caos2cob.inspections.file-name-inspections.invalid-format")
            )
        }

    }
}

private val PsiElement.directory: VirtualFile?
    get() {
        return (containingFile.virtualFile?.parent ?: containingFile.originalFile.virtualFile?.parent)
    }