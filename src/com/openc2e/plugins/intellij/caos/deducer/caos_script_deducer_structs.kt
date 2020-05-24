package com.openc2e.plugins.intellij.caos.deducer

import com.intellij.openapi.util.TextRange
import com.openc2e.plugins.intellij.caos.lang.CaosScriptFile
import com.openc2e.plugins.intellij.caos.psi.api.CaosExpressionValueType
import com.openc2e.plugins.intellij.caos.psi.util.CaosScriptNamedGameVarType


data class CaosScope(val range:TextRange, val blockType:CaosScriptBlockType, val enclosingScope:List<CaosScope>) {
    val startOffset:Int get() = range.startOffset
    val endOffset:Int get() = range.endOffset
}

fun rootScope(file:CaosScriptFile) : CaosScope {
    return CaosScope(file.textRange, CaosScriptBlockType.MACRO, emptyList())
}

data class CaosBlockCondition(
    val value1:CaosVar,
    val value2:CaosVar,
    val eqOp:CaosScriptEqualityOp
)

sealed class CaosLoopCondition {
    object Ever:CaosLoopCondition()
    data class Untl(val condition: CaosBlockCondition):CaosLoopCondition()
}

enum class CaosScriptEqualityOp (val expr:List<String>) {
    EQUAL(listOf("eq", "=")),
    NOT_EQUAL(listOf("ne", "<>")),
    LESS_THAN(listOf("lt", "<")),
    LESS_THAN_EQUAL(listOf("le", "<=")),
    GREATER_THAN(listOf("gt", ">")),
    GREATER_THAN_EQUAL(listOf("ge", ">=")),
    BITWISE_AND(listOf("bt")),
    BITWISE_NAND(listOf("bf"))
}

enum class CaosScriptBlockType(val value:String) {
    UNDEF("UNDEF"),
    SCRP("scrp"),
    MACRO("inst"),
    DOIF("doif"),
    ELIF("elif"),
    ELSE("else"),
    SUBR("subr"),
    ENUM("enum"),
    LOOP("loop"),
    REPS("reps"),
    ESCN("escn");

    companion object {
        fun fromValue(value:String) : CaosScriptBlockType {
            return values().first { it.value == value}
        }
    }
}

enum class CaosOp(val value:Int) {
    SETV(0),
    ORRV(1),
    ANDV(2),
    DIVV(3),
    MULV(4),
    ADDV(5),
    SUBV(6),
    NEGV(7),
    MODV(8),
    UNDEF(-1),
    SETS(9);

    companion object {
        fun fromValue(value:Int) : CaosOp = values().first { it.value == value }
    }
}

interface CaosScriptIsVariableVar

sealed class CaosVar(open val text:String, val simpleType: CaosExpressionValueType) {
    data class ConstVal(val name:String) : CaosVar(name, CaosExpressionValueType.DECIMAL)
    data class NamedVar(val name:String, val assumedValue: CaosVar? = null) : CaosVar(name, CaosExpressionValueType.VARIABLE), CaosScriptIsVariableVar
    sealed class CaosNumberedVar(override val text:String, open val number:Int, open val isC1Var:Boolean) : CaosVar(text, CaosExpressionValueType.VARIABLE),CaosScriptIsVariableVar {
        data class CaosVaXXVar(override val text:String, override val number:Int, override val isC1Var:Boolean) : CaosNumberedVar(text, number, isC1Var)
        data class CaosOvXXVar(override val text:String, override val number:Int, override val isC1Var:Boolean) : CaosNumberedVar(text, number, isC1Var)
        data class CaosMvXXVar(override val text:String, override val number:Int) : CaosNumberedVar(text, number, false)
    }
    sealed class CaosNamedGameVar(val name:String, val type:CaosScriptNamedGameVarType) : CaosVar("${type.token} \"$name\"", CaosExpressionValueType.VARIABLE), CaosScriptIsVariableVar {
        class MameVar(name:String) : CaosNamedGameVar(name, CaosScriptNamedGameVarType.MAME)
        class GameVar(name:String) : CaosNamedGameVar(name, CaosScriptNamedGameVarType.GAME)
        class EameVar(name:String) : CaosNamedGameVar(name, CaosScriptNamedGameVarType.EAME)
        class NameVar(name:String) : CaosNamedGameVar(name, CaosScriptNamedGameVarType.NAME)
    }
    data class CaosCommandCall(override val text:String) : CaosVar(text, CaosExpressionValueType.ANY)
    object CaosLiteralVal: CaosVar("", CaosExpressionValueType.ANY)
    sealed class CaosLiteral(text:String, simpleType: CaosExpressionValueType) : CaosVar(text, simpleType) {
        data class CaosString(val value:String) : CaosLiteral(value, CaosExpressionValueType.STRING)
        data class CaosC1String(val value:String) : CaosLiteral(value, CaosExpressionValueType.C1_STRING)
        data class CaosByteString(override val text:String, val value:List<Int>) : CaosLiteral(text, CaosExpressionValueType.BYTE_STRING)
        data class CaosInt(val value:Int) : CaosLiteral("$value", CaosExpressionValueType.INT)
        data class CaosFloat(val value:Float) : CaosLiteral("$value", CaosExpressionValueType.FLOAT)
        data class CaosAnimationString(val value:String, val animation:CaosAnimation?) : CaosLiteral(value, CaosExpressionValueType.ANIMATION)
        data class CaosToken(val value:String) : CaosLiteral(value, CaosExpressionValueType.TOKEN)
    }
    object CaosVarNull : CaosVar("[NULL]", CaosExpressionValueType.NULL)
    object CaosVarNone : CaosVar("{NONE}", CaosExpressionValueType.NULL)
}

sealed class CaosNumber {
    data class CaosIntNumber(val value:Int) : CaosNumber()
    data class CaosFloatNumber(val value:Float) : CaosNumber()
    object Undefined: CaosNumber()
}

sealed class CaosAnimation {
    data class Animation(val poseList:List<Int>, val repeats:Boolean, val repeatsFrom:Int? = null) : CaosAnimation()
    data class ErrorAnimation(val errorMessage:String) : CaosAnimation()
}