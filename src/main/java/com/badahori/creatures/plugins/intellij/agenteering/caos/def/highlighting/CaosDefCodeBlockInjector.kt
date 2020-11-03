package com.badahori.creatures.plugins.intellij.agenteering.caos.def.highlighting

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefCodeBlock
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.impl.CaosDefCodeBlockImpl
import com.badahori.creatures.plugins.intellij.agenteering.utils.nullIfEmpty
import com.intellij.openapi.util.ProperTextRange
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.util.IncorrectOperationException
import kotlin.math.max
import kotlin.math.min

class CaosDefCodeBlockInjector : LanguageInjector {
    override fun getLanguagesToInject(host: PsiLanguageInjectionHost, injectionPlacesRegistrar: InjectedLanguagePlaces) {
        if (host !is CaosDefCodeBlock)
            return
        val text = host.text.substring(2, host.text.length - 2).toUpperCase().nullIfEmpty() ?: return
        val prefix = text.substring(0, max(1, min(4, text.length)))
        var suffix = when (prefix) {
            "DOIF" -> if (!text.endsWith("ENDI")) "endi" else null
            "ENUM","ETCH","ESEE","ECON" -> if (!text.endsWith("NEXT")) "next" else null
            "ESCN" -> if (!text.endsWith("NSCN")) "nscn" else null
            "LOOP" -> if (!text.contains("EVER") && !text.contains("UNTL")) " ever" else null
            "REPS" -> if (!text.endsWith("REPE")) "repe" else null
            else -> null
        }
        val codePrefix = if (suffix == null) {
            val matches = CaosDefCommandElementsByNameIndex.Instance[prefix, host.project]
            when {
                matches.any { it.isCommand } -> null
                matches.any { it.isRvalue } -> "setv var0 "
                matches.any { it.isLvalue } -> {
                    suffix = " 0"
                    "setv"
                }
                else -> null
            }
        } else null
        //injectionPlacesRegistrar.addPlace(CaosScriptLanguage, TextRange(2, host.textLength - 1), codePrefix, " $suffix")
    }
}


class CaosDefCodeBlockStringEscaper(block: CaosDefCodeBlock) : LiteralTextEscaper<CaosDefCodeBlock>(block) {
    override fun isOneLine(): Boolean {
        return false
    }

    override fun getOffsetInHost(offsetInDecoded: Int, rangeInsideHost: TextRange): Int {
        ProperTextRange.assertProperRange(rangeInsideHost)
        var offset = offsetInDecoded
        offset += rangeInsideHost.startOffset
        if (offset < rangeInsideHost.startOffset) offset = rangeInsideHost.startOffset
        if (offset > rangeInsideHost.endOffset) offset = rangeInsideHost.endOffset
        return offset
    }

    override fun decode(rangeInsideHost: TextRange, outChars: StringBuilder): Boolean {
        ProperTextRange.assertProperRange(rangeInsideHost);
        outChars.append(myHost.text, rangeInsideHost.startOffset, rangeInsideHost.endOffset);
        return true;
    }

}

class CaosDefCodeBlockManipulator : AbstractElementManipulator<CaosDefCodeBlockImpl>() {
    @Throws(IncorrectOperationException::class)
    override fun handleContentChange(psi: CaosDefCodeBlockImpl, range: TextRange, newContent: String): CaosDefCodeBlockImpl? {
        val oldText: String = psi.text
        val newText = oldText.substring(0, range.startOffset) + newContent + oldText.substring(range.endOffset)
        return psi.updateText(newText) as? CaosDefCodeBlockImpl
    }

    override fun getRangeInElement(element: CaosDefCodeBlockImpl): TextRange {
        return getStringTokenRange(element)
    }

    companion object {
        fun getStringTokenRange(element: CaosDefCodeBlockImpl): TextRange {
            return TextRange.from(1, element.textLength - 2)
        }
    }
}