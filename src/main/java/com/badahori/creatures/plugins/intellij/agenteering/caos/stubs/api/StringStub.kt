package com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api


enum class StringStubKind {
    GAME,
    EAME,
    NAME,
    JOURNAL;

    override fun toString(): String {
        return name
    }

    companion object {
        fun fromString(value: String?): StringStubKind? {
            if (value.isNullOrBlank()) {
                return null
            }
            return when (value) {
                "GAME" -> GAME
                "EAME" -> EAME
                "NAME" -> NAME
                "JOURNAL" -> JOURNAL
                else -> null
            }
        }
    }
}