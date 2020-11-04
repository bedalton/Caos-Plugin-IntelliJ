package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosVar
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptTokenRValueStub
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType

abstract class CaosScriptTokenRvalueMixin : CaosScriptStubBasedElementImpl<CaosScriptTokenRValueStub> {
    constructor(stub: CaosScriptTokenRValueStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
    constructor(node: ASTNode) : super(node)

    val namedGameVar: CaosScriptNamedGameVar? get() = null

    val rvaluePrime: CaosScriptRvaluePrime? get() = null

    val varToken: CaosScriptVarToken? get() = null

    val commandToken: CaosScriptIsCommandToken? get() = null

    val commandString: String? get() = null

    val commandStringUpper: String? get() = null

    fun toCaosVar(): CaosVar = CaosVar.CaosLiteral.CaosToken(text)

    val arguments: List<CaosScriptArgument> get() = emptyList()

    val argumentValues: List<CaosVar> get() = emptyList()

    val inferredType: CaosExpressionValueType = CaosExpressionValueType.TOKEN
}