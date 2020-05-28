package com.openc2e.plugins.intellij.caos.stubs.api

import com.intellij.openapi.util.TextRange
import com.intellij.psi.stubs.StubElement
import com.openc2e.plugins.intellij.caos.deducer.*
import com.openc2e.plugins.intellij.caos.psi.api.CaosExpressionValueType
import com.openc2e.plugins.intellij.caos.psi.impl.*
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptVarTokenGroup
import com.openc2e.plugins.intellij.caos.psi.util.CaosScriptNamedGameVarType

interface CaosScriptCommandCallStub : StubElement<CaosScriptCommandCallImpl> {
    val command:String
    val commandTokens:List<String>
    val argumentValues:List<CaosVar>
    val numArguments:Int get() = argumentValues.size
}

interface CaosScriptTargAssignmentStub : StubElement<CaosScriptCTargImpl> {
    val scope:CaosScope
    val rvalue:CaosVar?
}

interface CaosScriptLValueStub : StubElement<CaosScriptLvalueImpl> {
    val caosVar:CaosVar
    val argumentValues: List<CaosVar>
}

interface CaosScriptRValueStub : StubElement<CaosScriptRvalueImpl> {
    val caosVar:CaosVar
    val argumentValues: List<CaosVar>
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

interface CaosScriptExpectsIntStub : StubElement<CaosScriptExpectsIntImpl>, CaosScriptArgumentStub
interface CaosScriptExpectsFloatStub : StubElement<CaosScriptExpectsFloatImpl>, CaosScriptArgumentStub
interface CaosScriptExpectsQuoteStringStub : StubElement<CaosScriptExpectsQuoteStringImpl>, CaosScriptArgumentStub
interface CaosScriptExpectsC1StringStub : StubElement<CaosScriptExpectsC1StringImpl>, CaosScriptArgumentStub
interface CaosScriptExpectsByteStringStub : StubElement<CaosScriptExpectsByteStringImpl>, CaosScriptArgumentStub
interface CaosScriptExpectsTokenStub : StubElement<CaosScriptExpectsTokenImpl>, CaosScriptArgumentStub
interface CaosScriptExpectsAgentStub : StubElement<CaosScriptExpectsAgentImpl>, CaosScriptArgumentStub
interface CaosScriptExpectsValueStub : StubElement<CaosScriptExpectsValueImpl>, CaosScriptArgumentStub
interface CaosScriptExpectsDecimalStub : StubElement<CaosScriptExpectsDecimalImpl>, CaosScriptArgumentStub

interface CaosScriptConstantAssignmentStub : StubElement<CaosScriptConstantAssignmentImpl> {
    val name:String
    val value:CaosNumber
}

interface CaosScriptNamedVarAssignmentStub : StubElement<CaosScriptNamedVarAssignmentImpl> {
    val name:String
    val value:CaosVar?
}

interface CaosScriptNamedConstantStub : StubElement<CaosScriptNamedConstantImpl> {
    val name:String
    val scope:CaosScope
}

interface CaosScriptNamedVarStub : StubElement<CaosScriptNamedVarImpl> {
    val name:String
    val scope:CaosScope
}

interface CaosScriptAssignmentStub : StubElement<CaosScriptCAssignmentImpl> {
    val fileName:String
    val operation: CaosOp
    val lvalue:CaosVar?
    val rvalue:CaosVar?
    val enclosingScope:CaosScope
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