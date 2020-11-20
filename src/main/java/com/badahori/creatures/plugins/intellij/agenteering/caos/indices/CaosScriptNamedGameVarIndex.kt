package com.badahori.creatures.plugins.intellij.agenteering.caos.indices

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosScriptNamedGameVarType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptNamedGameVar
import com.intellij.openapi.project.Project
import com.intellij.psi.stubs.StubIndexKey

class CaosScriptNamedGameVarIndex : CaosStringIndexBase<CaosScriptNamedGameVar>(CaosScriptNamedGameVar::class.java) {
    override fun getKey(): StubIndexKey<String, CaosScriptNamedGameVar> = KEY

    override fun getVersion(): Int = super.getVersion() + VERSION

    operator fun get(type:CaosScriptNamedGameVarType, key:String, project:Project) : List<CaosScriptNamedGameVar> {
        val indexKey = getKey(type, key)
        return get(indexKey, project)
    }

    companion object {
        val KEY: StubIndexKey<String, CaosScriptNamedGameVar> = IndexKeyUtil.create(CaosScriptNamedGameVarIndex::class.java)
        const val VERSION = 0
        val instance = CaosScriptNamedGameVarIndex()

        fun getKey(type:CaosScriptNamedGameVarType, key:String) : String {
            return type.token + ":" + key
        }
    }
}

/*
private object NamedGameVarKeySerializer : KeyDescriptor<NamedGameVarKey> {
    override fun getHashCode(value: NamedGameVarKey?): Int {
        return value.hashCode()
    }

    override fun isEqual(val1: NamedGameVarKey, val2: NamedGameVarKey): Boolean {
        return val1.type == val2.type
                && val1.key == val2.key
    }

    override fun save(out: DataOutput, value: NamedGameVarKey) {
        out.writeInt(value.type.value)
        out.writeUTF(value.key)
    }

    override fun read(input: DataInput): NamedGameVarKey {
        val type = CaosScriptNamedGameVarType.fromValue(input.readInt())
        val key = input.readUTF()
        return NamedGameVarKey(type, key)
    }
}

data class NamedGameVarKey(val type:CaosScriptNamedGameVarType, val key:String)*/