package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

interface CaosScriptCommandWithArguments : CaosScriptCommandLike {
    val arguments: List<CaosScriptArgument>
    val argumentValues: List<CaosExpressionValueType>
}