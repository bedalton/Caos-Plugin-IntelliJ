package com.badahori.creatures.plugins.intellij.agenteering.caos.indices

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

object VariantKeyDescriptor : KeyDescriptor<CaosVariant> {

    const val VERSION = 2

    override fun getHashCode(value: CaosVariant): Int {
        val fudgedValue = if (value == CaosVariant.DS)
            CaosVariant.C3
        else
            value
        return fudgedValue.code.hashCode()
    }

    override fun isEqual(val1: CaosVariant, val2: CaosVariant): Boolean {
        return val1 == val2 || (val1.isC3DS && val2.isC3DS)
    }

    @Throws(IOException::class)
    override fun save(storage: DataOutput, value: CaosVariant) {
        IOUtil.writeUTF(storage, value.code)
    }

    @Throws(IOException::class)
    override fun read(storage: DataInput): CaosVariant {
        return CaosVariant.fromVal(IOUtil.readUTF(storage))
    }


}