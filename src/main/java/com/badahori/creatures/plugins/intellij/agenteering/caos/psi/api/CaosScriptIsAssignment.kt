package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

interface CaosScriptIsAssignment : CaosScriptCompositeElement {
    val lvalue: CaosScriptLvalue?
    val commandString: String
    val commandStringUpper: String
}