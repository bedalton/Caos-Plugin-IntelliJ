package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.stubs.api.StringStubKind

interface CaosScriptStringLike : CaosScriptCompositeElement{
    val stringValue:String
    val isClosed:Boolean
    val stringStubKind: StringStubKind
}