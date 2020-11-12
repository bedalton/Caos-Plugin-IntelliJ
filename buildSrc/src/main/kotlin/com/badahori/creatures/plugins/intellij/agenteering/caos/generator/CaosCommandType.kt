package com.badahori.creatures.plugins.intellij.agenteering.caos.generator


internal enum class CaosCommandType(val value: String) {
    COMMAND("Command"),
    RVALUE("RValue"),
    LVALUE("LValue"),
    CONTROL_STATEMENT("Control Statement"),
    UNDEFINED("???");
}