package com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.inspections

import bedalton.creatures.common.util.FileNameUtil
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.fixes.Caos2CobRemoveFileFix
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.caos2cob.inspections.Caos2CobRequiredFileExistsInspection.Companion.imageFileExtensions
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler.Caos2CobUtil.ARRAY_ACCESS_BEFORE_EXTENSION_REGEX
import com.badahori.creatures.plugins.intellij.agenteering.bundles.cobs.compiler.Caos2CobUtil.ARRAY_ACCESS_REGEX
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.*
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.inspections.isRawScriptNotFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.AgentMessages
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Cob
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.collectElementsOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor

class Caos2CobRequiredFileExistsInspection : LocalInspectionTool() {

    override fun getDisplayName(): String = "Source file does not exist"
    override fun getGroupDisplayName(): String = CAOS2Cob
    override fun getGroupPath(): Array<String> = CAOS2Path
    override fun getShortName(): String = "Caos2CobSourceFileDoesNotExist"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CaosScriptVisitor() {
            override fun visitCaos2Command(commandElement: CaosScriptCaos2Command) {
                super.visitCaos2Command(commandElement)
                if (!commandElement.containingCaosFile?.isCaos2Cob.orFalse()) {
                    return
                }
                annotateCommand(commandElement, holder)
            }

            override fun visitCaos2Tag(tagElement: CaosScriptCaos2Tag) {
                super.visitCaos2Tag(tagElement)
                if (!tagElement.containingCaosFile?.isCaos2Cob.orFalse())
                    return
                annotateTag(tagElement, holder)
            }
        }
    }

    companion object {
        private val mImageFileExtensions = listOf(
            "bmp",
            "png",
            "jpeg",
            "jpg",
            "gif"
        )

        internal fun imageFileExtensions(variant: CaosVariant?): List<String> {
            return if (variant == CaosVariant.C1)
                imageFilesC1
            else if (variant == CaosVariant.C2)
                imageFilesC2
            else if (variant != null) {
                imageFilesC2e
            } else {
                mImageFileExtensions
            }
        }

        private val imageFilesC1 = mImageFileExtensions + "spr"
        private val imageFilesC2 = mImageFileExtensions + "s16"
        private val imageFilesC2e = mImageFileExtensions + "c16"



    }
}


private val HAS_FILES = listOf(
    CobCommand.INLINE,
    CobCommand.ATTACH,
    CobCommand.DEPEND,
    CobCommand.LINK,
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
        AgentMessages.message("errors.files.invalid-names-format")
    )
}

private fun annotateCommand(commandElement: CaosScriptCaos2Command, holder: ProblemsHolder) {
    val commandType = CobCommand.fromString(commandElement.commandName)
        ?: return

    if (commandType !in HAS_FILES && (commandType != CobCommand.REMOVAL_SCRIPTS || isRawScriptNotFile(commandElement.commandArgs.firstOrNull().orEmpty()))) {
        return
    }

    val directory = commandElement.directory
        ?: return

    // Collect FileName and File element items
    val fileNames = commandElement.commandArgs
    val fileNameElements = commandElement.caos2ValueList

    // Find bad file references and build Element data pairs
    val badFileNameData = fileNames.indices.filter { i ->
        fileNames[i].let { it.isBlank() || directory.findChild(it) == null }
    }.mapNotNull { i ->
        fileNameElements.getOrNull(i)?.let { element -> Pair(element, getFileName(fileNames[i]) ?: fileNames[i]) }
    }

    // Add error messages to bad file references
    for ((fileNameElement, fileName) in badFileNameData) {
        val elementToRemove: PsiElement = if (commandElement.commandArgs.size > 1)
            fileNameElement
        else
            commandElement.parent
                ?: commandElement
        val error = AgentMessages.message(
            "errors.files.required-file-does-not-exist",
            fileName,
            fileNameElement.getParentOfType(CaosScriptCaos2Command::class.java)!!.commandName
        )
        val filenameFixes = getFilenameSuggestions(
            fileNameElement,
            false,
            fileName,
            includedFiles = commandElement.containingFile.collectElementsOfType(CaosScriptCaos2Command::class.java)
                .flatMap map@{
                    if (it == commandElement) {
                        return@map emptyList()
                    }
                    if (it.commandName like "Inline")
                        listOfNotNull(it.commandArgs.firstOrNull())
                    else if (it.commandName like "Attach") {
                        it.commandArgs
                    } else
                        emptyList()
                }
        )
            ?: continue
        val fixes = filenameFixes + Caos2CobRemoveFileFix(elementToRemove)
        holder.registerProblem(fileNameElement, error, *fixes.toTypedArray())
    }
}

