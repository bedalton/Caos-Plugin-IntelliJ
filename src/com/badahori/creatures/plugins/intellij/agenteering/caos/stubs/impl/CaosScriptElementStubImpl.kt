package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl

import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosNumber
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosOp
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosScope
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosVar
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptVarTokenGroup
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptNamedGameVarType
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.CaosScriptStubTypes

class CaosScriptSubroutineStubImpl(
        parent:StubElement<*>?,
        override val name:String
) : StubBase<CaosScriptSubroutineImpl>(parent, CaosScriptStubTypes.SUBROUTINE), CaosScriptSubroutineStub

class CaosScriptCommandCallStubImpl(
        parent:StubElement<*>?,
        override val command:String,
        override val argumentValues: List<CaosVar>
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
        override val caosVar: CaosVar,
        override val argumentValues: List<CaosVar>
) : StubBase<CaosScriptLvalueImpl>(parent, CaosScriptStubTypes.LVALUE),  CaosScriptLValueStub {
    override val commandString: String? by lazy {
        (caosVar as? CaosVar.CaosCommandCall)?.text?.toUpperCase()
    }
}

data class CaosScriptRValueStubImpl(
        val parent: StubElement<*>?,
        override val caosVar: CaosVar,
        override val argumentValues: List<CaosVar>
) : StubBase<CaosScriptRvalueImpl>(parent, CaosScriptStubTypes.RVALUE),  CaosScriptRValueStub {

    override val commandString: String? by lazy {
        (caosVar as? CaosVar.CaosCommandCall)?.text?.toUpperCase()
    }
}

data class CaosScriptTokenRValueStubImpl(
        val parent: StubElement<*>?,
        override val caosVar: CaosVar,
        override val argumentValues: List<CaosVar>
) : StubBase<CaosScriptTokenRvalueImpl>(parent, CaosScriptStubTypes.TOKEN_RVALUE),  CaosScriptTokenRValueStub {

    override val commandString: String? get() = null
}

data class CaosScriptRValuePrimeStubImpl(
        val parent: StubElement<*>?,
        override val caosVar: CaosVar,
        override val argumentValues: List<CaosVar>
) : StubBase<CaosScriptRvaluePrimeImpl>(parent, CaosScriptStubTypes.RVALUE_PRIME),  CaosScriptRValuePrimeStub {

    override val commandString: String? by lazy {
        (caosVar as? CaosVar.CaosCommandCall)?.text?.toUpperCase()
    }
}


data class CaosScriptRndvStubImpl(
        val parent: StubElement<*>?,
        override val min:Int?,
        override val max:Int?
) : StubBase<CaosScriptCRndvImpl>(parent, CaosScriptStubTypes.RNDV), CaosScriptRndvStub


data class CaosScriptExpectsIntStubImpl(
        val parent:StubElement<*>?,
        override val index:Int,
        override val caosVar: CaosVar
) : StubBase<CaosScriptExpectsIntImpl>(parent, CaosScriptStubTypes.EXPECTS_INT), CaosScriptExpectsIntStub {
    override val expectedType: CaosExpressionValueType
        get() =  CaosExpressionValueType.INT
}

data class CaosScriptExpectsFloatStubImpl(
        val parent:StubElement<*>?,
        override val index:Int,
        override val caosVar: CaosVar
) : StubBase<CaosScriptExpectsFloatImpl>(parent, CaosScriptStubTypes.EXPECTS_FLOAT), CaosScriptExpectsFloatStub {
    override val expectedType: CaosExpressionValueType
        get() =  CaosExpressionValueType.FLOAT
}

data class CaosScriptExpectsQuoteStringStubImpl(
        val parent:StubElement<*>?,
        override val index:Int,
        override val caosVar: CaosVar
) : StubBase<CaosScriptExpectsQuoteStringImpl>(parent, CaosScriptStubTypes.EXPECTS_QUOTE_STRING), CaosScriptExpectsQuoteStringStub {
    override val expectedType: CaosExpressionValueType
        get() =  CaosExpressionValueType.STRING
}


data class CaosScriptExpectsC1StringStubImpl(
        val parent:StubElement<*>?,
        override val index:Int,
        override val caosVar: CaosVar
) : StubBase<CaosScriptExpectsC1StringImpl>(parent, CaosScriptStubTypes.EXPECTS_C1_STRING), CaosScriptExpectsC1StringStub {
    override val expectedType: CaosExpressionValueType
        get() =  CaosExpressionValueType.C1_STRING
}

