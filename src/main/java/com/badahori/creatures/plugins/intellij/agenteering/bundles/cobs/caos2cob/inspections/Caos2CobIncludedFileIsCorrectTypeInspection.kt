package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.inspections

import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler.Caos2CobUtil.ARRAY_ACCESS_BEFORE_EXTENSION_REGEX
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler.Caos2CobUtil.ARRAY_ACCESS_REGEX
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.fixes.Caos2CobMoveFileToCommandFix
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.fixes.Caos2CobRemoveFileFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CobCommand.*
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor

class Caos2CobIncludedFileIsCorrectTypeInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = "Included file is valid type"
    override fun getGroupDisplayName(): String = CaosBundle.message("cob.caos2cob.inspections.group")
    override fun getGroupPath(): Array<String> {
        return arrayOf(CaosBundle.message("caos.intentions.family"))
    }

    override fun getShortName(): String = "Caos2CobIncludedFileIsValidType"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitCaos2Command(commandElement: CaosScriptCaos2Command) {
                super.visitCaos2Command(commandElement)
                val commandType = CobCommand.fromString(commandElement.commandName)
                    ?: return
                if (commandType !in HAS_FILES)
                    return

                // Collect FileName and File element items
                val fileNames = commandElement.commandArgs
                val fileNameElements = commandElement.caos2CommentValueList

                val expectedExtensions = when (commandType) {
                    LINK, INSTALL_SCRIPTS, REMOVAL_SCRIPTS -> listOf("cos", "caos")
                    else -> listOf("s16", "wav")
                }

                // Find bad file references and build Element data pairs
                val badFileNameData = fileNames.indices.filter { i ->
                    fileNames[i].let { FileNameUtils.getExtension(it) likeNone expectedExtensions }
                }.mapNotNull { i ->
                    fileNameElements.getOrNull(i)?.let { element -> Pair(element, fileNames[i]) }
                }

                // Add error messages to bad file references
                for ((fileNameElement, fileName) in badFileNameData) {
                    val fixes = mutableListOf<LocalQuickFix>(Caos2CobRemoveFileFix(fileNameElement, "Remove invalid file"))
                    val extension = FileNameUtils.getExtension(fileName)?.toLowerCase()
                    val error = if (commandType == LINK) {
                        if (extension == "s16" || extension == "wav") {
                            fixes.add(
                                Caos2CobMoveFileToCommandFix(
                                    fileNameElement,
                                    ATTACH
                                )
                            )
                            fixes.add(
                                Caos2CobMoveFileToCommandFix(
                                    fileNameElement,
                                    INLINE
                                )
                            )
                        }
                        CaosBundle.message(
                            "cob.caos2cob.inspections.included-file-type-valid.expects-caos-file",
                            commandType.keyString
                        )
                    } else {
                        if (extension == "cos" || extension == "caos") {
                            fixes.add(
                                Caos2CobMoveFileToCommandFix(
                                    fileNameElement,
                                    LINK
                                )
                            )
                        }
                        CaosBundle.message(
                            "cob.caos2cob.inspections.included-file-type-valid.expects-sprite-or-wav",
                            commandType.keyString
                        )
                    }
                    holder.registerProblem(fileNameElement, error, *fixes.toTypedArray())
                }
            }

            override fun visitCaos2Tag(tagElement: CaosScriptCaos2Tag) {
                super.visitCaos2Tag(tagElement)
                val tag = CobTag.fromString(tagElement.tagName)
                    ?: return
                val tagValueElement = tagElement.caos2CommentValue
                    ?: return
                if (tag != CobTag.THUMBNAIL)
                    return
                val fileName = getFileName(tagValueElement.value)
                    ?: tagElement.value
                    ?: return
                val extension = FileNameUtils
                    .getExtension(fileName)
                    ?.toLowerCase()
                val check = when (tag) {
                    CobTag.THUMBNAIL -> extension in listOf("gif", "jpeg", "jpg", "png", "spr", "s16", "c16", "bmp")
                    else -> true
                }
                if (check)
                    return
                val fixes = mutableListOf<LocalQuickFix>(Caos2CobRemoveFileFix(tagValueElement))
                val error = if (tag == CobTag.THUMBNAIL) {
                    CaosBundle.message(
                        "cob.caos2cob.inspections.included-file-type-valid.expects-image",
                        "${tag.keys.first()} property"
                    )
                } else {
                    CaosBundle.message(
                        "cob.caos2cob.inspections.included-file-type-valid.expects-caos-file",
                        "${tag.keys.first()} property"
                    )
                }
                holder.registerProblem(tagValueElement, error, *fixes.toTypedArray())
            }
        }
    }

    companion object {

        private val HAS_FILES = listOf(
            INLINE,
            ATTACH,
            LINK
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
    }
}

private val PsiElement.directory: VirtualFile?
    get() {
        return (containingFile.virtualFile?.parent ?: containingFile.originalFile.virtualFile?.parent)
    }