private fun annotateTag(tagElement: CaosScriptCaos2Tag, holder: ProblemsHolder) {

    var trueFileName: String = tagElement.valueAsString.nullIfEmpty()
        ?: return

    // Find corresponding CobTag enum value from tag name
    val tag = CobTag.fromString(tagElement.tagName)
        ?: return

    val tagValueElement = tagElement.caos2Value
        ?: return

    // Create thumbnail index either null or [n] with this value's selected sprite index
    var thumbnailIndex: String? = null
    if (tag.format == CobTagFormat.SINGLE_IMAGE) {
        val index = "[^\\[]+\\[(\\d+).*".toRegex().matchEntire(trueFileName)
            ?.groupValues
            ?.getOrNull(1)
        if (index != null) {
            thumbnailIndex = "[$index]"
        }
        val thumbnailFileName = getFileName(trueFileName)
        if (thumbnailFileName == null) {
            holder.registerProblem(
                tagElement,
                AgentMessages.message("errors.files.invalid-names-format")
            )
            return
        }
        annotateSpriteReference(tagValueElement, trueFileName, holder)
        trueFileName = thumbnailFileName
    }

    // Make sure this tag needs filename validation
    val tagValueIsFilename = tag.format == CobTagFormat.FILE || tag.format == CobTagFormat.SINGLE_IMAGE
    if (!tagValueIsFilename)
        return

    val directory = tagElement.directory
        ?: return

    // Directory.findChild may resolve incorrect case file as correct
    val trueChild = VirtualFileUtil.findChildIgnoreCase(directory, ignoreExtension = false, directory = false, trueFileName)
    // If name matches exactly or is on Windows case-insensitive file system
    if (trueChild != null && (trueChild.name == FileNameUtil.getLastPathComponent(trueFileName)) || OsUtil.isWindows)
        return

    val error = AgentMessages.message(
        "errors.files.required-file-does-not-exist",
        trueFileName,
        "'${tagElement.tagName}' property"
    )
    val extension = if (tag.format == CobTagFormat.SINGLE_IMAGE) {
        imageFileExtensions(tagElement.variant)
    } else
        null

    // Find all similar files to the one given
    val similarFileNames = getSimilarFileNames(
        element = tagValueElement,
        removeExtension = false,
        baseFileName = trueFileName,
        extensions = extension,
        orb = 3
    )

    val similar: Array<LocalQuickFix> = similarFileNames?.map {
        val thisExtension = FileNameUtil.getExtension(it).nullIfEmpty()
        val path = if (thumbnailIndex != null) {
            if (thisExtension == null)
                it + thumbnailIndex
            else
                it.substringFromEnd(0, 4) + thumbnailIndex + '.' + thisExtension
        } else {
            it
        }
        CaosScriptReplaceElementFix(
            tagValueElement,
            "\"$path\"",
            "Replace with file '$path'"
        )
    }
        .nullIfEmpty()
        ?.toTypedArray()
        ?: arrayOf (Caos2CobRemoveFileFix(tagElement.parent, "Remove unresolved ${tagElement.tagName} file reference"))

    holder.registerProblem(tagValueElement, error, *similar)
}