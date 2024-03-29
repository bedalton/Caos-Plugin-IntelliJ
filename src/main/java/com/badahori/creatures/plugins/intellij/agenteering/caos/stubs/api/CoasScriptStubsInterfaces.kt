package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptVarTokenGroup
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosScriptNamedGameVarType
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCaos2ValueToken
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CobTag
import com.intellij.openapi.util.TextRange
import com.intellij.psi.stubs.StubElement

interface CaosScriptCommandCallStub : StubElement<CaosScriptCommandCallImpl> {
    val command:String
    val commandUpper:String
    val commandTokens:List<String>
    val argumentValues:List<CaosExpressionValueType>
    val numArguments:Int get() = argumentValues.size
}

interface CaosScriptTargAssignmentStub : StubElement<CaosScriptCTargImpl> {
    val scope:CaosScope
    val rvalue: List<CaosExpressionValueType>?
}

interface CaosScriptRtarAssignmentStub : StubElement<CaosScriptCTargImpl> {
    val scope:CaosScope
    val family:CaosExpressionValueType?
    val genus:CaosExpressionValueType?
    val species:CaosExpressionValueType?
}

interface CaosScriptLValueStub : StubElement<CaosScriptLvalueImpl> {
    val type:List<CaosExpressionValueType>
    val argumentValues: List<CaosExpressionValueType>
    val commandString:String?
}

interface CaosScriptRValueStub : StubElement<CaosScriptRvalueImpl> {
    val type: List<CaosExpressionValueType>
    val argumentValues: List<CaosExpressionValueType>
    val commandString:String?
    val stringStubKind: StringStubKind?
}

interface CaosScriptTokenRValueStub : StubElement<CaosScriptTokenRvalueImpl> {
    val tokenText:String?
    val stringStubKind: StringStubKind?
}

interface CaosScriptSubroutineNameStub : StubElement<CaosScriptSubroutineNameImpl> {
    val tokenText:String?
}

interface CaosScriptRValuePrimeStub : StubElement<CaosScriptRvaluePrimeImpl> {
    val caosVar:CaosExpressionValueType
    val argumentValues: List<CaosExpressionValueType>
    val commandString:String?
}


interface CaosScriptRndvStub : StubElement<CaosScriptCRndvImpl> {
    val min:Int?
    val max:Int?
}


interface CaosScriptArgumentStub {
    val index:Int
    val caosVar:CaosExpressionValueType
    val expectedType:CaosExpressionValueType
}

interface CaosScriptAssignmentStub : StubElement<CaosScriptCAssignmentImpl> {
    val fileName:String
    val operation: CaosOp
    val lvalue:CaosExpressionValueType?
    val rvalue:List<CaosExpressionValueType>?
//    val enclosingScope:CaosScope
    val commandString:String
}

interface CaosScriptBlockStub {
    val range:TextRange
    val enclosingScope:CaosScope
    val blockType:CaosScriptBlockType
}

interface CaosScriptDoIfStub : StubElement<CaosScriptDoifStatementStatementImpl>, CaosScriptBlockStub {
    val condition:CaosBlockCondition
}

interface CaosScriptElIfStub : StubElement<CaosScriptElseIfStatementImpl>, CaosScriptBlockStub  {
    val condition:CaosBlockCondition
}

interface CaosScriptElseStub : StubElement<CaosScriptElseStatementImpl>, CaosScriptBlockStub

interface CaosScriptEnumBlockStub : CaosScriptBlockStub {
    val family:Int
    val genus:Int
    val species:Int
}

interface CaosScriptEscnStub : StubElement<CaosScriptEnumSceneryStatementImpl>, CaosScriptEnumBlockStub

interface CaosScriptEnumNextStub : StubElement<CaosScriptEnumNextStatementImpl>, CaosScriptEnumBlockStub

interface CaosScriptLoopStub : StubElement<CaosScriptLoopStatementImpl>, CaosScriptBlockStub

interface CaosScriptMacroStub : StubElement<CaosScriptMacroImpl>

interface CaosScriptInstallScriptStub : StubElement<CaosScriptInstallScriptImpl>

interface CaosScriptRemovalScriptStub : StubElement<CaosScriptRemovalScriptImpl>

interface CaosScriptEventScriptStub : StubElement<CaosScriptEventScriptImpl> {
    val family:Int
    val genus:Int
    val species:Int
    val eventNumber:Int
}

interface CaosScriptNamedGameVarStub : StubElement<CaosScriptNamedGameVarImpl> {
    val type: CaosScriptNamedGameVarType
    val key:String
    val keyType:List<CaosExpressionValueType>?
}

interface CaosScriptRepsStub : StubElement<CaosScriptRepeatStatementImpl>, CaosScriptBlockStub {
    val reps:CaosExpressionValueType
}

interface CaosScriptSubroutineStub : StubElement<CaosScriptSubroutineImpl> {
    val name:String
}

interface CaosScriptVarTokenStub : StubElement<CaosScriptVarTokenImpl> {
    val varGroup:CaosScriptVarTokenGroup
    val varIndex:Int?
}

interface CaosScriptCaos2BlockStub:StubElement<CaosScriptCaos2BlockImpl> {
    val isCaos2Pray:Boolean
    val isCaos2Cob:Boolean
    val caos2Variants: List<CaosVariant>
    val agentBlockNames: List<Pair<String, String>>
    val tags:Map<String,String>
    val cobTags:Map<CobTag, String>
    val commands:List<Pair<String,List<String>>>
}

interface CaosScriptCaos2TagStub:StubElement<CaosScriptCaos2TagImpl> {
    val rawValue: String
    val tagName:String
    val value:String?
    val isStringValue: Boolean
}

interface CaosScriptCaos2CommandStub:StubElement<CaosScriptCaos2CommandImpl> {
    val commandName:String
    val args:List<String>
}

interface CaosScriptQuoteStringLiteralStub: StubElement<CaosScriptQuoteStringLiteralImpl> {
    val kind: StringStubKind?
    val value: String
    val meta: Int
}

interface CaosScriptCaos2ValueTokenStub: StubElement<CaosScriptCaos2ValueTokenImpl> {
    val kind: StringStubKind?
    val value: String
}
