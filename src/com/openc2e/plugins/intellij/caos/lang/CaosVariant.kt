package com.openc2e.plugins.intellij.caos.lang

enum class CaosVariant(val code:String, val fullName:String, val index:Int) {
    C1("C1", "Creatures 1", 1),
    C2("C2", "Creatures 2", 2),
    CV("CV", "Creatures Village", 3),
    C3("C3", "Creatures 3", 4),
    DS("DS", "Docking Station", 5),
    SM("SM", "Sea Monkeys", 6),
    UNKNOWN("??", "Unknown", -1);

    companion object {
        fun fromVal(variant:String) :CaosVariant {
            return when (variant) {
                "C1" -> C1
                "C2" -> C2
                "CV" -> CV
                "C3" -> C3
                "DS" -> DS
                "SM" -> SM
                else -> UNKNOWN
            }
        }
    }


}