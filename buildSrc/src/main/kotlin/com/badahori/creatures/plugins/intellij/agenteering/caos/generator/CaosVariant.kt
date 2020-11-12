package com.badahori.creatures.plugins.intellij.agenteering.caos.generator

internal sealed class CaosVariant(open val code: String, open val fullName: String, open val index: Int) {
    object C1 : CaosVariant("C1", "Creatures 1", 1)
    object C2 : CaosVariant("C2", "Creatures 2", 2)
    object CV : CaosVariant("CV", "Creatures Village", 3)
    object C3 : CaosVariant("C3", "Creatures 3", 4)
    object DS : CaosVariant("DS", "Docking Station", 5)
    object SM : CaosVariant("SM", "Sea Monkeys", 6)

    companion object {
        val variants by lazy {
            listOf(
                    C1,
                    C2,
                    CV,
                    C3,
                    DS,
                    SM
            )
        }
    }

    val isOld: Boolean
        get() {
            return this in VARIANT_OLD
        }
    val isNotOld: Boolean
        get() {
            return this !in VARIANT_OLD
        }

    override fun toString(): String {
        return code
    }
}

internal fun CaosVariant?.orDefault(): CaosVariant? {
    if (this != null)
        return this
    return null
}

internal val VARIANT_OLD = listOf(CaosVariant.C1, CaosVariant.C2)
