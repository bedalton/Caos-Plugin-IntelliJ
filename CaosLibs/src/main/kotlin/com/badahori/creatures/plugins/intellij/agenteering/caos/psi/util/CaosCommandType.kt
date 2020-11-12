package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util


enum class CaosCommandType(val value: String) {
    COMMAND("Command"),
    RVALUE("RValue"),
    LVALUE("LValue"),
    CONTROL_STATEMENT("Control Statement"),
    UNDEFINED("???");
}