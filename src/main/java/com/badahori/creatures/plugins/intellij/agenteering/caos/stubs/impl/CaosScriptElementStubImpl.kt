package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.impl

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosOp
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosScope
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.types.CaosScriptVarTokenGroup
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosScriptNamedGameVarType
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptQuoteStringLiteral
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CobTag
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.CaosScriptStubTypes
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement

class CaosScriptSubroutineStubImpl(
    parent: StubElement<*>?,
    override val name: String
) : StubBase<CaosScriptSubroutineImpl>(parent, CaosScriptStubTypes.SUBROUTINE), CaosScriptSubroutineStub

class CaosScriptCommandCallStubImpl(
    parent: StubElement<*>?,
    override val command: String,
    override val argumentValues: List<CaosExpressionValueType>
) : StubBase<CaosScriptCommandCallImpl>(parent, CaosScriptStubTypes.COMMAND_CALL), CaosScriptCommandCallStub {
    override val commandUpper: String by lazy {
        command.uppercase()
    }
    override val commandTokens: List<String> by lazy {
        command.split(" ")
    }
}

data class CaosScriptLValueStubImpl(
    val parent: StubElement<*>?,
    override val commandString: String?,
    override val type: List<CaosExpressionValueType>,
    override val argumentValues: List<CaosExpressionValueType>
) : StubBase<CaosScriptLvalueImpl>(parent, CaosScriptStubTypes.LVALUE), CaosScriptLValueStub

data class CaosScriptRValueStubImpl(
    val parent: StubElement<*>?,
    override val commandString: String?,
    override val type: List<CaosExpressionValueType>,
    override val argumentValues: List<CaosExpressionValueType>,
    override val stringStubKind: StringStubKind?
) : StubBase<CaosScriptRvalueImpl>(parent, CaosScriptStubTypes.RVALUE), CaosScriptRValueStub

data class CaosScriptTokenRValueStubImpl(
    val parent: StubElement<*>?,
    override val tokenText: String?,
    override val stringStubKind: StringStubKind?
) : StubBase<CaosScriptTokenRvalueImpl>(parent, CaosScriptStubTypes.TOKEN_RVALUE), CaosScriptTokenRValueStub

data class CaosScriptSubroutineNameStubImpl(
    val parent: StubElement<*>?,
    override val tokenText: String?
) : StubBase<CaosScriptSubroutineNameImpl>(parent, CaosScriptStubTypes.SUBROUTINE_NAME), CaosScriptSubroutineNameStub

data class CaosScriptRValuePrimeStubImpl(
    val parent: StubElement<*>?,
    override val commandString: String?,
    override val caosVar: CaosExpressionValueType,
    override val argumentValues: List<CaosExpressionValueType>
) : StubBase<CaosScriptRvaluePrimeImpl>(parent, CaosScriptStubTypes.RVALUE_PRIME), CaosScriptRValuePrimeStub


data class CaosScriptRndvStubImpl(
    val parent: StubElement<*>?,
    override val min: Int?,
    override val max: Int?
) : StubBase<CaosScriptCRndvImpl>(parent, CaosScriptStubTypes.RNDV), CaosScriptRndvStub


class CaosScriptNamedGameVarStubImpl(
    parent: StubElement<*>?,
    override val type: CaosScriptNamedGameVarType,
    override val key: String,
    override val keyType: List<CaosExpressionValueType>?
) : StubBase<CaosScriptNamedGameVarImpl>(parent, CaosScriptStubTypes.NAMED_GAME_VAR), CaosScriptNamedGameVarStub


class CaosScriptAssignmentStubImpl(
    parent: StubElement<*>?,
    override val fileName: String,
    override val operation: CaosOp,
    override val rvalue: List<CaosExpressionValueType>?,
    override val lvalue: CaosExpressionValueType?,
//        override val enclosingScope:CaosScope,
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
    parent: StubElement<*>?,
    override val varGroup: CaosScriptVarTokenGroup,
    override val varIndex: Int?
) : StubBase<CaosScriptVarTokenImpl>(parent, CaosScriptStubTypes.VAR_TOKEN), CaosScriptVarTokenStub

class CaosScriptTargAssignmentStubImpl(
    parent: StubElement<*>?,
    override val scope: CaosScope,
    override val rvalue: List<CaosExpressionValueType>?
) : StubBase<CaosScriptCTargImpl>(parent, CaosScriptStubTypes.TARG_ASSIGNMENT), CaosScriptTargAssignmentStub


class CaosScriptCaos2BlockStubImpl(
    parent: StubElement<*>?,
    override val isCaos2Pray: Boolean,
    override val isCaos2Cob: Boolean,
    override val tags: Map<String, String>,
    override val commands: List<Pair<String, List<String>>>,
    override val agentBlockNames: List<Pair<String, String>>,
    override val caos2Variants: List<CaosVariant>
) : StubBase<CaosScriptCaos2BlockImpl>(parent, CaosScriptStubTypes.CAOS_2_BLOCK), CaosScriptCaos2BlockStub {
    override val cobTags: Map<CobTag, String> by lazy {
        tags.mapNotNull { (key, value) ->
            CobTag.fromString(key)?.let { cobKey ->
                cobKey to value
            }
        }.toMap()
    }
}

class CaosScriptQuoteStringLiteralStubImpl(
    parent: StubElement<*>,
    override val kind: StringStubKind?,
    override val value: String,
    override val meta: Int
) : StubBase<CaosScriptQuoteStringLiteralImpl>(parent, CaosScriptStubTypes.QUOTE_STRING_LITERAL),
    CaosScriptQuoteStringLiteralStub

class CaosScriptCaos2ValueTokenStubImpl(
    parent: StubElement<*>,
    override val kind: StringStubKind?,
    override val value: String,
) : StubBase<CaosScriptCaos2ValueTokenImpl>(
    parent,
    CaosScriptStubTypes.CAOS_2_VALUE_TOKEN
), CaosScriptCaos2ValueTokenStub

class CaosScriptCaos2TagStubImpl(
    parent: StubElement<*>?,
    override val tagName: String,
    override val rawValue: String,
    override val value: String?,
    override val isStringValue: Boolean
) : StubBase<CaosScriptCaos2TagImpl>(parent, CaosScriptStubTypes.CAOS_2_TAG), CaosScriptCaos2TagStub

class CaosScriptCaos2CommandStubImpl(
    parent: StubElement<*>?,
    override val commandName: String,
    override val args: List<String>
) : StubBase<CaosScriptCaos2CommandImpl>(parent, CaosScriptStubTypes.CAOS_2_COMMAND), CaosScriptCaos2CommandStub