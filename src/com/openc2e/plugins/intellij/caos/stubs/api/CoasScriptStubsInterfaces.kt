package com.openc2e.plugins.intellij.caos.stubs.api

import com.intellij.openapi.util.TextRange
import com.intellij.psi.stubs.StubElement
import com.openc2e.plugins.intellij.caos.deducer.*
import com.openc2e.plugins.intellij.caos.psi.impl.*
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptExpressionType
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptVarTokenGroup

interface CaosScriptCommandCallStub : StubElement<CaosScriptCommandCallImpl> {
    val command:String
    val commandTokens:List<String>
    val parameterTypes:List<CaosScriptExpressionType>
    val numParameters:Int
}

interface CaosScriptLValueStub : StubElement<CaosScriptLvalueImpl> {
    val caosVar:CaosVar
}

interface CaosScriptRValueStub : StubElement<CaosScriptRvalueImpl> {
    val caosVar:CaosVar
}

interface CaosScriptConstStub : StubElement<CaosScriptConstantAssignmentImpl> {
    val name:String
    val caosVar:CaosVar
}

interface CaosScriptNamedVarStub : StubElement<CaosScriptNamedVarImpl> {
    val name:String
    val caosVar:CaosVar
}

interface CaosScriptAssignmentStub : StubElement<CaosScriptCAssignmentImpl> {
    val operation: CaosOp
    val lvalue:CaosVar
    val rvalue:CaosVar
    val enclosingScope:List<CaosScope>
}

interface CaosScriptBlockStub {
    val range:TextRange
    val enclosingScope:List<CaosScope>
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

interface CaosScriptMacroStub : StubElement<CaosScriptMacroImpl>, CaosScriptBlockStub

interface CaosScriptEventScriptStub : StubElement<CaosScriptEventScriptImpl>, CaosScriptBlockStub {
    val family:Int
    val genus:Int
    val species:Int
    val event:Int
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