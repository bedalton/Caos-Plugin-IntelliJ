package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCAssignment
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRvalue
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
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
fun CaosScriptCAssignment.getAssignedType(): CaosExpressionValueType? {
    if (cKwAssignNumber != null) {
        return if (this.variant?.isOld.orFalse())
            CaosExpressionValueType.INT
        else
            CaosExpressionValueType.DECIMAL
    }
    return when (commandStringUpper!!.replace("\\s\\s+".toRegex(), " ")) {
        "SETV" -> getSetvValue(arguments.getOrNull(1) as? CaosScriptRvalue)
        in INT_ASSIGNMENT_COMMANDS -> CaosExpressionValueType.INT
        "RTAR" -> CaosExpressionValueType.AGENT
        "SETA" -> CaosExpressionValueType.AGENT
        in STRING_ASSIGNMENT_COMMANDS -> CaosExpressionValueType.STRING
        "ABSV" -> CaosExpressionValueType.DECIMAL
        "NOTV" -> CaosExpressionValueType.DECIMAL
        else -> null
    }?.let { type ->
        return type
    }
}

private fun getSetvValue(rvalue: CaosScriptRvalue?): CaosExpressionValueType? {
    if (rvalue == null)
        return null
    return if (rvalue.variant?.isNotOld.orTrue()) {
        when {
            rvalue.isInt.orFalse() -> CaosExpressionValueType.INT
            rvalue.isFloat.orFalse() -> CaosExpressionValueType.FLOAT
            rvalue.isNumeric.orFalse() -> CaosExpressionValueType.DECIMAL
            else -> rvalue.inferredType
        }
    } else
        rvalue.inferredType
}
