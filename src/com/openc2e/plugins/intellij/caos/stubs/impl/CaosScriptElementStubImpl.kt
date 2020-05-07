package com.openc2e.plugins.intellij.caos.stubs.impl

import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
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

class CaosScriptExpressionStubImpl(
        parent:StubElement<*>?,
        override val type: CaosScriptExpressionType,
        override val text: String
): StubBase<CaosScriptExpressionImpl>(parent, CaosScriptStubTypes.EXPRESSION), CaosScriptExpressionStub

class CaosScriptVarTokenStubImpl(
        parent:StubElement<*>?,
        override val varGroup: CaosScriptVarTokenGroup,
        override val varIndex: Int?
) : StubBase<CaosScriptVarTokenImpl>(parent, CaosScriptStubTypes.VAR_TOKEN), CaosScriptVarTokenStub