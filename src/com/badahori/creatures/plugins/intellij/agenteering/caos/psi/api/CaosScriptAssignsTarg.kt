package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

interface CaosScriptAssignsTarg : CaosScriptCompositeElement {
    fun hasTargClass(family:Int, genus:Int, species:Int) : Boolean
}