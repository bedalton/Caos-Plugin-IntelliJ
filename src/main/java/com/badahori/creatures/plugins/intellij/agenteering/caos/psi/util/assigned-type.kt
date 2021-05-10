package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLib
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCAssignment
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRvalue
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.utils.WHITESPACE
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.utils.orTrue


@Suppress("SpellCheckingInspection")
private val INT_ASSIGNMENT_COMMANDS = listOf(
        "ADDV",
        "ANDV",
        "SUBV",
        "DIVV",
        "MULV",
        "MODV",
        "ORRV",
        "NEW: GENE",
        "NET: RUSO"
)

@Suppress("SpellCheckingInspection")
private val STRING_ASSIGNMENT_COMMANDS = listOf(
        "ADDS",
        "CHAR",
        "NET: UNIK",
        "SETS"
)

@Suppress("SpellCheckingInspection")
fun CaosScriptCAssignment.getAssignedType(bias:CaosExpressionValueType): CaosExpressionValueType? {
    return when (commandStringUpper.replace("\\s\\s+".toRegex(), " ")) {
        //"ABSV", "NOTV" -> getSetvValue(arguments.getOrNull(0) as? CaosScriptRvalue, bias)
        //"SETV" -> getSetvValue(arguments.getOrNull(1) as? CaosScriptRvalue)
        //in INT_ASSIGNMENT_COMMANDS -> CaosExpressionValueType.INT
        //"RTAR" -> CaosExpressionValueType.AGENT
        //"SETA" -> getSetvValue(arguments.getOrNull(1) as? CaosScriptRvalue)
        //in STRING_ASSIGNMENT_COMMANDS -> getSetvValue(arguments.getOrNull(1) as? CaosScriptRvalue)
        else -> getSetvValue(arguments.getOrNull(1) as? CaosScriptRvalue, bias)
    }
}

private fun getSetvValue(rvalue: CaosScriptRvalue?, bias:CaosExpressionValueType): CaosExpressionValueType? {
    if (rvalue == null)
        return null
    return if (rvalue.variant?.isNotOld.orTrue()) {
        val commandString = rvalue.commandStringUpper?.replace(WHITESPACE, " ")
            ?: return null
        when {
            rvalue.isInt.orFalse() -> CaosExpressionValueType.INT
            rvalue.isFloat.orFalse() -> CaosExpressionValueType.FLOAT
            rvalue.isNumeric.orFalse() -> CaosExpressionValueType.DECIMAL
            rvalue.isC1String -> CaosExpressionValueType.STRING
            rvalue.isString -> CaosExpressionValueType.STRING
            rvalue.isByteString -> CaosExpressionValueType.BYTE_STRING
            rvalue.isToken -> CaosExpressionValueType.TOKEN
            rvalue.animationString != null -> CaosExpressionValueType.ANIMATION
            else -> rvalue.variant?.let { variant ->
                CaosLibs[variant].rvalues.filter { command -> command.command == commandString }.let { commands ->
                    if (commands.any { command -> command.returnType == bias }) {
                        bias
                    } else
                        commands.firstOrNull()?.returnType
                }
            }
        }
    } else
        rvalue.getInferredType(bias)
}
