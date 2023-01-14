package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api

import com.bedalton.common.util.FileNameUtil
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.getFileNameWithArrayAccess
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.inspections.tagRequiresFileOfType
import com.badahori.creatures.plugins.intellij.agenteering.caos.completion.textWithoutCompletionIdString
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType


enum class StringStubKind(val isFile: Boolean) {
    GAME(false),
    EAME(false),
    NAME(false),
    JOURNAL(false),
    WAV(true),
    C16(true),
    S16(true),
    C2E_SPRITE(true),
    BLK(true),
    COS(true),
    SPR(true),
    MNG(true)
    ;


    override fun toString(): String {
        return name
    }

    companion object {
        fun fromString(value: String?): StringStubKind? {
            if (value.isNullOrBlank()) {
                return null
            }
            return try {
                valueOf(value.uppercase())
            } catch (_: Exception) {
                null
            }
        }

        fun fromPsiElement(element: PsiElement): StringStubKind? {
            return getStubKind(element)
        }
    }
}

private fun getStubKind(element: PsiElement): StringStubKind? {
    if (
        element !is CaosScriptStringText &&
        element.parent !is CaosScriptStringText &&
        element !is CaosScriptQuoteStringLiteral &&
        element !is CaosScriptCaos2Value &&
        element !is CaosScriptRvalueLike &&
        element !is CaosScriptToken
    ) {
        return null
    }
    val variant = element.variant
        .nullIfUnknown()
        ?: CaosVariant.DS

    // Get as Caos2Value
    // Tries parent as well in case element is a quote string, but still inside a CAOS2Value
    (element as? CaosScriptCaos2Value ?: element.parent as? CaosScriptCaos2Value)?.let { value ->
        return getCaos2StubKind(variant, value)
    }

    // Get as quote literal as needed
    val quoteStringLiteral = element as? CaosScriptQuoteStringLiteral
        ?: element.parent as? CaosScriptQuoteStringLiteral
        ?: element.parent?.parent as? CaosScriptQuoteStringLiteral

    if (quoteStringLiteral != null) {
        return getParameterStubKind(variant, quoteStringLiteral)
    }

    // Get element or parent as Rvalue like
    val rvalue = element as? CaosScriptRvalueLike
        ?: element.parent as? CaosScriptRvalueLike
        ?: element.parent?.parent as? CaosScriptRvalueLike

    // Found rvalue type, so get string stub type
    if (rvalue != null) {
        return getArgumentStubType(variant, rvalue)
    }

    // Element cannot have a string stub kind
    return null
}

private fun getStringStubKindForExtension(extension: String?): StringStubKind? {
    if (extension == null) {
        return null
    }
    return when (extension) {
        "COS" -> StringStubKind.COS
        "SPR" -> StringStubKind.SPR
        "S16" -> StringStubKind.S16
        "C16" -> StringStubKind.C16
        "C16/S16", "S16/C16" -> StringStubKind.C2E_SPRITE
        "WAV" -> StringStubKind.WAV
        "MNG" -> StringStubKind.MNG
        "BLK" -> StringStubKind.BLK
        else -> null
    }
}

private fun getParameterStubKind(variant: CaosVariant, element: CaosScriptQuoteStringLiteral): StringStubKind? {
    element.stub?.kind?.let {
        return it
    }
    // Get parent argument
    val argument: CaosScriptArgument = element.parent as? CaosScriptRvalue
        ?: element.parent?.parent as? CaosScriptRvalue
        ?: return null
    return getArgumentStubType(variant, argument)
}

private fun getTokenStringStubKind(variant: CaosVariant, element: CaosScriptToken): StringStubKind? {
    // Get parent argument
    // Gets as RValueLike because parent could be rvalue or token rvalue
    val argument: CaosScriptArgument = element.parent as? CaosScriptRvalueLike
        ?: return null
    return getArgumentStubType(variant, argument)
}


