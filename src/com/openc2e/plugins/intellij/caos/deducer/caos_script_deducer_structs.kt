package com.openc2e.plugins.intellij.caos.deducer

import com.intellij.openapi.util.TextRange


data class CaosScope(val range:TextRange, val blockType:CaosScriptBlockType, val enclosingScope:List<CaosScope>) {
    val startOffset:Int get() = range.startOffset
    val endOffset:Int get() = range.endOffset
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
    SCRP("scrp"),
    INST("inst"),
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
    SUBV(6);

    companion object {
        fun fromValue(value:Int) : CaosOp = values().first { it.value == value }
    }
}

sealed class CaosVar(open val text:String) {
    data class ConstVal(val name:String) : CaosVar(name)
    data class NamedVar(val name:String, val assumedValue: CaosVar? = null) : CaosVar(name)
    sealed class CaosNumberedVar(override val text:String, open val number:Int, open val c1:Boolean) : CaosVar(text) {
        data class CaosVaXXVar(override val text:String, override val number:Int, override val c1:Boolean) : CaosNumberedVar(text, number, c1)
        data class CaosOvXXVar(override val text:String, override val number:Int, override val c1:Boolean) : CaosNumberedVar(text, number, c1)
        data class CaosMvXXVar(override val text:String, override val number:Int) : CaosNumberedVar(text, number, false)
    }
    data class CaosCommandCall(override val text:String) : CaosVar(text)
    object CaosLiteralVal: CaosVar("")
    sealed class CaosLiteral(text:String) : CaosVar(text) {
        data class CaosString(val value:String) : CaosLiteral(value)
        data class CaosByteString(val value:String) : CaosLiteral(value)
        data class CaosInt(val value:Int) : CaosLiteral("$value")
        data class CaosFloat(val value:Float) : CaosLiteral("$value")
        data class CaosAnimationString(val value:String, val repeats:Boolean = false) : CaosLiteral(value)
        data class CaosToken(val value:String) : CaosLiteral(value)
    }
}