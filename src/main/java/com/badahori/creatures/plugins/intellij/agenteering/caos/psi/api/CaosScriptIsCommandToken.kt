package com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.references.CaosScriptCommandTokenReference
import com.badahori.creatures.plugins.intellij.agenteering.utils.equalsIgnoreCase
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import bedalton.creatures.util.toListOf
import com.badahori.creatures.plugins.intellij.agenteering.utils.tokenType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.elementType

interface CaosScriptIsCommandToken : PsiNamedElement, CaosScriptCompositeElement, CaosScriptShouldBeLowerCase {
    fun isVariant(variants:List<CaosVariant>, strict:Boolean) : Boolean
    val reference:CaosScriptCommandTokenReference
    val commandString:String
}

interface CaosScriptIsSuffixToken : CaosScriptCompositeElement
interface CaosScriptIsPrefixToken : CaosScriptCompositeElement

infix fun CaosScriptIsCommandToken?.equalTo(otherValue:String) : Boolean = this?.text?.equalsIgnoreCase(otherValue) ?: false
infix fun CaosScriptIsCommandToken?.notEqualTo(otherValue:String) : Boolean = this?.text?.equalsIgnoreCase(otherValue) ?: false

val CaosScriptIsCommandToken.commandTokenElements: List<PsiElement> get() {
    return children.filter { it.elementType == TokenType.WHITE_SPACE }
        .map {
            (it.firstChild?.firstChild?.firstChild ?: it.firstChild?.firstChild ?: it.firstChild ?: it)
        }
        .nullIfEmpty()
        ?: firstChild?.toListOf()
        ?: listOf(this)
}

val CaosScriptIsCommandToken.commandTokenElementTokens: List<IElementType> get() {
    return children
        .filter { it.elementType == TokenType.WHITE_SPACE }
        .nullIfEmpty()
        ?.map {
            (it.firstChild?.firstChild?.firstChild ?: it.firstChild?.firstChild ?: it.firstChild ?: it).tokenType
        }
        ?: firstChild
            ?.elementType
            ?.toListOf()
        ?: this.elementType!!.toListOf()
}