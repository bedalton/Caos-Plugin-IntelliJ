package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

interface CaosScriptScriptElement : CaosScriptHasCodeBlock {
    val scriptTerminator:CaosScriptScriptTerminator?
}

interface CaosScriptMacroLike: CaosScriptScriptElement