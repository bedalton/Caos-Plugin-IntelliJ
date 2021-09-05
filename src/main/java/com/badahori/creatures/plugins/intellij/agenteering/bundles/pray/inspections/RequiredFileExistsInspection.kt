package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.inspections

import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Cob
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.CAOS2Path
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.PRAY
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.actions.AddIgnoredModuleLevelFile
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.actions.AddIgnoredProjectLevelFile
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.getFilenameSuggestions
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support.DefaultGameFiles
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.support.PrayTags
import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptReplaceElementFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.AgentMessages
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Pray
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import kotlin.math.min

class Caos2PrayRequiredFileExistsInspection : LocalInspectionTool() {

    override fun getGroupDisplayName(): String = CAOS2Cob
    override fun getGroupPath(): Array<String> = CAOS2Path
    override fun getDisplayName(): String = AgentMessages.message("inspections.caos-to-compiler.file-exists.display-name", CAOS2Cob)
    override fun getShortName(): String = "Caos2PraySourceFileDoesNotExist"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {

        return object : CaosScriptVisitor() {

            override fun visitCaos2Command(commandElement: CaosScriptCaos2Command) {
                super.visitCaos2Command(commandElement)
                if (!commandElement.containingCaosFile?.isCaos2Pray.orFalse())
                    return
                val tagRaw = commandElement.commandName.trim()
                val tag = PrayTags.normalize(tagRaw)
                    ?: tagRaw

                PrayCommand.fromString(tagRaw)?.let {
                    annotateCaos2PrayCommand(it, commandElement.caos2ValueList, holder)
                    return
                }

                if (!requiresFile(tag))
                    return
                for (tagValue in commandElement.caos2ValueList) {
                    annotateFileError(tagValue, tag, tagValue.valueAsString?.trim(), "command", holder)
                }
            }

            override fun visitCaos2Tag(tagElement: CaosScriptCaos2Tag) {
                super.visitCaos2Tag(tagElement)
                if (!tagElement.containingCaosFile?.isCaos2Pray.orFalse())
                    return
                val tagRaw = tagElement.tagName.trim()
                val tag = PrayTags.normalize(tagRaw)
                    ?: tagRaw
                val tagValue = tagElement.caos2Value
                    ?: return
                annotateFileError(tagValue, tag, tagValue.valueAsString?.trim(), "property", holder)
            }
        }
    }
}


class PrayRequiredFileExistsInspection : LocalInspectionTool() {

    override fun getGroupDisplayName(): String = PRAY
    override fun getGroupPath(): Array<String> = CAOS2Path
    override fun getDisplayName(): String = AgentMessages.message("inspections.caos-to-compiler.file-exists.display-name", PRAY)

    override fun getShortName(): String = "PRAYSourceFileDoesNotExist"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {

        return object : PrayVisitor() {
            override fun visitPrayTag(o: PrayPrayTag) {
                super.visitPrayTag(o)
                val tagName = o.tagName.trim().nullIfEmpty()
                    ?: return
                val tagValue = o.tagTagValue
                annotateFileError(tagValue, tagName, tagValue.valueAsString, "property", holder)
            }

            override fun visitInlineFile(o: PrayInlineFile) {
                super.visitInlineFile(o)
                val inputFile = o.inputFileName
                    ?: return
                annotateFileError(inputFile, "Inline", inputFile.stringValue?.trim(), "command", true, holder)
            }

            override fun visitInputFileName(o: PrayInputFileName) {
                super.visitInputFileName(o)
                annotateFileError(o, "@Inline", o.stringValue?.trim(), "command", true, holder)
            }
        }
    }
}

private val requiresFileWithoutExtension = listOf(
    "Egg\\s+Gallery\\s+male",
    "Egg\\s+Gallery\\s+female",
    "Agent\\s+Animation\\s+Gallery",
).joinToString("|") { "($it)" }.toRegex(RegexOption.IGNORE_CASE)

private val requiresFiles = listOf(
    "Dependency\\s+\\d+",
    "Agent\\s+Animation\\s+File",
    "Egg\\s+Glyph\\s+File",
    "Egg\\s+Glyph\\s+File\\s2"
).joinToString("|") { "($it)" }.toRegex(RegexOption.IGNORE_CASE)

private val requiresScript = "(Link)".toRegex(RegexOption.IGNORE_CASE)


