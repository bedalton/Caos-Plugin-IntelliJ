package com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang

import com.intellij.spellchecker.BundledDictionaryProvider

class CaosDictionary: BundledDictionaryProvider {
    override fun getBundledDictionaries(): Array<String> {
        return arrayOf("CAOS.dic")
    }
}