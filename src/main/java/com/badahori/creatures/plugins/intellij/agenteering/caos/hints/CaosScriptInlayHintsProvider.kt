@file:Suppress("UnstableApiUsage")

package com.badahori.creatures.plugins.intellij.agenteering.caos.hints

import com.badahori.creatures.plugins.intellij.agenteering.common.InlayHintGenerator
import com.badahori.creatures.plugins.intellij.agenteering.common.AbstractInlayHintsProvider

class CaosScriptInlayHintsProvider: AbstractInlayHintsProvider() {

    override val inlayHintGenerators: List<InlayHintGenerator> by lazy {
        arrayOf(*CaosScriptInlayTypeHint.values(), *CaosScriptInlayParameterHintsProvider.values())
            .sortedByDescending { it.priority }
    }

}