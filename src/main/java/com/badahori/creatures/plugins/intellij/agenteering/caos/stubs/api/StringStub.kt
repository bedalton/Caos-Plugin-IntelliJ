package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api

import com.bedalton.common.util.PathUtil
import com.badahori.creatures.plugins.intellij.agenteering.bundles.general.getFileNameWithArrayAccess
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.inspections.tagRequiresFileOfType
import com.badahori.creatures.plugins.intellij.agenteering.caos.completion.textWithoutCompletionIdString
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.commandStringUpper
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType


enum class StringStubKind(val extensions: Set<String>? = null) {
    GAME(null),
    EAME(null),
    NAME(null),
    JOURNAL(null),
    WAV(setOf("WAV")),
    MNG(setOf("MNG", "MING")),
    MIDI(setOf("MIDI")),
    AUDIO(setOf("WAV", "MNG", "MING")),
    SPR(setOf("SPR")),
    S16(setOf("S16")),
    C16(setOf("C16")),
    GEN(setOf("GEN")),
    C2E_SPRITE(setOf("S16", "C16")),
    BLK(setOf("BLK")),
    COS(setOf("COS", "CAOS")),
    ;

    val isFile get() = extensions.isNotNullOrEmpty()

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
            } catch (e: Exception) {
                e.rethrowAnyCancellationException()
                null
            }
        }

        fun fromPsiElement(element: PsiElement): StringStubKind? {
            return getStubKind(element)
        }

        fun fromExtension(extension: String): StringStubKind? {
            return values().firstOrNull {
                val extensions = it.extensions
                    ?: return@firstOrNull false
                extension likeAny extensions
            }
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
    return when (extension.uppercase()) {
        "COS" -> StringStubKind.COS
        "SPR" -> StringStubKind.SPR
        "S16" -> StringStubKind.S16
        "C16" -> StringStubKind.C16
        "C16/S16", "S16/C16" -> StringStubKind.C2E_SPRITE
        "WAV" -> StringStubKind.WAV
        "MNG" -> StringStubKind.MNG
        "MNG/WAV", "WAV/MNG" -> StringStubKind.AUDIO
        "BLK" -> StringStubKind.BLK
        "GEN" -> StringStubKind.GEN
        "JOURNAL" -> StringStubKind.JOURNAL
        "MIDI" -> StringStubKind.MIDI
        else -> null
    }
}

infix fun StringStubKind?.notLike(other: StringStubKind?): Boolean {
    if (this == null || other == null) {
        return false
    }
    if (this == other) {
        return false
    }
    val thisExtensions = this.extensions
        .nullIfEmpty()
        ?.map { it.uppercase() }
        ?.toSet()
    val otherExtensions = other.extensions
        .nullIfEmpty()
        ?.map { it.uppercase() }
        ?.toSet()
    return thisExtensions == null ||
            otherExtensions == null ||
            thisExtensions.intersect(otherExtensions).isEmpty()
}

infix fun StringStubKind?.like(other: StringStubKind?): Boolean {
    if (this == null || other == null) {
        return false
    }
    if (this == other) {
        return true
    }
    val thisExtensions = this.extensions
        .nullIfEmpty()
        ?.map { it.uppercase() }
        ?.toSet()
        ?: return false

    val otherExtensions = other.extensions
        .nullIfEmpty()
        ?.map { it.uppercase() }
        ?.toSet()
        ?: return false

    return thisExtensions.intersect(otherExtensions).isNotEmpty()
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

    // Get parameter information
    var index = argument.index
    // Get parent command
    var command = argument.parent as? CaosScriptCommandElement
        ?: return null

    // Handle TOKN, which returns an int, but is needed for genome files
    if (variant.isOld && command.commandStringUpper == "TOKN") {
        index = command.getSelfOrParentOfType(CaosScriptArgument::class.java)?.index
            ?: return null
        command = command.getParentOfType(CaosScriptCommandElement::class.java)
            ?: return null
    }


    // Get called command details
    val commandDefinition = command.commandDefinition
        ?: return null

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
        e.rethrowAnyCancellationException()
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
    val extension = PathUtil.getExtension(fileName)
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
    val fileNameRaw = PathUtil.getLastPathComponent(file)
        ?: file
    val fileName = getFileNameWithArrayAccess(fileNameRaw)
        ?: fileNameRaw

    val fileExtension = PathUtil.getExtension(fileName)
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
    (PathUtil.getExtension(file))?.let { extension ->
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
