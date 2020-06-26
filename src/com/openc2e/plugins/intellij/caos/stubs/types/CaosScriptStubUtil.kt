package com.openc2e.plugins.intellij.caos.stubs.types

import com.intellij.openapi.util.TextRange
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.openc2e.plugins.intellij.caos.deducer.*
import com.openc2e.plugins.intellij.caos.deducer.CaosNumber.CaosFloatNumber
import com.openc2e.plugins.intellij.caos.deducer.CaosNumber.CaosIntNumber
import com.openc2e.plugins.intellij.caos.deducer.CaosVar.*
import com.openc2e.plugins.intellij.caos.deducer.CaosVar.CaosLiteral.*
import com.openc2e.plugins.intellij.caos.deducer.CaosVar.CaosNamedGameVar.EameVar
import com.openc2e.plugins.intellij.caos.deducer.CaosVar.CaosNamedGameVar.GameVar
import com.openc2e.plugins.intellij.caos.deducer.CaosVar.CaosNumberedVar.*
import com.openc2e.plugins.intellij.caos.psi.api.CaosExpressionValueType
import com.openc2e.plugins.intellij.caos.utils.nullIfEmpty
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
private const val TOKEN = 12
private const val RANGE = 13
private const val C1_STRING = 14
private const val MAME = 15
private const val GAME = 16
private const val EAME = 17
private const val NAME = 18
private const val NULL = 19
private const val NONE = 20
private const val PICT_DIMENSIONS = 21;



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
        BYTE_STRING -> CaosByteString(readNameString() ?: "", readList { readInt() }.filterNotNull())
        INT -> CaosInt(readLong())
        FLOAT -> CaosFloat(readFloat())
        ANIMATION_STRING -> CaosAnimationString(value = readNameAsString() ?: "", animation = readAnimation())
        TOKEN -> CaosToken(value = readNameString() ?: "XXXX")
        RANGE -> readCaosRange()
        C1_STRING -> CaosC1String(readNameAsString() ?: "")
        EAME -> EameVar(readNameAsString()?:"???")
        GAME -> GameVar(readNameAsString()?:"???")
        MAME -> CaosNamedGameVar.MameVar(readNameAsString()?:"???")
        NAME -> CaosNamedGameVar.NameVar(readNameAsString()?:"???")
        NULL -> CaosVarNull
        NONE -> CaosVarNone
        PICT_DIMENSIONS -> CaosVar.CaosLiteral.CaosPictDimension(readInt(), readInt())
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

private const val UNDEFINED = -1

internal fun StubInputStream.readCaosNumber() : CaosNumber {
    return when (readInt()) {
        INT -> CaosIntNumber(readInt())
        FLOAT -> CaosFloatNumber(readFloat())
        else -> CaosNumber.Undefined
    }
}


internal fun StubOutputStream.writeCaosNumber(number:CaosNumber) {
    when (number) {
        is CaosIntNumber -> {
            writeInt(INT)
            writeInt(number.value)
        }
        is CaosFloatNumber -> {
            writeInt(FLOAT)
            writeFloat(number.value)
        }
        else -> writeInt(UNDEFINED)
    }
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
        is CaosC1String -> {
            writeInt(C1_STRING)
            writeName(caosVar.value)
        }
        is CaosByteString -> {
            writeInt(BYTE_STRING)
            writeName(caosVar.text)
            writeList(caosVar.value) { writeInt(it) }
        }
        is CaosInt -> {
            writeInt(INT)
            writeLong(caosVar.value)
        }
        is CaosFloat -> {
            writeInt(FLOAT)
            writeFloat(caosVar.value)
        }
        is CaosAnimationString -> {
            writeInt(ANIMATION_STRING)
            writeName(caosVar.value)
            writeAnimation(caosVar.animation)

        }
        is CaosToken -> {
            writeInt(TOKEN)
            writeName(caosVar.value)
        }
        is CaosIntRange -> writeCaosRange(caosVar)
        is EameVar -> {
            writeInt(EAME)
            writeName(caosVar.name)
        }
        is GameVar -> {
            writeInt(GAME)
            writeName(caosVar.name)
        }
        is CaosNamedGameVar.MameVar -> {
            writeInt(MAME)
            writeName(caosVar.name)
        }
        is CaosNamedGameVar.NameVar -> {
            writeInt(NAME)
            writeName(caosVar.name)
        }
        is CaosVarNull -> writeInt(NULL)
        is CaosVarNone -> writeInt(NONE)
        is CaosVar.CaosLiteral.CaosPictDimension -> {
            writeInt(PICT_DIMENSIONS)
            writeInt(caosVar.width)
            writeInt(caosVar.height)
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
    val command =readNameAsString() ?: UNDEF
    val returnType = readNameAsString()?.nullIfEmpty()?.let { CaosExpressionValueType.fromSimpleName(it)}
    return CaosCommandCall(command, returnType)
}

private fun StubOutputStream.writeCaosCommandVar(call: CaosCommandCall) {
    writeInt(COMMAND_CALL)
    writeName(call.text)
    writeName(call.returnType?.simpleName ?: "")
}

internal inline fun <T> StubOutputStream.writeList(list:List<T>, writer:StubOutputStream.(T)->Unit) {
    writeInt(list.size)
    for(item in list) {
        writer(item)
    }
}

internal inline fun <T:Any> StubInputStream.readList(reader:StubInputStream.()->T) : List<T> {
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

internal fun StubOutputStream.writeAnimation(animation:CaosAnimation?) {
    writeBoolean(animation != null)
    if (animation == null)
        return
    writeBoolean(animation !is CaosAnimation.ErrorAnimation)
    if (animation is CaosAnimation.ErrorAnimation) {
        writeName(animation.errorMessage)
        return
    }
    animation as CaosAnimation.Animation
    writeList(animation.poseList) {
        writeInt(it)
    }
    writeBoolean(animation.repeats)
    writeInt(animation.repeatsFrom ?: -1)
}

internal fun StubInputStream.readAnimation() : CaosAnimation? {
    if (!readBoolean())
        return null
    if (readBoolean()) {
        val poseList = readList {
            readInt()
        }.filterNotNull()
        val repeats = readBoolean()
        val repeatsFrom = readInt()
        return if (repeatsFrom < 0)
            CaosAnimation.Animation(poseList, repeats, null)
        else
            CaosAnimation.Animation(poseList, repeats, repeatsFrom)
    }
    return CaosAnimation.ErrorAnimation(readNameAsString() ?: "UNDEFINED ERROR")
}

private fun StubOutputStream.writeCaosRange(range:CaosIntRange) {
    writeInt(RANGE)
    writeBoolean(range.min != null)
    range.min?.let {
        writeInt(it)
    }
    writeBoolean(range.max != null)
    range.max?.let {
        writeInt(it)
    }
}

private fun StubInputStream.readCaosRange() : CaosIntRange {
    val min = if (readBoolean()) readInt() else null
    val max = if (readBoolean()) readInt() else null
    return CaosIntRange(min, max)
}