package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosOp
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosScope
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptVarTokenGroup
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosScriptNamedGameVarType
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.CaosScriptStubTypes
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement

class CaosScriptSubroutineStubImpl(
        parent:StubElement<*>?,
        override val name:String
) : StubBase<CaosScriptSubroutineImpl>(parent, CaosScriptStubTypes.SUBROUTINE), CaosScriptSubroutineStub

class CaosScriptCommandCallStubImpl(
        parent:StubElement<*>?,
        override val command:String,
        override val argumentValues: List<CaosExpressionValueType>
) : StubBase<CaosScriptCommandCallImpl>(parent, CaosScriptStubTypes.COMMAND_CALL), CaosScriptCommandCallStub {
    override val commandUpper: String by lazy {
        command.toUpperCase()
    }
    override val commandTokens: List<String> by lazy {
        command.split(" ")
    }
}

data class CaosScriptLValueStubImpl(
        val parent: StubElement<*>?,
        override val commandString: String?,
        override val type: CaosExpressionValueType,
        override val argumentValues: List<CaosExpressionValueType>
) : StubBase<CaosScriptLvalueImpl>(parent, CaosScriptStubTypes.LVALUE),  CaosScriptLValueStub

data class CaosScriptRValueStubImpl(
        val parent: StubElement<*>?,
        override val commandString: String?,
        override val type: CaosExpressionValueType,
        override val argumentValues: List<CaosExpressionValueType>
) : StubBase<CaosScriptRvalueImpl>(parent, CaosScriptStubTypes.RVALUE),  CaosScriptRValueStub

data class CaosScriptTokenRValueStubImpl(
        val parent: StubElement<*>?,
        override val tokenText: String?
) : StubBase<CaosScriptTokenRvalueImpl>(parent, CaosScriptStubTypes.TOKEN_RVALUE),  CaosScriptTokenRValueStub

data class CaosScriptRValuePrimeStubImpl(
        val parent: StubElement<*>?,
        override val commandString: String?,
        override val caosVar: CaosExpressionValueType,
        override val argumentValues: List<CaosExpressionValueType>
) : StubBase<CaosScriptRvaluePrimeImpl>(parent, CaosScriptStubTypes.RVALUE_PRIME),  CaosScriptRValuePrimeStub


data class CaosScriptRndvStubImpl(
        val parent: StubElement<*>?,
        override val min:Int?,
        override val max:Int?
) : StubBase<CaosScriptCRndvImpl>(parent, CaosScriptStubTypes.RNDV), CaosScriptRndvStub


class CaosScriptNamedGameVarStubImpl(
        parent: StubElement<*>?,
        override val type: CaosScriptNamedGameVarType,
        override val key: String,
        override val keyType: CaosExpressionValueType
) : StubBase<CaosScriptNamedGameVarImpl>(parent, CaosScriptStubTypes.NAMED_GAME_VAR), CaosScriptNamedGameVarStub


class CaosScriptAssignmentStubImpl(
        parent: StubElement<*>?,
        override val fileName:String,
        override val operation:CaosOp,
        override val rvalue:CaosExpressionValueType?,
        override val lvalue:CaosExpressionValueType?,
        override val enclosingScope:CaosScope,
        override val commandString: String
) : StubBase<CaosScriptCAssignmentImpl>(parent, CaosScriptStubTypes.VAR_ASSIGNMENT), CaosScriptAssignmentStub

/*

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
) : StubBase<CaosScriptRepeatStatementImpl>(parent, CaosScriptStubTypes.REPS_STUB), CaosScriptRepsStub
*/
class CaosScriptEventScriptStubImpl(
        parent: StubElement<*>?,
        override val family: Int,
        override val genus: Int,
        override val species: Int,
        override val eventNumber: Int
) : StubBase<CaosScriptEventScriptImpl>(parent, CaosScriptStubTypes.EVENT_SCRIPT), CaosScriptEventScriptStub

class CaosScriptMacroStubImpl(
        parent: StubElement<*>?
) : StubBase<CaosScriptMacroImpl>(parent, CaosScriptStubTypes.MACRO), CaosScriptMacroStub

class CaosScriptInstallScriptStubImpl(
        parent: StubElement<*>?
) : StubBase<CaosScriptInstallScriptImpl>(parent, CaosScriptStubTypes.INSTALL_SCRIPT), CaosScriptInstallScriptStub


class CaosScriptRemovalScriptStubImpl(
        parent: StubElement<*>?
) : StubBase<CaosScriptRemovalScriptImpl>(parent, CaosScriptStubTypes.REMOVAL_SCRIPT), CaosScriptRemovalScriptStub


class CaosScriptVarTokenStubImpl(
        parent:StubElement<*>?,
        override val varGroup: CaosScriptVarTokenGroup,
        override val varIndex: Int?
) : StubBase<CaosScriptVarTokenImpl>(parent, CaosScriptStubTypes.VAR_TOKEN), CaosScriptVarTokenStub

class CaosScriptTargAssignmentStubImpl(
        parent:StubElement<*>?,
        override val scope: CaosScope,
        override val rvalue: CaosExpressionValueType?
) : StubBase<CaosScriptCTargImpl>(parent, CaosScriptStubTypes.TARG_ASSIGNMENT), CaosScriptTargAssignmentStub
