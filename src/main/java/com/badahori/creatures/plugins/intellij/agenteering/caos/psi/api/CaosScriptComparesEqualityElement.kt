package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api


interface CaosScriptComparesEqualityElement : CaosScriptCompositeElement{
    val first: CaosScriptRvalue
    val second: CaosScriptRvalue?
    val eqOp: CaosScriptEqOp?
}