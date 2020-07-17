package com.badahori.creatures.plugins.intellij.agenteering.caos.utils

import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.util.CaosDefPsiImplUtil
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.TypeDefEq
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefParameterStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefReturnTypeStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefTypeDefValueStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.impl.CaosDefVariableTypeStruct
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.readStringList
import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types.writeStringList

fun StubInputStream.readNameAsString() : String? {
    return readName()?.string
}

internal fun StubInputStream.readParameter() : CaosDefParameterStruct {
    val name = readNameAsString().nullIfEmpty() ?: "???"
    val type = readVariableType() ?: CaosDefPsiImplUtil.AnyTypeType
    val comment = readUTFFast().nullIfEmpty()
    val parameterNumber = readInt()
    return CaosDefParameterStruct(
            name = name,
            type = type,
            comment = comment,
            parameterNumber = parameterNumber
    )
}

internal fun StubOutputStream.writeParameter(parameter:CaosDefParameterStruct) {
    writeName(parameter.name)
    writeVariableType(parameter.type)
    writeUTFFast(parameter.comment ?: "")
    writeInt(parameter.parameterNumber)
}

internal fun StubInputStream.readReturnType() : CaosDefReturnTypeStruct? {
    val returnType = readVariableType()
    val comment = readUTFFast().nullIfEmpty()
    if (returnType == null)
        return null
    return CaosDefReturnTypeStruct(
            type = returnType,
            comment = comment
    )
}

internal fun StubOutputStream.writeReturnType(returnType:CaosDefReturnTypeStruct) {
    writeVariableType(returnType.type)
    writeUTFFast(returnType.comment ?: "")
}

internal fun StubInputStream.readVariableType() : CaosDefVariableTypeStruct? {
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
    val fileTypes = readStringList().ifEmpty { null }
    return CaosDefVariableTypeStruct(
            type = type,
            typedef = typeDef,
            noteText = typeNote,
            intRange = intRange,
            length = if (length >= 0) length else null,
            fileTypes = fileTypes
    )
}

internal fun StubOutputStream.writeVariableType(struct:CaosDefVariableTypeStruct) {
    writeName(struct.type)
    writeName(struct.typedef)
    writeName(struct.noteText)
    writeInt(struct.intRange?.first ?: -1)
    writeInt(struct.intRange?.second ?: -1)
    writeInt(struct.length ?: -1)
    writeStringList(struct.fileTypes.orEmpty())
}

internal fun <T> StubOutputStream.writeList(list: List<T>, write:StubOutputStream.(T)->Unit) {
    writeInt(list.size)
    list.forEach {
        write(it)
    }
}

internal fun <T> StubInputStream.readList(readItem:StubInputStream.()->T?) : List<T?> {
    val size = readInt()
    return (0 until size).map {
        readItem()
    }
}

internal fun StubOutputStream.writeTypeDefValue(value:CaosDefTypeDefValueStruct) {
    writeName(value.key)
    writeName(value.value)
    writeUTFFast(value.description ?: "")
    val equality = when (value.equality) {
        TypeDefEq.EQUAL -> 0
        TypeDefEq.GREATER_THAN -> 1
        TypeDefEq.NOT_EQUAL -> 2
    }
    writeInt(equality)
}

internal fun StubInputStream.readTypeDefValue() : CaosDefTypeDefValueStruct? {
    val key = readNameAsString().nullIfEmpty()
    val value = readNameString() ?: CaosDefPsiImplUtil.UnknownReturn
    val comment = readUTFFast().nullIfEmpty()
    val equality = when (val eq= readInt()) {
        0 -> TypeDefEq.EQUAL
        1 -> TypeDefEq.GREATER_THAN
        2 -> TypeDefEq.NOT_EQUAL
        else -> throw Exception("Invalid value passed to type def equality. Error Value: '$eq'")
    }
    if (key == null)
        return null
    return CaosDefTypeDefValueStruct(key = key, value = value, description = comment, equality = equality)
}