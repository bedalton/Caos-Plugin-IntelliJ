package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api

import com.intellij.openapi.util.TextRange
import com.intellij.psi.stubs.StubElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptVarTokenGroup
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptNamedGameVarType

interface CaosScriptCommandCallStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptCommandCallImpl> {
    val command:String
    val commandUpper:String
    val commandTokens:List<String>
    val argumentValues:List<CaosVar>
    val numArguments:Int get() = argumentValues.size
}

interface CaosScriptTargAssignmentStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptCTargImpl> {
    val scope:CaosScope
    val rvalue:CaosVar?
}

interface CaosScriptLValueStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptLvalueImpl> {
    val caosVar:CaosVar
    val argumentValues: List<CaosVar>
    val commandString:String?
}

interface CaosScriptRValueStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptRvalueImpl> {
    val caosVar:CaosVar
    val argumentValues: List<CaosVar>
    val commandString:String?
}

interface CaosScriptRValuePrimeStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptRvaluePrimeImpl> {
    val caosVar:CaosVar
    val argumentValues: List<CaosVar>
    val commandString:String?
}


interface CaosScriptRndvStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptCRndvImpl> {
    val min:Int?
    val max:Int?
}


interface CaosScriptArgumentStub {
    val index:Int
    val caosVar:CaosVar
    val expectedType:CaosExpressionValueType
}

interface CaosScriptExpectsIntStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptExpectsIntImpl>, CaosScriptArgumentStub
interface CaosScriptExpectsFloatStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptExpectsFloatImpl>, CaosScriptArgumentStub
interface CaosScriptExpectsQuoteStringStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptExpectsQuoteStringImpl>, CaosScriptArgumentStub
interface CaosScriptExpectsC1StringStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptExpectsC1StringImpl>, CaosScriptArgumentStub
interface CaosScriptExpectsByteStringStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptExpectsByteStringImpl>, CaosScriptArgumentStub
interface CaosScriptExpectsTokenStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptExpectsTokenImpl>, CaosScriptArgumentStub
interface CaosScriptExpectsAgentStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptExpectsAgentImpl>, CaosScriptArgumentStub
interface CaosScriptExpectsValueStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptExpectsValueImpl>, CaosScriptArgumentStub
interface CaosScriptExpectsDecimalStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptExpectsDecimalImpl>, CaosScriptArgumentStub

interface CaosScriptConstantAssignmentStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptConstantAssignmentImpl> {
    val name:String
    val value:CaosNumber
}

interface CaosScriptNamedVarAssignmentStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptNamedVarAssignmentImpl> {
    val name:String
    val value:CaosVar?
}

interface CaosScriptNamedConstantStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptNamedConstantImpl> {
    val name:String
    val scope:CaosScope
}

interface CaosScriptNamedVarStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptNamedVarImpl> {
    val name:String
    val scope:CaosScope
}

interface CaosScriptAssignmentStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptCAssignmentImpl> {
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

interface CaosScriptDoIfStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptDoifStatementStatementImpl>, CaosScriptBlockStub {
    val condition:CaosBlockCondition
}

interface CaosScriptElIfStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptElseIfStatementImpl>, CaosScriptBlockStub  {
    val condition:CaosBlockCondition
}

interface CaosScriptElseStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptElseStatementImpl>, CaosScriptBlockStub

interface CaosScriptEnumBlockStub : CaosScriptBlockStub {
    val family:Int
    val genus:Int
    val species:Int
}

interface CaosScriptEscnStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptEnumSceneryStatementImpl>, CaosScriptEnumBlockStub

interface CaosScriptEnumNextStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptEnumNextStatementImpl>, CaosScriptEnumBlockStub

interface CaosScriptLoopStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptLoopStatementImpl>, CaosScriptBlockStub {
    val loopCondition:CaosLoopCondition?
}

interface CaosScriptMacroStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptMacroImpl>

interface CaosScriptEventScriptStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptEventScriptImpl> {
    val family:Int
    val genus:Int
    val species:Int
    val eventNumber:Int
}

interface CaosScriptNamedGameVarStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptNamedGameVarImpl> {
    val type: CaosScriptNamedGameVarType
    val name:String
    val key:CaosVar
}

interface CaosScriptRepsStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptRepeatStatementImpl>, CaosScriptBlockStub {
    val reps:CaosVar
}

interface CaosScriptSubroutineStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptSubroutineImpl> {
    val name:String
}

interface CaosScriptVarTokenStub : StubElement<com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptVarTokenImpl> {
    val varGroup:CaosScriptVarTokenGroup
    val varIndex:Int?
}