package org.badahori.creatures.plugins.caos.compilers.general

private var nextId:Int = 1;
data class CaosC1Var(val id:Int = nextId++, val firstUse:Int, val lastUsed:Int, val caosVar: CaosC1VarName)

interface CaosVarValue<T> {
    val value:T
}

data class CaosTokenValue(override val value:String): CaosVarValue<String>
data class CaosStringValue(override val value:String): CaosVarValue<String>
data class CaosIntValue(override val value:Int): CaosVarValue<Int>
data class CaosDoubleValue(override val value:Double): CaosVarValue<Double>
data class CaosVarReferenceValue(override val value:Int) : CaosVarValue<Int>
data class CaosVarPointer(override val value:Int) : CaosVarValue<Int>

data class CaosVarUses(val id:Int, val tokenIndex:Int, val lastUsed:Int)

data class Expr(val var1: CaosVarValue<*>, val var2: CaosVarValue<*>, val operation: CaosOperation, val tokenIndex:Int)

enum class CaosOperation {
    ADD,
    SUB,
    MULT,
    DIV,
    MOD,
    ASSIGN,
    ADD_ASSIGN,
    MULT_ASSIGN,
    DIV_ASSIGN,
    MOD_ASSIGN
}

enum class CaosC1VarName(val varString:String) {
    VAR0("var1"),
    VAR1("var1"),
    VAR2("var2"),
    VAR3("var3"),
    VAR4("var4"),
    VAR5("var5"),
    VAR6("var6"),
    VAR7("var7"),
    VAR8("var8"),
    VAR9("var9"),
    P1("_P1_"),
    P2("_P2_");
}