package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosVar

interface CaosScriptRvalueLike : CaosScriptCompositeElement {

    val namedGameVar: CaosScriptNamedGameVar?

    val rvaluePrime: CaosScriptRvaluePrime?

    val token: CaosScriptToken?

    val varToken: CaosScriptVarToken?

    val quoteStringLiteral: CaosScriptQuoteStringLiteral?

    val c1String: CaosScriptC1String?

    val byteString: CaosScriptByteString?

    val animationString: CaosScriptAnimationString?

    val number: CaosScriptNumber?

    val pictDimensionLiteral: CaosScriptPictDimensionLiteral?

    val incomplete: CaosScriptIncomplete?

    val commandToken: CaosScriptIsCommandToken?

    val commandString: String?

    val commandStringUpper: String?

    fun toCaosVar(): CaosVar

    val arguments: List<CaosScriptArgument>

    val argumentValues: List<CaosVar>

    val inferredType: CaosExpressionValueType

    val index:Int
}