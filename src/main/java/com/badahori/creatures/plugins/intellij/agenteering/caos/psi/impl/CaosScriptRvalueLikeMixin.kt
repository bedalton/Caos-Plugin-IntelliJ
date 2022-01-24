package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommand
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosValuesListValue
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.CaosScriptTokenRValueStub
import com.badahori.creatures.plugins.intellij.agenteering.utils.toListOf
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IElementType

abstract class CaosScriptRvalueLikeMixin<StubT : StubElement<out PsiElement>> : CaosScriptStubBasedElementImpl<StubT>, CaosScriptRvalueLike {
    constructor(stub: StubT, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
    constructor(node: ASTNode) : super(node)

    override val token: CaosScriptToken? get() =  null

    override val namedGameVar: CaosScriptNamedGameVar? get() = null

    override val rvaluePrime: CaosScriptRvaluePrime? get() = null

    override val varToken: CaosScriptVarToken? get() = null

    override val commandTokenElement: CaosScriptIsCommandToken? get() = null

    override val commandTokenElementType: IElementType? get() = null

    override val commandString: String? get() = null

    override val commandStringUpper: String? get() = null


    override val arguments: List<CaosScriptArgument> by lazy { emptyList<CaosScriptArgument>() }

    override val argumentValues: List<CaosExpressionValueType> by lazy { emptyList<CaosExpressionValueType>() }

    override val inferredType: List<CaosExpressionValueType> get() = CaosExpressionValueType.TOKEN.toListOf()

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