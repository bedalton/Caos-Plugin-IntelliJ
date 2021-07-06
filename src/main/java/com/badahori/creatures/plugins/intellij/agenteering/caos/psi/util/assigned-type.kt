package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.getRvalueTypeWithoutInference
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
    return getRvalueTypeWithoutInference(arguments.getOrNull(1) as? CaosScriptRvalue, bias)
}

