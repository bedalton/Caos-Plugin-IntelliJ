package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefValuesListElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.lang.CaosDefFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.stubs.api.variants
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.CaosCommandType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getEnclosingCommandType
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getNextNonEmptySibling
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getPreviousNonEmptySibling
import com.badahori.creatures.plugins.intellij.agenteering.utils.equalsIgnoreCase
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

class CaosScriptCommandTokenReference(element: CaosScriptIsCommandToken) : PsiPolyVariantReferenceBase<CaosScriptIsCommandToken>(element, TextRange(0, element.text.length)) {

    private val name: String by lazy {
        element.name ?: "{{UNDEF}}"
    }

    private val VARx = "[Vv][Aa][Rr][0-9]".toRegex()
    private val VAxx = "[Vv][Aa][0-9][0-9]".toRegex()
    private val OBVx = "[Oo][Bb][Vv][0-9]".toRegex()
    private val OVxx = "[Oo][Vv][0-9][0-9]".toRegex()
    private val MVxx = "[Mm][Vv][0-9][0-9]".toRegex()

    private val variants: List<CaosVariant> by lazy {
        when (myElement) {
            is CaosDefCompositeElement -> myElement.variants
            is CaosScriptCompositeElement -> myElement.containingCaosFile?.variant?.let { listOf(it) } ?: emptyList()
            else -> emptyList()
        }
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element is CaosScriptVarToken && element.varGroup.value.toUpperCase() == name.toUpperCase()) {
            return true
        }
        if (element.text.equalsIgnoreCase(name)) {
            return when (element) {
                is CaosDefCompositeElement -> if (element.variantsIntersect(variants)) {
                    if (myElement.parent is CaosDefCodeBlock || myElement.parent?.parent is CaosDefWordLink) {
                        element.parent?.parent is CaosDefCommandDefElement
                    } else
                        false
                } else {
                    false
                }
                is CaosScriptCompositeElement -> element.containingCaosFile?.variant in variants
                else -> false
            }
        }
        return super.isReferenceTo(element)
    }

    override fun multiResolve(partial: Boolean): Array<ResolveResult> {
        if (DumbService.isDumb(myElement.project))
            return emptyArray()
        if (myElement.parent?.parent is CaosDefCommandDefElement) {
            return PsiElementResolveResult.EMPTY_ARRAY
        }
        val elements = if (myElement is CaosDefCompositeElement)
            findFromDefElement()
        else
            findFromScriptElement()
        return PsiElementResolveResult.createResults(elements)
    }

    private fun findFromDefElement(): List<PsiElement> {
        if (myElement.parent is CaosDefCodeBlock) {
            return getCommandFromCodeBlockCommand()
        }
        val parentCommand = myElement.parent as? CaosDefCommand
                ?: return emptyList()
        val index = parentCommand.commandWordList.indexOf(myElement)
        val variants = (myElement.containingFile as? CaosDefFile)?.variants
        return CaosDefCommandElementsByNameIndex
                .Instance[parentCommand.commandWordList.joinToString(" ") { it.text }, myElement.project]
                .filter {
                    ProgressIndicatorProvider.checkCanceled()
                    it.variants.intersect(variants.orEmpty()).isNotEmpty()
                }.mapNotNull {
                    it.command.commandWordList.getOrNull(index)
                }
                .filter { it != myElement }
    }

    private fun findFromScriptElement(): List<CaosDefCommandWord> {
        val type = myElement.getEnclosingCommandType()
        val formattedName = name.replace(EXTRA_SPACE_REGEX, " ")
        val variant = myElement.containingCaosFile?.variant
                ?: return emptyList()
        val matches = CaosDefCommandElementsByNameIndex
                .Instance[formattedName, myElement.project]
                // Filter for type and variant
                .filter {
                    ProgressIndicatorProvider.checkCanceled()
                    val isVariant = it.isVariant(variant)
                    if (!isVariant)
                        return@filter false
                    val isForElement = when (type) {
                        CaosCommandType.COMMAND -> it.isCommand
                        CaosCommandType.CONTROL_STATEMENT -> it.isCommand
                        CaosCommandType.RVALUE -> it.isRvalue
                        CaosCommandType.LVALUE -> it.isLvalue
                        CaosCommandType.UNDEFINED -> it.isRvalue
                    }
                    if (!isForElement) {
                        if (!(it.isLvalue || it.isRvalue || it.isCommand)) {
                            return@filter false
                        }
                    }
                    isForElement
                }
        if (matches.size <= 1)
            return matches
                    .mapNotNull {
                        it.command.commandWordList.getOrNull(0)
                    }
        val parentArgument = myElement.getParentOfType(CaosScriptArgument::class.java)
                ?: return matches
                        .mapNotNull {
                            it.command.commandWordList.getOrNull(0)
                        }
        val out = when (parentArgument) {
            is CaosScriptExpectsInt -> matches.filter { it.returnTypeString.toLowerCase().startsWith("int") }
            is CaosScriptExpectsDecimal -> matches.filter { it.returnTypeString.toLowerCase() == "decimal" || it.returnTypeString.toLowerCase().startsWith("int") || it.returnTypeString.toLowerCase() == "float" }
            is CaosScriptExpectsFloat -> matches.filter { it.returnTypeString.toLowerCase() == "decimal" || it.returnTypeString.toLowerCase().startsWith("int") || it.returnTypeString.toLowerCase() == "float" }
            is CaosScriptExpectsQuoteString -> matches.filter { it.returnTypeString.toLowerCase() == "string" }
            else -> matches
        }
        return out.mapNotNull {
            it.command.commandWordList.getOrNull(0)
        }
    }

    private fun getCommandFromCodeBlockCommand(): List<PsiElement> {
        val caseName = when {
            VARx.matches(name) -> "[Vv][Aa][Rr][Xx]"
            VAxx.matches(name) -> "[Vv][Aa][Xx][Xx]"
            OBVx.matches(name) -> "[Oo][Bb][Vv][Xx]"
            OVxx.matches(name) -> "[Oo][Vv][Xx][Xx]"
            MVxx.matches(name) -> "[Mm][Vv][Xx][Xx]"
            else -> name.toCharArray().joinToString("") {
                val lowerCase = it.toLowerCase()
                val upperCase = it.toUpperCase()
                if (lowerCase != upperCase)
                    "[$lowerCase$upperCase]"
                else if (it == '!' || it == '$')
                    "\\$it"
                else
                    "$it"
            }
        }
        if (name.equalsIgnoreCase("CNAM") || name.equalsIgnoreCase("DATA")) {
            if ((myElement.getPreviousNonEmptySibling(false) as? CaosDefCodeBlockPrimitive)?.codeBlockString != null)
                return CaosDefValuesListElementsByNameIndex.Instance["PutBOption", myElement.project]
                        .filter {
                            it.variantsIntersect(variants)
                        }
                        .mapNotNull { valuesListElement ->
                            valuesListElement.valuesListValueList.firstOrNull { it.key.equalsIgnoreCase(name) }
                        }
        }
        val rawMatches = CaosDefCommandElementsByNameIndex
                .Instance.getByPatternFlat("([^ ]{4}[ ])*$caseName([ ][^ ]{4})*", myElement.project)
        val multiMatches = rawMatches
                .filter { commandDefElement ->
                    if (!commandDefElement.variantsIntersect(variants))
                        return@filter false
                    val words = commandDefElement.commandWords.map { it.toLowerCase() }
                    if (words.size < 2)
                        return@filter false
                    matchesWords(words)
                }
                .map {
                    val index = it.commandWords.indexOf(name)
                    it.command.commandWordList.getOrNull(index) ?: it.command
                }
        if (multiMatches.isNotEmpty())
            return multiMatches
        return CaosDefCommandElementsByNameIndex
                .Instance.getByPatternFlat(caseName, myElement.project)
                .filter {
                    ProgressIndicatorProvider.checkCanceled()
                    it.variants.intersect(variants).isNotEmpty()
                }.map {
                    it.command
                }
                .filter { it != myElement }
    }

    private fun matchesWords(words:List<String>) : Boolean {
        when (words.indexOf(name.toLowerCase())) {
            0 -> {
                var next: CaosDefCommandWord? = myElement as? CaosDefCommandWord
                for (word in words) {
                    if (next == null || !next.text.equalsIgnoreCase(word))
                        return false
                    next = next.getNextNonEmptySibling(false) as? CaosDefCommandWord
                }
            }
            1 -> {
                (myElement.getPreviousNonEmptySibling(false) as? CaosDefCommandWord)?.let {
                    if (!it.text.equalsIgnoreCase(words[0]))
                        return false
                }
                        ?: return false
                if (words.size == 2)
                    return true
                var next: CaosDefCommandWord? = myElement as? CaosDefCommandWord
                for (word in words.subList(1, words.size)) {
                    if (next == null || !next.text.equalsIgnoreCase(word))
                        return false
                    next = next.getNextNonEmptySibling(false) as? CaosDefCommandWord
                }
            }
            2 -> {
                var previous: CaosDefCommandWord? = myElement as? CaosDefCommandWord
                for (word in words.reversed()) {
                    if (previous == null || !previous.text.equalsIgnoreCase(word))
                        return false
                    previous = previous.getPreviousNonEmptySibling(false) as? CaosDefCommandWord
                }
            }
            else -> return false
        }
        return true
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        //if (renameRegex.matches(newElementName))
        //  return myElement.setName(newElementName)
        return myElement
    }

    companion object {
        val EXTRA_SPACE_REGEX = "\\s\\s+".toRegex()
    }

}