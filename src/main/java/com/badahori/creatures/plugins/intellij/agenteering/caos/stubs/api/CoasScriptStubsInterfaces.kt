package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptVarTokenGroup
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptNamedGameVarType
import com.intellij.openapi.util.TextRange
import com.intellij.psi.stubs.StubElement

interface CaosScriptCommandCallStub : StubElement<CaosScriptCommandCallImpl> {
    val command:String
    val commandUpper:String
    val commandTokens:List<String>
    val argumentValues:List<CaosVar>
    val numArguments:Int get() = argumentValues.size
}

interface CaosScriptTargAssignmentStub : StubElement<CaosScriptCTargImpl> {
    val scope:CaosScope
    val rvalue:CaosVar?
}

interface CaosScriptRtarAssignmentStub : StubElement<CaosScriptCTargImpl> {
    val scope:CaosScope
    val family:CaosVar?
    val genus:CaosVar?
    val species:CaosVar?
}

interface CaosScriptLValueStub : StubElement<CaosScriptLvalueImpl> {
    val caosVar:CaosVar
    val argumentValues: List<CaosVar>
    val commandString:String?
}

interface CaosScriptRValueStub : StubElement<CaosScriptRvalueImpl> {
    val caosVar:CaosVar
    val argumentValues: List<CaosVar>
    val commandString:String?
}
interface CaosScriptTokenRValueStub : StubElement<CaosScriptTokenRvalueImpl> {
    val caosVar:CaosVar
    val argumentValues: List<CaosVar>
    val commandString:String?
}

interface CaosScriptRValuePrimeStub : StubElement<CaosScriptRvaluePrimeImpl> {
    val caosVar:CaosVar
    val argumentValues: List<CaosVar>
    val commandString:String?
}


interface CaosScriptRndvStub : StubElement<CaosScriptCRndvImpl> {
    val min:Int?
    val max:Int?
}


interface CaosScriptArgumentStub {
    val index:Int
    val caosVar:CaosVar
    val expectedType:CaosExpressionValueType
}

interface CaosScriptAssignmentStub : StubElement<CaosScriptCAssignmentImpl> {
    val fileName:String
    val operation: CaosOp
    val lvalue:CaosVar?
    val rvalue:CaosVar?
    val enclosingScope:CaosScope
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

interface CaosScriptLoopStub : StubElement<CaosScriptLoopStatementImpl>, CaosScriptBlockStub {
    val loopCondition:CaosLoopCondition?
}

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
    val name:String
    val key:CaosVar
}

interface CaosScriptRepsStub : StubElement<CaosScriptRepeatStatementImpl>, CaosScriptBlockStub {
    val reps:CaosVar
}

interface CaosScriptSubroutineStub : StubElement<CaosScriptSubroutineImpl> {
    val name:String
}

interface CaosScriptVarTokenStub : StubElement<CaosScriptVarTokenImpl> {
    val varGroup:CaosScriptVarTokenGroup
    val varIndex:Int?
}