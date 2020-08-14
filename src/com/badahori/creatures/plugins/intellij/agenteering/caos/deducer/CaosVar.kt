package com.badahori.creatures.plugins.intellij.agenteering.caos.deducer

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosExpressionValueType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosScriptNamedGameVarType
import kotlin.math.max
import kotlin.math.min


interface CaosScriptIsVariableVar

sealed class CaosVar(open val text:String, val simpleType: CaosExpressionValueType) {
    data class ConstVal(val name:String) : CaosVar(name, CaosExpressionValueType.DECIMAL)
    data class NamedVar(val name:String, val assumedValue: CaosVar? = null) : CaosVar(name, CaosExpressionValueType.VARIABLE), CaosScriptIsVariableVar
    sealed class CaosNumberedVar(override val text:String, open val number:Int, open val isC1Var:Boolean) : CaosVar(text, CaosExpressionValueType.VARIABLE),CaosScriptIsVariableVar {
        data class CaosVaXXVar(override val text:String, override val number:Int, override val isC1Var:Boolean) : CaosNumberedVar(text, number, isC1Var)
        data class CaosOvXXVar(override val text:String, override val number:Int, override val isC1Var:Boolean) : CaosNumberedVar(text, number, isC1Var)
        data class CaosMvXXVar(override val text:String, override val number:Int) : CaosNumberedVar(text, number, false)
    }
    sealed class CaosNamedGameVar(val name:String, val type: CaosScriptNamedGameVarType) : CaosVar("${type.token} \"$name\"", CaosExpressionValueType.VARIABLE), CaosScriptIsVariableVar {
        class MameVar(name:String) : CaosNamedGameVar(name, CaosScriptNamedGameVarType.MAME)
        class GameVar(name:String) : CaosNamedGameVar(name, CaosScriptNamedGameVarType.GAME)
        class C2GameVar(val first:Int, val second:Int) : CaosNamedGameVar("$first $second", CaosScriptNamedGameVarType.GAME)
        class EameVar(name:String) : CaosNamedGameVar(name, CaosScriptNamedGameVarType.EAME)
        class NameVar(name:String) : CaosNamedGameVar(name, CaosScriptNamedGameVarType.NAME)
    }
    data class CaosCommandCall(override val text:String, val returnType: CaosExpressionValueType? = null) : CaosVar(text, CaosExpressionValueType.ANY)
    object CaosLiteralVal: CaosVar("", CaosExpressionValueType.ANY)
    data class CaosInferredVariableType(val varName:String, val value:CaosExpressionValueType) : CaosVar("$varName", value)
    sealed class CaosLiteral(text:String, simpleType: CaosExpressionValueType) : CaosVar(text, simpleType) {
        data class CaosString(val value:String) : CaosLiteral(value, CaosExpressionValueType.STRING)
        data class CaosC1String(val value:String) : CaosLiteral(value, CaosExpressionValueType.C1_STRING)
        data class CaosByteString(override val text:String, val value:List<Int>) : CaosLiteral(text, CaosExpressionValueType.BYTE_STRING)
        data class CaosInt(val value:Long) : CaosLiteral("$value", CaosExpressionValueType.INT)
        data class CaosFloat(val value:Float) : CaosLiteral("$value", CaosExpressionValueType.FLOAT)
        data class CaosAnimationString(val value:String, val animation:CaosAnimation?) : CaosLiteral(value, CaosExpressionValueType.ANIMATION)
        data class CaosToken(val value:String) : CaosLiteral(value, CaosExpressionValueType.TOKEN)
        data class CaosIntRange(private val minIn:Int?, private val maxIn:Int?) : CaosLiteral("[$minIn...$maxIn]", CaosExpressionValueType.INT) {
            val min:Int? by lazy {
                if (minIn == null || maxIn == null)
                    min
                else
                    min(minIn, maxIn)
            }
            val max:Int? by lazy {
                if (minIn == null || maxIn == null)
                    max
                else
                    max(minIn, maxIn)
            }
        }
        data class CaosPictDimension(val width:Int, val height:Int) : CaosLiteral("${width}x${height}", CaosExpressionValueType.PICT_DIMENSION)
    }
    object CaosVarNull : CaosVar("[NULL]", CaosExpressionValueType.NULL)
    object CaosVarNone : CaosVar("{NONE}", CaosExpressionValueType.UNKNOWN)
}

val CaosVar.commandString:String? get() = (this as? CaosVar.CaosCommandCall)?.text

sealed class CaosAnimation {
    data class Animation(val poseList:List<Int>, val repeats:Boolean, val repeatsFrom:Int? = null) : CaosAnimation()
    data class ErrorAnimation(val errorMessage:String) : CaosAnimation()
}

val CaosVar.isNumeric get() = this is CaosVar.CaosLiteral.CaosInt || this is CaosVar.CaosLiteral.CaosFloat