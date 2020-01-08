package com.openc2e.plugins.intellij.caos.utils

import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.caos.def.psi.util.CaosDefPsiImplUtil
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefParameterStruct
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefReturnTypeStruct
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefTypeDefValueStruct
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefVariableTypeStruct

fun StubInputStream.readNameAsString() : String? {
    return readName()?.string
}

fun StubInputStream.readParameter() : CaosDefParameterStruct {
    val name = readNameAsString().nullIfEmpty() ?: "???"
    val type = readVariableType() ?: CaosDefPsiImplUtil.AnyTypeType
    val comment = readUTFFast().nullIfEmpty()
    return CaosDefParameterStruct(
            name = name,
            type = type,
            comment = comment
    )
}

fun StubOutputStream.writeParameter(parameter:CaosDefParameterStruct) {
    writeName(parameter.name)
    writeVariableType(parameter.type)
    writeUTFFast(parameter.comment ?: "")

}

fun StubInputStream.readReturnType() : CaosDefReturnTypeStruct? {
    val returnType = readVariableType()
    val comment = readUTFFast().nullIfEmpty()
    if (returnType == null)
        return null
    return CaosDefReturnTypeStruct(
            type = returnType,
            comment = comment
    )
}

fun StubOutputStream.writeReturnType(returnType:CaosDefReturnTypeStruct) {
    writeVariableType(returnType.type)
    writeUTFFast(returnType.comment ?: "")
}

fun StubInputStream.readVariableType() : CaosDefVariableTypeStruct? {
    val type = readNameAsString()
    val typeDef = readNameString()
    val typeNote = readNameAsString()
    val min = readInt()
    val max = readInt()
    val intRange = if (min >=0 && max >= 0)
        Pair(min, max)
    else
        null
    val length = readInt()
    if (type == null)
        return null
    return CaosDefVariableTypeStruct(
            type = type,
            typedef = typeDef,
            noteText = typeNote,
            intRange = intRange,
            length = if (length >= 0) length else null
    )
}

fun StubOutputStream.writeVariableType(struct:CaosDefVariableTypeStruct) {
    writeName(struct.type)
    writeName(struct.typedef)
    writeName(struct.noteText)
    writeInt(struct.intRange?.first ?: -1)
    writeInt(struct.intRange?.second ?: -1)
    writeInt(struct.length ?: -1)
}

fun <T> StubOutputStream.writeList(list: List<T>, write:StubOutputStream.(T)->Unit) {
    writeInt(list.size)
    list.forEach {
        write(it)
    }
}

fun <T> StubInputStream.readList(readItem:StubInputStream.()->T?) : List<T?> {
    val size = readInt()
    return (0 until size).map {
        readItem()
    }
}

fun StubOutputStream.writeTypeDefValue(value:CaosDefTypeDefValueStruct) {
    writeName(value.key)
    writeName(value.value)
    writeUTFFast(value.description ?: "")
}

fun StubInputStream.readTypeDefValue() : CaosDefTypeDefValueStruct? {
    val key = readNameAsString().nullIfEmpty()
    val value = readNameString() ?: CaosDefPsiImplUtil.UnknownReturn
    val comment = readUTFFast().nullIfEmpty()
    if (key == null)
        return null
    return CaosDefTypeDefValueStruct(key = key, value = value, description = comment)
}