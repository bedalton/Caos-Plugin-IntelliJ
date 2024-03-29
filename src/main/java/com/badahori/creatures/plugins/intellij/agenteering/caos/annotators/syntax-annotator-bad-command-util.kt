package com.badahori.creatures.plugins.intellij.agenteering.caos.annotators

import com.badahori.creatures.plugins.intellij.agenteering.caos.fixes.CaosScriptInsertBeforeFix
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle.message
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.isCaos2Cob
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommand
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCommandElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommandType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCKwInvalidLoopTerminator
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getEnclosingCommandType
import com.badahori.creatures.plugins.intellij.agenteering.utils.getNextNonEmptySibling
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement

/**
 * Tests command token validity and generates an annotation if needed
 * Checks for:
 *  - Command existence
 *  - Proper context(i.e. Command, RValue, LValue)
 *  - Variant
 */
internal fun getErrorCommandAnnotation(variant: CaosVariant, element: PsiElement, commandToken: String, annotationHolder: AnnotationHolder): MyAnnotationBuilder? {
    if (variant == CaosVariant.UNKNOWN)
        return null
    // Get command as upper case
    val commandToUpperCase = commandToken.uppercase()

    if ((commandToUpperCase == "RSCR" || commandToUpperCase == "ISCR") && (element.containingFile as? CaosScriptFile)?.isCaos2Cob.orFalse())
        return null

    if (commandToUpperCase == "VARX" ||
        commandToUpperCase == "VAXX" ||
        commandToUpperCase == "OVXX" ||
        commandToUpperCase == "OBVX" ||
        commandToUpperCase == "MVXX"
    ) {
        return annotationHolder
            .newErrorAnnotation(message("caos.annotator.syntax-error-annotator.invalid-command", commandToUpperCase))
            .range(element)
    }

    // Ascertain the type of command involved
    val commandType = element.getEnclosingCommandType()

    // If command type cannot be determined, exit This means it was used as out of command expression
    if (commandType == CaosCommandType.UNDEFINED)
        return annotationHolder
                .newErrorAnnotation(message("caos.annotator.syntax-error-annotator.invalid-command", commandToUpperCase))
                .range(element)

    if (CaosLibs[variant][commandType][commandToUpperCase] != null) {
        return null
    }

    val commands = CaosLibs.commands(commandToUpperCase)

    // If matches do not exist in libs object it is completely invalid
    // If commands do exist, they will be formatted differently
    // Based on type and variants involved
    if (commands.isEmpty()) {
        return annotationHolder
                .newErrorAnnotation(message("caos.annotator.syntax-error-annotator.invalid-command", commandToUpperCase))
                .range(element)
    }

    // Get all commands matching token and variant
    val variantCode = variant.code
    val commandsInVariant = commands
            .filter { command ->
                variantCode in command.variants
            }

    // Command exists in variant, but is not the right type
    // i.e. Used as command but is rvalue
    if (commandsInVariant.isNotEmpty()) {
        return annotationInvalidCommandTypeInVariant(
                variant = variant,
                element = element,
                commandToken = commandToken,
                commandType = commandType,
                commandsInVariant = commandsInVariant,
                annotationHolder = annotationHolder
        )
    }
    // == if command does not exist in variant, it exists outside of variant
    // The check for variants to exist was checked earlier on

    // Get filter to find command in other variants with the correct type
    val filter: (command: CaosCommand) -> Boolean = when (commandType) {
        CaosCommandType.COMMAND -> { command ->
            command.isCommand
        }
        CaosCommandType.RVALUE -> { command -> command.rvalue }
        CaosCommandType.LVALUE -> { command -> command.lvalue }
        else -> { _ -> false }
    }

    // Find commands matching command type
    val commandsOutOfVariant = commands
            .filter(filter)
    if (commandsOutOfVariant.isNotEmpty()) {
        return validTypeInOtherVariantsAnnotation(
                element,
                commandType,
                commandToken,
                commandsOutOfVariant,
                annotationHolder
        )
    }

    // Is in the format "an l/r value", "a command", etc.
    val type = formatPossibleCommandTypes(commands)

    // Format all variants matching commands as a simplified string
    val variantsString = variantsString(commands)

    val message = message(
            "caos.annotator.syntax-error-annotator.invalid-command-and-type",
            commandToUpperCase,
            type,
            variantsString
    )

    // Create and return annotation
    return annotationHolder
            .newErrorAnnotation(message)
            .range(element)
}

