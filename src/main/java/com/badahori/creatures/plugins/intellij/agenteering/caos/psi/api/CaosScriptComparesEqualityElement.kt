package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEqOp
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptLiteral

interface CaosScriptComparesEqualityElement : CaosScriptCompositeElement{
    val first: CaosScriptLiteral
    val second: CaosScriptLiteral
    val eqOp: CaosScriptEqOp
}