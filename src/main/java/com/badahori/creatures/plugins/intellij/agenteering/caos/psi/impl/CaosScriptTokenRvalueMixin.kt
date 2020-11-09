package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosVar
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommand
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosValuesListValue
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptTokenRValueStub
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType

abstract class CaosScriptTokenRvalueMixin : CaosScriptStubBasedElementImpl<CaosScriptTokenRValueStub>, CaosScriptRvalueLike {
    constructor(stub: CaosScriptTokenRValueStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
    constructor(node: ASTNode) : super(node)

    override val namedGameVar: CaosScriptNamedGameVar? get() = null

    override val rvaluePrime: CaosScriptRvaluePrime? get() = null

    override val varToken: CaosScriptVarToken? get() = null

    override val commandToken: CaosScriptIsCommandToken? get() = null

    override val commandString: String? get() = null

    override val commandStringUpper: String? get() = null

    override fun toCaosVar(): CaosVar = CaosVar.CaosLiteral.CaosToken(text)

    override val arguments: List<CaosScriptArgument> by lazy { emptyList<CaosScriptArgument>() }

    override val argumentValues: List<CaosVar> by lazy { emptyList<CaosVar>() }

    override val inferredType: CaosExpressionValueType get() = CaosExpressionValueType.TOKEN

    override val quoteStringLiteral:CaosScriptQuoteStringLiteral? get() = null

    override val c1String: CaosScriptC1String? get() = null

    override val byteString: CaosScriptByteString? get() = null

    override val animationString: CaosScriptAnimationString? get() = null

    override val number: CaosScriptNumber? get() = null

    override val pictDimensionLiteral: CaosScriptPictDimensionLiteral? get() = null

    override val incomplete: CaosScriptIncomplete? get() = null

    override val commandDefinition:CaosCommand? get() = null

    override val parameterValuesListValue: CaosValuesListValue? = null
}