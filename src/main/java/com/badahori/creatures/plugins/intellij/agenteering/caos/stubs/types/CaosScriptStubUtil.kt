package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.types

import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosScope
import com.badahori.creatures.plugins.intellij.agenteering.caos.deducer.CaosScriptBlockType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readList
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.readNameAsString
import com.intellij.openapi.util.TextRange
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream


internal fun StubInputStream.readExpressionValueType() : CaosExpressionValueType {
    return CaosExpressionValueType.fromIntValue(readInt())
}


internal fun StubInputStream.readCaosVarSafe() : CaosExpressionValueType? {
    if (!readBoolean())
        return null
    return readExpressionValueType()
}

internal fun StubOutputStream.writeCaosVarSafe(caosVar:CaosExpressionValueType?) {
    writeBoolean(caosVar != null)
    if (caosVar != null)
        writeExpressionValueType(caosVar)
}

internal fun StubOutputStream.writeExpressionValueType(caosVar:CaosExpressionValueType) {
        writeInt(caosVar.value)
}


internal inline fun <T> StubOutputStream.writeList(list:List<T>, writer:StubOutputStream.(T)->Unit) {
    writeInt(list.size)
    for(item in list) {
        writer(item)
    }
}
internal inline fun <T> StubOutputStream.writeList(list:Set<T>, writer:StubOutputStream.(T)->Unit) {
    writeInt(list.size)
    for(item in list) {
        writer(item)
    }
}

internal inline fun <T:Any> StubInputStream.readList(reader:StubInputStream.()->T?) : List<T> {
    val listSize = readInt()
    return (0 until listSize).mapNotNull {
        reader()
    }
}

internal fun StubOutputStream.writeStringList(list:List<String>) {
    writeInt(list.size)
    for(item in list) {
        writeName(item)
    }
}

internal fun StubInputStream.readStringList() : List<String> {
    val listSize = readInt()
    return (0 until listSize).mapNotNull {
        readNameAsString()
    }
}

internal fun StubInputStream.readScope() : CaosScope {
    val startOffset = readInt()
    val endOffset = readInt()
    val blockType = readNameAsString()?.let { CaosScriptBlockType.fromValue(it) } ?: CaosScriptBlockType.UNDEF
    val enclosingScope = readList { readScope() }.filterNotNull()
    return CaosScope(range = TextRange.create(startOffset, endOffset), blockType = blockType, enclosingScope = enclosingScope)
}

internal fun StubOutputStream.writeScope(scope: CaosScope) {
    writeInt(scope.startOffset)
    writeInt(scope.endOffset)
    writeName(scope.blockType.value)
    writeList(scope.enclosingScope) { writeScope(it) }
}

fun StubInputStream.readSimpleType() : CaosExpressionValueType {
    return CaosExpressionValueType.fromIntValue(readInt())
}