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
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Pray
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.module
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.PrayCommand.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.collectElementsOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import kotlin.math.min

class Caos2PrayRequiredFileExistsInspection : LocalInspectionTool(), DumbAware {

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

                PrayCommand.fromString(tagRaw)?.let {
                    annotateCaos2PrayCommand(it, commandElement.caos2ValueList, holder)
                    return
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
                annotateFileError(tagValue, tag, tagValue.valueAsString?.trim(), "property", false, holder)
            }
        }
    }
}

class PrayRequiredFileExistsInspection : LocalInspectionTool(), DumbAware {

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
                annotateFileError(tagValue, tagName, tagValue.valueAsString, "property", false, holder)
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
    "Egg\\s+Glyph\\s+File\\s2",
    "Web\\s+Icon"
).joinToString("|") { "($it)" }.toRegex(RegexOption.IGNORE_CASE)


private val requiresScript = "(Link)|(Script\\s+\\d+)|(RSCR)".toRegex(RegexOption.IGNORE_CASE)

private fun requiresFile(tagName: String): Boolean {
    return requiresFiles.matches(tagName) ||
            requiresFileWithoutExtension.matches(tagName)
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

internal fun getPrayTagRequiredExtension(tagName: String): List<String>? {
    return if (requiresSpriteRegex.matches(tagName)) {
        listOf("c16", "s16")
    } else if (requiresScript.matches(tagName)) {
        listOf("cos", "caos")
    } else {
        null
    }
}

/**
 * Adds annotations to missing files in CAOS2Pray commands
 */
private fun annotateCaos2PrayCommand(command: PrayCommand, values: List<CaosScriptCaos2Value>, holder: ProblemsHolder) {
    when (command) {
        LINK -> annotateFilesInCommand("Link", values, holder)
        ATTACH -> annotateFilesInCommand("Attach", values, holder)
        REMOVAL_SCRIPTS -> annotateRemovalScriptCommand(values, holder)
        INLINE -> {
            val input = values.lastOrNull()
                ?: return
            annotateFileError(input, "@Inline", input.valueAsString, "command", true, holder)
        }
        DEPEND -> {}
        PRAY_FILE -> {}
        JOIN -> annotateFilesInCommand("Join", values, holder)
    }
}

private fun annotateFilesInCommand(kind: String, values: List<CaosScriptCaos2Value>, holder: ProblemsHolder) {
    val atKind = if (kind.startsWith('@')) {
        kind
    } else {
        "@$kind"
    }
    for (value in values) {
        annotateFileError(value, atKind, value.valueAsString, "command", true, holder)
    }
    return
}

private fun annotateRemovalScriptCommand(values: List<CaosScriptCaos2Value>, holder: ProblemsHolder) {
    if (values.isEmpty()) {
        return
    }

    // Add error if more than one values is supplied
    if (values.size > 1) {
        holder.registerProblem(
            values.first().parent,
            TextRange.create(values.first().startOffset, values.last().endOffset),
            AgentMessages.message("pray.caos2pray.inspections.too-many-removal-scripts-in-command")
        )
        return
    }

    // Annotate only if file is path and not raw CAOS script
    val input = values.singleOrNull()
    val script = input
        ?.valueAsString
        ?: return
    if (isRawScriptNotFile(script)) {
        return
    }
    annotateFileError(input, "@RSCR", input.valueAsString, "command", true, holder)
}

private fun annotateFileError(element: PsiElement, tagName: String, fileName: String?, type: String, force: Boolean, holder: ProblemsHolder) {

    // Make sure tag has length.
    if (tagName.isEmpty())
        return

    val command = tagName[0] == '@'

    // Make sure tag needs file
    if (!requiresFile(tagName) && !command && !force) {
        return
    }

    if (fileName.isNullOrBlank()) {
        holder.registerProblem(element, AgentMessages.message("caos.inspections.expects-file.filename-is-blank"))
        return
    }

    val stripExtension = requiresFileWithoutExtension(tagName)
    val requiredExtensions = getPrayTagRequiredExtension(tagName)

    val file = element.containingFile
        ?: return
    val includedFiles = if (file is CaosScriptFile) {
        file.collectElementsOfType(CaosScriptCaos2Command::class.java)
            .flatMap map@{
                if (it.commandName like "Inline")
                    listOfNotNull(it.commandArgs.firstOrNull())
                else if (it.commandName like "Attach") {
                    it.commandArgs
                } else {
                    emptyList()
                }
            }
    } else {
        // Is pray file
        element.containingFile
            .collectElementsOfType(PrayOutputFileName::class.java)
            .mapNotNull {
                if (PsiTreeUtil.isAncestor(element, it, false)) {
                    null
                } else {
                    it.stringValue
                }
            }
    }

//    val includedFiles = (includedFilesRaw.map { PathUtil.getLastPathComponent(it) ?: it } + includedFilesRaw)
//        .distinct()
    // If replacement files is null, then the file matches, so return
    val fixes: MutableList<LocalQuickFix> = (getFilenameSuggestions(
        element,
        stripExtension,
        fileName,
        requiredExtensions,
        includedFiles = includedFiles,
        pathless = !command
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
        val fileNameLowercase = fileName.lowercase()
        val orb = min(fileName.length / 3, 4)
        fixes += DefaultGameFiles.C3DSFiles.filter {
            it.lowercase().levenshteinDistance(fileNameLowercase) < orb
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
//
//    if (stripExtension && requiredExtensions.isNotNullOrEmpty()) {
//        if (requiredExtensions.any { extension -> fileName.endsWith(extension)}) {
//            val error = AgentMessages.message("errors.files.no-extension", tagName)
//            holder.registerProblem(element, error, *fixes.toTypedArray())
//        }
//    }
}


/**
 * Returns a nullable pair of extension types and a boolean representing whether to drop extension
 * #Second <b>TRUE</b> Drop extension, <b>FALSE</b> require extension
 */
internal fun tagRequiresFileOfType(tagName: String): Pair<List<String>?, Boolean>? {
    val normalized = PrayTags.normalize(tagName)
        ?: return null
    if (!requiresFile(normalized))
        return null
    val extensions = getPrayTagRequiredExtension(normalized)
    val noExtension = requiresFileWithoutExtension.matches(normalized)
    return Pair(extensions, noExtension)
}

// Check that RSCR is a script not a file path
private const val FILE_NAME_CHAR = "[a-zA-Z0-9_'\" \\-#$&@()]"
private val PATH_REGEX by lazy { "(^[a-zA-Z]:\\\\[a-zA-Z0-9_'\" \\-#$&@()]+(\\.[^\"\\\\\n]+)?)|(^([a-zA-Z0-9_'\" \\-#\$&@()]*[/\\\\])*[a-zA-Z0-9_'\" \\-#\$&@()]+\\.[^\$\n]+\$)".toRegex() }
private val PRIMITIVE_CAOS_REGEX by lazy { "^((\\\\n)|(\\\\t)|( )|\\t|([+-]?[0-9]+)|\"([^\"\\\\]|\\\\.)*\"|\\[[^]]*]|([-+]?[0-9]*\\.[0-9]+)|((subr|gsub|goto)\\s+[^\\\\\\s]+)|([a-zA-Z_][a-zA-Z_\$*#:]{3})|(\\*([^\\n\\\\]|\\\\[^\\n])*))+\$".toRegex() }
internal fun isRawScriptNotFile(text: String): Boolean {
    if (text.isEmpty())
        return true
    if (PRIMITIVE_CAOS_REGEX.matches(text))
        return true
    return !PATH_REGEX.matches(text)
}