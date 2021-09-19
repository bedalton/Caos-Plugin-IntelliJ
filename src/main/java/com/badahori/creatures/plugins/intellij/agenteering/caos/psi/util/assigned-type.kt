package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.getRvalueTypeWithoutInference
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCAssignment
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptRvalue


@Suppress("SpellCheckingInspection", "unused")
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

@Suppress("SpellCheckingInspection", "unused")
private val STRING_ASSIGNMENT_COMMANDS = listOf(
        "ADDS",
        "CHAR",
        "NET: UNIK",
        "SETS"
)

@Suppress("SpellCheckingInspection", "unused")
fun CaosScriptCAssignment.getAssignedType(bias:CaosExpressionValueType): CaosExpressionValueType? {
    return getRvalueTypeWithoutInference(arguments.getOrNull(1) as? CaosScriptRvalue, bias, true)
}