private fun requiresFile(tagName: String): Boolean {
    return requiresFiles.matches(tagName) ||
            requiresFileWithoutExtension.matches(tagName) ||
            requiresScript.matches(tagName)
}

private fun requiresFileWithoutExtension(tagName: String): Boolean {
    return requiresFileWithoutExtension.matches(tagName)
}

private val requiresSprite = listOf(
    "Egg Glyph File",
    "Egg Glyph File 2",
    "Agent Animation File",
    "Web Icon",
    "Agent Animation Gallery",
    "Egg Gallery male",
    "Egg Gallery female"
)

private val usesExisting = listOf(
    "Egg Glyph File",
    "Egg Glyph File 2",
    "Agent Animation File",
    "Web Icon",
    "Agent Animation Gallery",
    "Egg Gallery male",
    "Egg Gallery female"
).joinToString("|") { "(${it.replace(" ", "\\s+")})" }.toRegex(RegexOption.IGNORE_CASE)

private val requiresSpriteRegex = requiresSprite.joinToString("|") { "(" + it.replace(" ", "\\s+") + ")"}
    .toRegex(RegexOption.IGNORE_CASE)

private fun getRequiredExtension(tagName: String): List<String>? {
    return if (requiresSpriteRegex.matches(tagName))
        listOf("c16")
    else if (requiresScript.matches(tagName))
        listOf("cos", "caos")
    else
        null
}

private fun annotateCaos2PrayCommand(command: PrayCommand, values: List<CaosScriptCaos2Value>, holder: ProblemsHolder) {
    if (command == PrayCommand.DEPEND)
        return
    if (command == PrayCommand.LINK) {
        for (value in values) {
            annotateFileError(value, "Link", value.valueAsString, "command", holder)
        }
        return
    }

    if (command == PrayCommand.ATTACH) {
        val input = values.getOrNull(1)
            ?: return

        annotateFileError(input, "Link", input.valueAsString, "command", holder)
    }

    if (command == PrayCommand.INLINE) {
        for (value in values) {
            annotateFileError(value, "Inline", value.valueAsString, "command", holder)
        }
        return
    }
}

private fun annotateFileError(element: PsiElement, tagName: String, fileName: String?, type: String, holder: ProblemsHolder) {
    annotateFileError(element, tagName, fileName, type, false, holder)
}

private fun annotateFileError(element: PsiElement, tagName: String, fileName: String?, type: String, force: Boolean, holder: ProblemsHolder) {

    if (!requiresFile(tagName) && !force)
        return

    if (fileName.isNullOrBlank()) {
        holder.registerProblem(element, AgentMessages.message("caos.inspections.expects-file.filename-is-blank"))
        return
    }

    val stripExtension = requiresFileWithoutExtension(tagName)

    // If replacement files is null, then the file matches, so return
    val fixes: MutableList<LocalQuickFix> = (getFilenameSuggestions(
        element,
        stripExtension,
        fileName,
        getRequiredExtension(tagName)
    ) ?: return).toMutableList()

    fixes += listOfNotNull(
        element.containingFile.module?.let {
            AddIgnoredModuleLevelFile(fileName)
        },
        AddIgnoredProjectLevelFile(fileName)
    )

    if (usesExisting.matches(tagName)) {
        if (OsUtil.isWindows) {
            if (fileName likeAny DefaultGameFiles.C3DSFiles) {
                return
            }
        } else if (DefaultGameFiles.C3DSFiles.any { it == fileName })
            return
        val fileNameLowercase = fileName.toLowerCase()
        val orb = min(fileName.length / 3, 4)
        fixes += DefaultGameFiles.C3DSFiles.filter {
            it.toLowerCase().levenshteinDistance(fileNameLowercase) < orb
        }.map { replacement ->
            CaosScriptReplaceElementFix(
                element,
                "\"$replacement\"",
                "Replace with default file '$replacement'"
            )
        }
        val error = AgentMessages.message(
            "errors.files.referenced-file-might-not-exist",
            fileName
        )
        holder.registerProblem(element, error, ProblemHighlightType.WEAK_WARNING, *fixes.toTypedArray())
    } else {
        val error = AgentMessages.message(
            "errors.files.required-file-does-not-exist",
            fileName,
            "$tagName $type"
        )

        holder.registerProblem(element, error, *fixes.toTypedArray())
    }
}
