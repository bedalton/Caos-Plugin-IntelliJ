package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

interface CaosScriptIsAssignment : CaosScriptCompositeElement {
    val lvalue: CaosScriptLvalue?
}