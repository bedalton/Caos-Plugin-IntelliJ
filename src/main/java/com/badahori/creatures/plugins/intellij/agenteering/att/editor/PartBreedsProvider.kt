package com.badahori.creatures.plugins.intellij.agenteering.att.editor

import com.bedalton.creatures.common.structs.BreedKey

interface PartBreedsProvider {
    fun getPartBreed(part: Char?): BreedKey?
}