package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.stubs

import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayLanguage
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.impl.PrayPrayTagImpl
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.CaosScriptStubElementType
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.readNullable
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.writeNullable
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString
import com.intellij.lang.Language
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream


object PrayTagStubType
    : CaosScriptStubElementType<PrayTagStub, PrayPrayTagImpl>("Pray_PRAY_TAG") {

    override fun getLanguage(): Language {
        return PrayLanguage
    }
    
    override fun getExternalId(): String {
        return "pray." + super.toString()
    }


    override fun serialize(stub: PrayTagStub, stream: StubOutputStream) {
        stream.writeName(stub.tagName)
        stream.writeName(stub.tagValueRaw)
        stream.writeNullable(stub.stringValue) {
            writeName(it)
        }
        stream.writeNullable(stub.intValue) {
            writeInt(it)
        }
    }

    override fun deserialize(stream: StubInputStream, parentStub: StubElement<*>?): PrayTagStub {
        val tagName = stream.readNameAsString()!!
        val tagValueRaw = stream.readNameAsString()!!
        val stringValue = stream.readNullable { readNameAsString() }
        val intValue = stream.readNullable { readInt() }
        return PrayTagStubImpl(
            parentStub,
            tagName = tagName,
            tagValueRaw = tagValueRaw,
            stringValue = stringValue,
            intValue = intValue
        )
    }

    override fun createPsi(stub: PrayTagStub): PrayPrayTagImpl {
        return PrayPrayTagImpl(stub, this)
    }

    override fun createStub(psi: PrayPrayTagImpl, parentStub: StubElement<*>?): PrayTagStub {
        return PrayTagStubImpl(
            parentStub,
            tagName = psi.tagName,
            tagValueRaw = psi.tagTagValue.text,
            stringValue = psi.valueAsString,
            intValue = psi.valueAsInt
        )
    }

}