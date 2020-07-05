package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptEqOp
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptExpression

interface CaosScriptComparesEqualityElement : CaosScriptCompositeElement{
    val first: CaosScriptExpression
    val second: CaosScriptExpression
    val eqOp: CaosScriptEqOp
}