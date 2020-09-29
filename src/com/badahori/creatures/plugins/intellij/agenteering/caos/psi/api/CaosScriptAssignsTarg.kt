package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.AgentClass

interface CaosScriptAssignsTarg : CaosScriptCompositeElement {
    val targClass:AgentClass


}

val CaosScriptAssignsTarg.family get() = targClass.family
val CaosScriptAssignsTarg.genus get() = targClass.genus
val CaosScriptAssignsTarg.species get() = targClass.species