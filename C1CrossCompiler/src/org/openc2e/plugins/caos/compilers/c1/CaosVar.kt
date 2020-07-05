
private var nextId:Int = 1;

data class CaosC1Var(val id:Int = nextId++, val referenceName:String, val caosVar:CaosC1VarName? = null, val uses:MutableList<Int> = mutableListOf())


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