/**
 * Annotates a command error when used as incorrect type
 * i.e. RValue used as Command or LValue
 */
private fun annotationInvalidCommandTypeInVariant(
        variant: CaosVariant,
        element: PsiElement,
        commandToken: String,
        commandType: CaosCommandType,
        commandsInVariant: List<CaosCommand>,
        annotationHolder: AnnotationHolder
): MyAnnotationBuilder {
    // Format command to upper case
    val commandToUpperCase: String = commandToken.uppercase()
    // Gets the command types as a string
    val alternativeCommandTypesAsString = formatPossibleCommandTypes(commandsInVariant)
    // Format message
    val message = message(
            "caos.annotator.syntax-error-annotator.invalid-command-type-in-variant",
            commandToUpperCase,
            commandType.value.lowercase(),
            alternativeCommandTypesAsString,
            variant.code,
    )
    // Build annotation
    var builder = annotationHolder
            .newErrorAnnotation(message)
            .range(element)

    // Add SETV/SETS/SETA fixes as needed
    if (commandType == CaosCommandType.COMMAND && commandsInVariant.any { it.lvalue }) {
        builder = addSetvLikeFixes(variant, element, commandToken, builder)
    }

    // create and return error annotation
    return builder
}

/**
 * Adds SETV/SETA/SETS prefixing fixes as needed
 */
private fun addSetvLikeFixes(variant: CaosVariant, element:PsiElement, commandToken: String, builderIn:MyAnnotationBuilder) : MyAnnotationBuilder {
    var builder = builderIn
    val commandToUpperCase = commandToken.uppercase()
    val nextRvalue = nextRvalues(element)
    val expectsString = nextRvalue.any { (it.command != "CHAR" && it.command != "TRAN") && it.returnType == CaosExpressionValueType.STRING }
    // Add fixes for left values as needed
    if (variant.isOld) {
        // If old variant and lvalue is not a string value return
        if (!expectsString)
            builder = builder
                    .withFix(CaosScriptInsertBeforeFix("Insert SETV before $commandToUpperCase", "setv", element))
    } else {
        val case = commandToken.case
        if (expectsString)
            builder = builder
                    .withFix(CaosScriptInsertBeforeFix("Insert SETS before $commandToUpperCase", "SETS".matchCase(case), element))
        if (nextRvalue.any { it.returnType == CaosExpressionValueType.AGENT })
            builder = builder
                    .withFix(CaosScriptInsertBeforeFix("Insert SETA before $commandToUpperCase", "SETA".matchCase(case), element))

        if (nextRvalue.any { it.returnType != CaosExpressionValueType.AGENT && it.returnType != CaosExpressionValueType.STRING })
            builder = builder
                    .withFix(CaosScriptInsertBeforeFix("Insert SETS before $commandToUpperCase", "SETS".matchCase(case), element))
    }
    return builder
}

/**
 * Error annotation when command exists as type, but not in this variant
 * i.e. ATTR used as command in C1 when it is only command in CV+ as opposed to an L/Rvalue in C1
 */
private fun validTypeInOtherVariantsAnnotation(
        element: PsiElement,
        commandType:CaosCommandType,
        commandToken:String,
        commandsOutOfVariant:List<CaosCommand>,
        annotationHolder: AnnotationHolder
) : MyAnnotationBuilder {

    // Format all variants matching commands as a simplified string
    val variantsString = variantsString(commandsOutOfVariant)

    // Format message for command and variant
    val message = message(
            "caos.annotator.syntax-error-annotator.invalid-variant",
            commandToken.uppercase(),
            commandType.value.lowercase(),
            variantsString

    )

    // Create and return annotation
    return annotationHolder
            .newErrorAnnotation(message)
            .range(element)
}

