package com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.psi

import com.badahori.creatures.plugins.intellij.agenteering.bundles.pray.lang.PrayLanguage
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.CaosScriptStubBasedElementImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IElementType

class PrayElementType(
    debugName: String
) : IElementType(debugName, PrayLanguage)


class PrayTokenType(debug: String) : IElementType(debug, PrayLanguage)