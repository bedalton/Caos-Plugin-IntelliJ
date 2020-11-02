package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

interface CaosScriptStringLike : CaosScriptCompositeElement{
    val stringValue:String
    val isClosed:Boolean
}