/**
 * Gets possibly next rvalue in an error command call
 */
private fun nextRvalues(element: PsiElement): List<CaosCommand> {
    val nextElement = element.getNextNonEmptySibling(false)

    // If next element is already a command element
    // Simply return its definition
    (nextElement as? CaosScriptCommandElement)?.let { next ->
        listOfNotNull(next.commandDefinition)
                .nullIfEmpty()
                ?.let { commands -> return commands }
    }

    // Get text for next->next, in case of a two word command
    val nextText = nextElement?.text?.uppercase()
    val nextNextText = nextElement?.getNextNonEmptySibling(false)?.text?.uppercase()

    // Return possible next commands
    return when {
        nextNextText != null -> {
            CaosLibs.commands("$nextText $nextNextText").filter { it.rvalue }.nullIfEmpty()
                    ?: CaosLibs.commands(nextText!!).filter { it.rvalue }
        }
        nextText != null -> {
            CaosLibs.commands(nextText)
        }
        else -> {
            emptyList()
        }
    }
}

/**
 * Formats a list of commands to their possible command type combinations
 */
private fun formatPossibleCommandTypes(commands:List<CaosCommand>) : String {
    val rvalue = commands.any { it.rvalue }
    val lvalue = commands.any { it.lvalue }
    val command = commands.any { it.isCommand }
    //
    return when {
        rvalue && lvalue -> "an l/r value"
        rvalue && command -> "an rvalue and command"
        lvalue && command -> "an lvalue and command"
        command -> "a command"
        rvalue -> "an rvalue"
        lvalue -> "an lvalue"
        else -> throw Exception("Command should have been an lvalue, rvalue or command if it exists")
    }
}

/**
 * Formats all variants found in a list of commands as a simplified string
 */
private fun variantsString(commands:List<CaosCommand>) : String {
    val variants = commands
            // Map all possible variants matching this command
            .flatMap {
                it.variants
            }
            // Make unique
            .toSet()
            // Convert string to variant object
            // for use in formatter
            .map {
                CaosVariant.fromVal(it)
            }
            // Filter out unknown as it would mess up the description
            .filter {
                it != CaosVariant.UNKNOWN
            }

    // Format variants to simple form. ie CV,C3,DS -> CV+
    return getVariantString(variants)
}

/**
 * Formats a list of variants into a simplified string
 */
private fun getVariantString(variantsIn: List<CaosVariant>): String {
    val variants = variantsIn.sortedBy { it.index }
    return when {
        4 == variants.intersect(setOf(CaosVariant.C2, CaosVariant.CV, CaosVariant.C3, CaosVariant.DS)).size -> "C2+"
        3 == variants.intersect(setOf(CaosVariant.CV, CaosVariant.C3, CaosVariant.DS)).size -> "CV+"
        2 == variants.intersect(setOf(CaosVariant.C3, CaosVariant.DS)).size -> "C3+"
        else -> variants.joinToString(",") { it.code }
    }
}

internal fun annotateInvalidLoopTerminator(element:CaosScriptCKwInvalidLoopTerminator, holder: AnnotationHolder) {
    val commandStringUpper = element.commandStringUpper
    val expectedLoop = when (commandStringUpper) {
        "ELSE", "ELIF" -> "DOIF statement"
        "EVER", "UNTIL" -> "LOOP statement"
        "RETN" -> "Subroutine"
        "REPE" -> "REPS loop"
        "NEXT" -> "ENUM, ESEE, ETCH, EPAS or ECON loop"
        "NSCN" -> "ESCN loop"
        else -> "Control Statement"
    }
    val error = message("caos.annotator.syntax-error-annotator.invalid-loop-terminator", commandStringUpper, expectedLoop)
    holder.newErrorAnnotation(error)
            .range(element)
            .create()


}