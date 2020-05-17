package com.openc2e.plugins.intellij.caos.stubs.impl

import com.intellij.openapi.util.TextRange
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.openc2e.plugins.intellij.caos.deducer.*
import com.openc2e.plugins.intellij.caos.psi.impl.*
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptExpressionType
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptVarTokenGroup
import com.openc2e.plugins.intellij.caos.stubs.api.*
import com.openc2e.plugins.intellij.caos.stubs.types.CaosScriptStubTypes

class CaosScriptSubroutineStubImpl(
        parent:StubElement<*>?,
        override val name:String
) : StubBase<CaosScriptSubroutineImpl>(parent, CaosScriptStubTypes.SUBROUTINE), CaosScriptSubroutineStub {
}

class CaosScriptCommandCallStubImpl(
        parent:StubElement<*>?,
        override val commandTokens:List<String>,
        override val numParameters: Int,
        override val parameterTypes: List<CaosScriptExpressionType>
) : StubBase<CaosScriptCommandCallImpl>(parent, CaosScriptStubTypes.COMMAND_CALL), CaosScriptCommandCallStub {
    override val command:String by lazy {
        commandTokens.joinToString(" ")
    }
}


data class CaosScriptLValueStubImpl(val parent: StubElement<*>?, override val caosVar: CaosVar) : StubBase<CaosScriptLvalueImpl>(parent, CaosScriptStubTypes.LVALUE),  CaosScriptLValueStub

data class CaosScriptRValueStubImpl(val parent: StubElement<*>?, override val caosVar: CaosVar) : StubBase<CaosScriptRvalueImpl>(parent, CaosScriptStubTypes.RVALUE),  CaosScriptRValueStub

class CaosScriptConstStubImpl(
        parent: StubElement<*>?,
        override val name:String,
        override val caosVar: CaosVar
) : StubBase<CaosScriptConstantAssignmentImpl>(parent, CaosScriptStubTypes.CONST), CaosScriptConstStub

class CaosScriptNamedVar(
        parent: StubElement<*>?,
        override val name:String,
        override val caosVar: CaosVar
) : StubBase<CaosScriptNamedVarImpl>(parent, CaosScriptStubTypes.NAMED_VAR), CaosScriptNamedVarStub

class CaosScriptAssignmentStubImpl(
        parent: StubElement<*>?,
        override val operation:CaosOp,
        override val rvalue:CaosVar?,
        override val lvalue:CaosVar?,
        override val enclosingScope:CaosScope
) : StubBase<CaosScriptCAssignmentImpl>(parent, CaosScriptStubTypes.VAR_ASSIGNMENT), CaosScriptAssignmentStub

class CaosScriptDoIfStubImpl(
        parent: StubElement<*>?,
        override val range: TextRange,
        override val condition:CaosBlockCondition,
        override val enclosingScope: List<CaosScope>
) : StubBase<CaosScriptDoifStatementStatementImpl>(parent, CaosScriptStubTypes.DO_IF_STATEMENT), CaosScriptDoIfStub {
    override val blockType:CaosScriptBlockType get() = CaosScriptBlockType.DOIF
}

class CaosScriptElIfStubImpl(
        parent: StubElement<*>?,
        override val range: TextRange,
        override val condition:CaosBlockCondition,
        override val enclosingScope: List<CaosScope>
) : StubBase<CaosScriptElseIfStatementImpl>(parent, CaosScriptStubTypes.ELSE_IF_STATEMENT), CaosScriptElIfStub {
    override val blockType:CaosScriptBlockType get() = CaosScriptBlockType.ELIF
}

class CaosScriptElseStubImpl(
        parent: StubElement<*>?,
        override val range: TextRange,
        override val enclosingScope: List<CaosScope>
) : StubBase<CaosScriptElseStatementImpl>(parent, CaosScriptStubTypes.ELSE_STATEMENT), CaosScriptElseStub {
    override val blockType:CaosScriptBlockType get() = CaosScriptBlockType.ELSE
}

class CaosScriptLoopStubImpl(
        parent: StubElement<*>?,
        override val range: TextRange,
        override val loopCondition: CaosLoopCondition?,
        override val enclosingScope: List<CaosScope>
) : StubBase<CaosScriptLoopStatementImpl>(parent, CaosScriptStubTypes.LOOP_STATEMENT), CaosScriptLoopStub {
    override val blockType: CaosScriptBlockType get() = CaosScriptBlockType.LOOP
}

class CaosEscnStubImpl(
        parent: StubElement<*>?,
        override val range: TextRange,
        override val family: Int,
        override val genus: Int,
        override val species: Int,
        override val enclosingScope: List<CaosScope>
) : StubBase<CaosScriptEnumSceneryStatementImpl>(parent, CaosScriptStubTypes.ESCN), CaosScriptEscnStub {
    override val blockType: CaosScriptBlockType
        get() = CaosScriptBlockType.ESCN
}

class CaosEnumNextStubImpl(
        parent: StubElement<*>?,
        override val range: TextRange,
        override val family: Int,
        override val genus: Int,
        override val species: Int,
        override val enclosingScope: List<CaosScope>
) : StubBase<CaosScriptEnumNextStatementImpl>(parent, CaosScriptStubTypes.ENUM), CaosScriptEnumNextStub {
    override val blockType: CaosScriptBlockType
        get() = CaosScriptBlockType.ENUM
}

class CaosScriptEventScriptStubImpl(
        parent: StubElement<*>?,
        override val range: TextRange,
        override val family: Int,
        override val genus: Int,
        override val species: Int,
        override val event: Int,
        override val enclosingScope: List<CaosScope>
) : StubBase<CaosScriptEventScriptImpl>(parent, CaosScriptStubTypes.EVENT_SCRIPT), CaosScriptEventScriptStub {
    override val blockType: CaosScriptBlockType
        get() = CaosScriptBlockType.SCRP
}

class CaosScriptRepsStubImpl(
        parent: StubElement<*>?
) : StubBase<CaosScriptRepsStub>, CaosS


class CaosScriptVarTokenStubImpl(
        parent:StubElement<*>?,
        override val varGroup: CaosScriptVarTokenGroup,
        override val varIndex: Int?
) : StubBase<CaosScriptVarTokenImpl>(parent, CaosScriptStubTypes.VAR_TOKEN), CaosScriptVarTokenStub

class CaosScriptTargAssignmentStubImpl(
        parent:StubElement<*>?,
        override val scope: CaosScope,
        override val rvalue: CaosVar?
) : StubBase<CaosScriptCTargImpl>(parent, CaosScriptStubTypes.TARG_ASSIGNMENT), CaosScriptTargAssignmentStub