private fun getArgumentStubType(variant: CaosVariant, argument: CaosScriptArgument): StringStubKind? {

    // This should weed out LValue and anything not thought of
    if (argument !is CaosScriptRvalueLike) {
        return null
    }

    // Get stubbed string stub kind for RValue
    ((argument as? CaosScriptRvalue)?.stub)?.let {
        return it.stringStubKind
    }

    // Get stubbed string stub kind for Token Rvalue
    ((argument as? CaosScriptTokenRvalue)?.stub)?.let {
        return it.stringStubKind
    }

    // Get parent command
    val command = argument.parent as? CaosScriptCommandElement
        ?: return null

    // Get called command details
    val commandDefinition = command.commandDefinition
        ?: return null

    // Get parameter information
    val index = argument.index
    val parameter = commandDefinition.parameters.getOrNull(index)
        ?: return null

    // Make sure the expected type of the parameter could match a string
    if (parameter.type != CaosExpressionValueType.STRING
        && parameter.type != CaosExpressionValueType.ANY
        && parameter.type != CaosExpressionValueType.UNKNOWN
        && parameter.type != CaosExpressionValueType.TOKEN
    ) {
        return null
    }

    // Get first four chars of command for a fast assumption of stub type
    val first = try {
        commandDefinition.command.substring(0, 4)
    } catch (e: Exception) {
        return null
    }

    // Try to discern string stub type
    return when (first) {
        //GAME
        "GAME" -> StringStubKind.GAME
        "DELG" -> StringStubKind.GAME
        // EAME
        "EAME" -> StringStubKind.EAME
        "DELE" -> StringStubKind.EAME
        // NAME
        "NAME" -> StringStubKind.NAME
        "MAME" -> StringStubKind.NAME
        "DELN" -> StringStubKind.NAME
        // JOURNAL
        "FILE" -> StringStubKind.JOURNAL
        else -> {
            // As fallback, get type for parameter values list
            val listName = parameter
                .valuesList[variant]
                ?.name
                ?.uppercase()
                ?: return null
            if (!listName.startsWith("FILE.")) {
                return null
            }
            getStringStubKindForExtension(listName.substring(5))
        }
    }
}


private val hasFileCaos2Commands = listOf(
    "attach",
    "link",
    "depend",
    "depends",
    "inline"
).map { it.lowercase() }

private fun getCaos2StubKind(variant: CaosVariant, element: CaosScriptCaos2Value): StringStubKind? {

    val caos2Parent = element.parent
        ?: return null

    (caos2Parent as? CaosScriptCaos2Tag)?.let {
        return getCaos2TagStringStubKind(variant, element, caos2Parent)
    }

    (element.parent as? CaosScriptCaos2Command)?.let { command ->
        return getCaos2CommandStubKind(element, command)
    }
    return null
}

private fun getCaos2CommandStubKind(
    element: CaosScriptCaos2Value,
    command: CaosScriptCaos2Command,
): StringStubKind? {

    // Get CAOS2 command text value
    val commandName = command.commandName.lowercase()

    // Check if this command requires a file
    // This is a simple way to exclude pray file name, and agent name commands
    if (commandName.lowercase() !in hasFileCaos2Commands) {
        return null
    }

    // Get this value as a string without quotes
    val fileName = element.valueAsString
        ?: return null

    // Get actual file extension
    val extension = FileNameUtil.getExtension(fileName)
        ?: return null

    // Get stub kind for extension
    return getStringStubKindForExtension(extension)
}


private fun getCaos2TagStringStubKind(
    variant: CaosVariant,
    element: CaosScriptCaos2Value,
    tag: CaosScriptCaos2Tag,
): StringStubKind? {
    val file = element.valueAsString
        ?.nullIfEmpty()
        ?: return null

    val tagName = tag.tagName
        .nullIfEmpty()
        ?: return null

    return if (variant.isOld) {
        getC1eCaos2TagStubKind(file, tagName)
    } else {
        getC2eCaos2TagStubKind(file, tagName)
    }
}


private fun getC1eCaos2TagStubKind(
    file: String,
    tagName: String,
): StringStubKind? {

    val tagKind = CobTag.fromString(tagName)
        ?: return null

    if (tagKind != CobTag.THUMBNAIL) {
        return null
    }
    val fileNameRaw = FileNameUtil.getLastPathComponent(file)
        ?: file
    val fileName = getFileNameWithArrayAccess(fileNameRaw)
        ?: fileNameRaw

    val fileExtension = FileNameUtil.getExtension(fileName)
        ?: return null

    return getStringStubKindForExtension(fileExtension)
}

private fun getC2eCaos2TagStubKind(
    file: String,
    tagName: String,
): StringStubKind? {

    val data = tagRequiresFileOfType(tagName)
        ?: return null

    // If this requires file, just try with whatever extension it has
    (FileNameUtil.getExtension(file))?.let { extension ->
        return getStringStubKindForExtension(extension)
    }

    if (!data.second) {
        return null
    }

    // If file has no extension, get expected extension for tag
    return when (data.first?.firstOrNull()?.uppercase()) {
        "S16", "C16" -> StringStubKind.C2E_SPRITE
        "COS", "CAOS" -> StringStubKind.COS
        else -> null
    }
}

internal infix fun StringStubKind?.like(other: StringStubKind?): Boolean {
    if (this == null || other == null) {
        return false
    }
    return if (this == StringStubKind.C2E_SPRITE) {
        when (other) {
            StringStubKind.S16 -> true
            StringStubKind.C16 -> true
            StringStubKind.C2E_SPRITE -> true
            else -> false
        }
    } else if (other == StringStubKind.C2E_SPRITE) {
        when (this) {
            StringStubKind.S16 -> true
            StringStubKind.C16 -> true
            else -> false
        }
    } else {
        this == other
    }
}