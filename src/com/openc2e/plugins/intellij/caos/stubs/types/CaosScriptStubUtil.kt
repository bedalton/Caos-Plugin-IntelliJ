package com.openc2e.plugins.intellij.caos.stubs.types

import com.intellij.openapi.util.TextRange
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.caos.deducer.CaosScope
import com.openc2e.plugins.intellij.caos.deducer.CaosScriptBlockType
import com.openc2e.plugins.intellij.caos.deducer.CaosVar
import com.openc2e.plugins.intellij.caos.deducer.CaosVar.*
import com.openc2e.plugins.intellij.caos.deducer.CaosVar.CaosLiteral.*
import com.openc2e.plugins.intellij.caos.deducer.CaosVar.CaosNumberedVar.*
import com.openc2e.plugins.intellij.caos.utils.readList
import com.openc2e.plugins.intellij.caos.utils.readNameAsString

private const val UNDEF = "{{UNDEF}}"
private const val CONST = 0
private const val NAMED = 1
private const val VAxx = 2
private const val OVxx = 3
private const val MVxx = 4
private const val COMMAND_CALL = 5
private const val LITERAL_VAL = 6
private const val STRING = 7
private const val BYTE_STRING = 8
private const val INT = 9
private const val FLOAT = 10
private const val ANIMATION_STRING = 11
private const val TOKEN = 12;


internal fun StubInputStream.readCaosVar() : CaosVar{
    return when (val value = readInt()) {
        CONST -> readConst()
        NAMED -> readNamedVar()
        VAxx -> readVAxx()
        OVxx -> readOVxx()
        MVxx -> readMVxx()
        COMMAND_CALL -> readCaosCommandAsCaosVar()
        LITERAL_VAL -> CaosLiteralVal
        STRING -> CaosString(readNameAsString() ?: "")
        BYTE_STRING -> CaosByteString(readNameString() ?: "")
        INT -> CaosInt(readInt())
        FLOAT -> CaosFloat(readFloat())
        ANIMATION_STRING -> CaosAnimationString(value = readNameAsString() ?: "", repeats = readBoolean())
        TOKEN -> CaosToken(value = readNameString() ?: "XXXX")
        else -> throw Exception("Unexpected caos var type '$value' encountered")
    }
}


internal fun StubInputStream.readCaosVarSafe() : CaosVar? {
    if (!readBoolean())
        return null
    return readCaosVar()
}

internal fun StubOutputStream.writeCaosVarSafe(caosVar:CaosVar?) {
    writeBoolean(caosVar != null)
    if (caosVar != null)
        writeCaosVar(caosVar)
}


internal fun StubOutputStream.writeCaosVar(caosVar:CaosVar) {
    when (caosVar) {
        is ConstVal -> writeConst(caosVar)
        is NamedVar -> writeNamedVar(caosVar)
        is CaosVaXXVar -> writeVAxx(caosVar)
        is CaosOvXXVar -> writeOvxx(caosVar)
        is CaosMvXXVar -> writeMVxx(caosVar)
        is CaosCommandCall -> writeCaosCommandVar(caosVar)
        is CaosLiteralVal -> writeInt(LITERAL_VAL)
        is CaosString -> {
            writeInt(STRING)
            writeName(caosVar.value)
        }
        is CaosByteString -> {
            writeInt(BYTE_STRING)
            writeName(caosVar.value)
        }
        is CaosInt -> {
            writeInt(INT)
            writeInt(caosVar.value)
        }
        is CaosFloat -> {
            writeInt(FLOAT)
            writeFloat(caosVar.value)
        }
        is CaosAnimationString -> {
            writeInt(ANIMATION_STRING)
            writeName(caosVar.value)
            writeBoolean(caosVar.repeats)
        }
        is CaosToken -> {
            writeInt(TOKEN)
            writeName(caosVar.value)
        }
    }
}

private fun StubInputStream.readConst() : ConstVal {
    return ConstVal(readNameAsString() ?: UNDEF)
}

private fun StubOutputStream.writeConst(value:ConstVal) {
    writeInt(CONST)
    writeName(value.name)
}

private fun StubInputStream.readNamedVar() : NamedVar {
    val name = readNameAsString() ?: UNDEF
    val assumedValue = if (readBoolean())
        readCaosVar()
    else
        null
    return NamedVar(name, assumedValue = assumedValue)
}

private fun StubOutputStream.writeNamedVar(value:NamedVar) {
    writeInt(NAMED)
    writeName(value.name)
    writeBoolean(value.assumedValue != null)
    if (value.assumedValue != null)
        writeCaosVar(value.assumedValue)
}

private fun StubInputStream.readVAxx() : CaosVaXXVar {
    val name = readNameAsString() ?: UNDEF
    val number = readInt()
    val c1Var = readBoolean()
    return CaosVaXXVar(text = name, number = number, isC1Var = c1Var)
}
private fun StubOutputStream.writeVAxx(caosVar:CaosVaXXVar) {
    writeInt(VAxx)
    writeName(caosVar.text)
    writeInt(caosVar.number)
    writeBoolean(caosVar.isC1Var)
}

private fun StubInputStream.readOVxx() : CaosOvXXVar {
    val name = readNameAsString() ?: UNDEF
    val number = readInt()
    val c1Var = readBoolean()
    return CaosOvXXVar(text = name, number = number, isC1Var = c1Var)
}

private fun StubOutputStream.writeOvxx(caosVar:CaosOvXXVar) {
    writeInt(OVxx)
    writeName(caosVar.text)
    writeInt(caosVar.number)
    writeBoolean(caosVar.isC1Var)
}

private fun StubInputStream.readMVxx() : CaosMvXXVar {
    val name = readNameAsString() ?: UNDEF
    val number = readInt()
    return CaosMvXXVar(text = name, number = number)
}
private fun StubOutputStream.writeMVxx(caosVar:CaosMvXXVar) {
    writeInt(MVxx)
    writeName(caosVar.text)
    writeInt(caosVar.number)
}

private fun StubInputStream.readCaosCommandAsCaosVar() : CaosCommandCall {
    return CaosCommandCall(readNameAsString() ?: UNDEF)
}

private fun StubOutputStream.writeCaosCommandVar(call: CaosCommandCall) {
    writeInt(COMMAND_CALL)
    writeName(call.text)
}

internal inline fun <T> StubOutputStream.writeList(list:List<T>, writer:StubOutputStream.(T)->Unit) {
    writeInt(list.size)
    for(item in list) {
        writer(item)
    }
}

internal inline fun <T:Any> StubInputStream.readList(reader:StubInputStream.()->T) : List<T> {
    val listSize = readInt()
    val out:List<T> = (0 until listSize).mapNotNull {
        reader()
    }
    return out
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