data class CaosScriptExpectsTokenStubImpl(
        val parent:StubElement<*>?,
        override val index:Int,
        override val caosVar: CaosVar
) : StubBase<CaosScriptExpectsTokenImpl>(parent, CaosScriptStubTypes.EXPECTS_TOKEN), CaosScriptExpectsTokenStub {
    override val expectedType: CaosExpressionValueType
        get() =  CaosExpressionValueType.TOKEN
}

data class CaosScriptExpectsDecimalStubImpl(
        val parent:StubElement<*>?,
        override val index:Int,
        override val caosVar: CaosVar
) : StubBase<CaosScriptExpectsDecimalImpl>(parent, CaosScriptStubTypes.EXPECTS_DECIMAL), CaosScriptExpectsDecimalStub {
    override val expectedType: CaosExpressionValueType
        get() =  CaosExpressionValueType.DECIMAL
}

data class CaosScriptExpectsByteStringStubImpl(
        val parent:StubElement<*>?,
        override val index:Int,
        override val caosVar: CaosVar
) : StubBase<CaosScriptExpectsByteStringImpl>(parent, CaosScriptStubTypes.EXPECTS_BYTE_STRING), CaosScriptExpectsByteStringStub {
    override val expectedType: CaosExpressionValueType
        get() =  CaosExpressionValueType.BYTE_STRING
}

data class CaosScriptExpectsAgentStubImpl(
        val parent:StubElement<*>?,
        override val index:Int,
        override val caosVar: CaosVar
) : StubBase<CaosScriptExpectsAgentImpl>(parent, CaosScriptStubTypes.EXPECTS_AGENT), CaosScriptExpectsAgentStub {
    override val expectedType: CaosExpressionValueType
        get() =  CaosExpressionValueType.AGENT
}

data class CaosScriptExpectsValueStubImpl(
        val parent:StubElement<*>?,
        override val index:Int,
        override val caosVar: CaosVar
) : StubBase<CaosScriptExpectsValueImpl>(parent, CaosScriptStubTypes.EXPECTS_VALUE), CaosScriptExpectsValueStub {
    override val expectedType: CaosExpressionValueType
        get() =  CaosExpressionValueType.ANY
}

class CaosScriptNamedGameVarStubImpl(
        parent: StubElement<*>?,
        override val type: CaosScriptNamedGameVarType,
        override val name: String,
        override val key:CaosVar
) : StubBase<CaosScriptNamedGameVarImpl>(parent, CaosScriptStubTypes.NAMED_GAME_VAR), CaosScriptNamedGameVarStub


class CaosScriptConstantAssignmentStubImpl(
        parent: StubElement<*>?,
        override val name:String,
        override val value:CaosNumber
) : StubBase<CaosScriptConstantAssignmentImpl>(parent, CaosScriptStubTypes.CONSTANT_ASSIGNMENT), CaosScriptConstantAssignmentStub

class CaosScriptNamedVarAssignmentStubImpl(
        parent: StubElement<*>?,
        override val name:String,
        override val value:CaosVar?
) : StubBase<CaosScriptNamedVarAssignmentImpl>(parent, CaosScriptStubTypes.NAMED_VAR_ASSIGNMENT), CaosScriptNamedVarAssignmentStub

class CaosScriptNamedConstantStubImpl(
        parent: StubElement<*>?,
        override val name: String,
        override val scope: CaosScope
) : StubBase<CaosScriptNamedConstantImpl>(parent, CaosScriptStubTypes.NAMED_CONSTANT), CaosScriptNamedConstantStub

class CaosScriptNamedVarStubImpl(
        parent:StubElement<*>?,
        override val name:String,
        override val scope: CaosScope
) : StubBase<CaosScriptNamedVarImpl>(parent, CaosScriptStubTypes.NAMED_VAR), CaosScriptNamedVarStub

class CaosScriptAssignmentStubImpl(
        parent: StubElement<*>?,
        override val fileName:String,
        override val operation:CaosOp,
        override val rvalue:CaosVar?,
        override val lvalue:CaosVar?,
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


data class CaosScriptConstantAssignmentStruct(
        val name:String,
        val value:CaosNumber
)


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
