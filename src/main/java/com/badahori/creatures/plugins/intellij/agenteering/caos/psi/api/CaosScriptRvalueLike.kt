package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosCommand
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosValuesListValue
import com.intellij.psi.tree.IElementType

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

    val commandTokenElement: CaosScriptIsCommandToken?

    val commandTokenElementType: IElementType?

    val commandString: String?

    val commandStringUpper: String?

    val arguments: List<CaosScriptArgument>

    val argumentValues: List<CaosExpressionValueType>

    val inferredType: List<CaosExpressionValueType>

    val index:Int

    val commandDefinition:CaosCommand?

    val parameterValuesListValue:CaosValuesListValue?
}