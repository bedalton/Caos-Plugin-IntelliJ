package com.openc2e.plugins.intellij.caos.stubs.impl

import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptCommandCallImpl
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptCommandImpl
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptCommandTokenImpl
import com.openc2e.plugins.intellij.caos.psi.impl.CaosScriptExpressionImpl
import com.openc2e.plugins.intellij.caos.psi.types.CaosScriptExpressionType
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptCommandCallStub
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptCommandStub
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptCommandTokenStub
import com.openc2e.plugins.intellij.caos.stubs.api.CaosScriptExpressionStub
import com.openc2e.plugins.intellij.caos.stubs.types.CaosScriptStubTypes

class CaosScriptCommandStubImpl(
        parent:StubElement<*>,
        override val commandTokens:List<String>
) : StubBase<CaosScriptCommandImpl>(parent, CaosScriptStubTypes.COMMAND), CaosScriptCommandStub {
    override val command:String by lazy {
        commandTokens.joinToString(" ")
    }
}


class CaosScriptCommandCallStubImpl(
        parent:StubElement<*>,
        override val commandTokens:List<String>,
        override val numParameters: Int
) : StubBase<CaosScriptCommandCallImpl>(parent, CaosScriptStubTypes.COMMAND_CALL), CaosScriptCommandCallStub {
    override val command:String by lazy {
        commandTokens.joinToString(" ")
    }
}

class CaosScriptCommandTokenStubImpl(
        parent:StubElement<*>,
        override val text:String,
        override val index: Int
) : StubBase<CaosScriptCommandTokenImpl>(parent, CaosScriptStubTypes.COMMAND_TOKEN), CaosScriptCommandTokenStub


class CaosScriptExpressionStubImpl(
        parent:StubElement<*>,
        override val type: CaosScriptExpressionType,
        override val text: String
): StubBase<CaosScriptExpressionImpl>(parent, CaosScriptStubTypes.EXPRESSION), CaosScriptExpressionStub
