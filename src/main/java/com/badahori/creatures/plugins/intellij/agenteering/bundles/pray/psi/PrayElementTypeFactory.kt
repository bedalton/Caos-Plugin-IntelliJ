package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi

import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.stubs.PrayAgentBlockStubType
import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi.stubs.PrayTagStubType
import com.intellij.psi.tree.IElementType

class PrayElementTypeFactory {

    companion object {
        @JvmStatic
        fun factory(debugName: String): IElementType {
            return when (debugName) {
                "Pray_AGENT_BLOCK" -> PrayAgentBlockStubType
                "Pray_PRAY_TAG" -> PrayTagStubType
                else -> throw IndexOutOfBoundsException("PRAY token '$debugName' is not recognized")
            }
        }
    }
}