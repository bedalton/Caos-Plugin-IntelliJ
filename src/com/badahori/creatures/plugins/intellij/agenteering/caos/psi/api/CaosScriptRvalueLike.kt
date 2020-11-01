package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosVar
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptTokenRValueStub
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType

interface CaosScriptRvalueLike : CaosScriptCompositeElement {

    val expression: CaosScriptLiteral?

    val namedConstant: CaosScriptNamedConstant?

    val namedGameVar: CaosScriptNamedGameVar?

    val namedVar: CaosScriptNamedVar?

    val rvaluePrime: CaosScriptRvaluePrime?

    val token: CaosScriptToken?

    val varToken: CaosScriptVarToken?

    val commandToken: CaosScriptIsCommandToken?

    val commandString: String?

    val commandStringUpper: String?

    fun toCaosVar(): CaosVar

    val arguments: List<CaosScriptArgument>

    val argumentValues: List<CaosVar>

    val inferredType: CaosExpressionValueType
}