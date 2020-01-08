package com.openc2e.plugins.intellij.caos.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCommandToken
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCompositeElement

object CaosScriptCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, resultSet: CompletionResultSet) {
        val element = parameters.position as? CaosScriptCompositeElement
                ?: return;
        val variant = element.containingCaosFile.variant.toUpperCase();
        when {
            element is CaosScriptCommandToken -> {
                CaosDefCommandElementsByNameIndex.Instance.getAllKeys(element.project).forEach {
                    resultSet.addElement(LookupElementBuilder.create(it))
                }
                when (variant) {
                    "C1" -> {
                        (0..2).forEach {
                            resultSet.addElement(LookupElementBuilder.create("obv$it"));
                        }
                        (0..9).forEach {
                            resultSet.addElement(LookupElementBuilder.create("var$it"));
                        }
                    }
                    "C2" -> {
                        (0..9).forEach {
                            resultSet.addElement(LookupElementBuilder.create("obv$it"));
                        }
                        (0..9).forEach {
                            resultSet.addElement(LookupElementBuilder.create("var$it"));
                        }
                    }
                }
                if (variant != "C1" && element.text.substringBefore("zz").matches("(va|[om][v])[0-9]".toRegex())) {
                    val items = (0..99).map {
                        "$it".padStart(2, '0');
                    }
                    items.map {
                        resultSet.addElement(LookupElementBuilder.create("va$it"))
                        resultSet.addElement(LookupElementBuilder.create("ov$it"))
                    }
                    if (variant != "C2") {
                        items.map {
                            resultSet.addElement(LookupElementBuilder.create("mv$it"))
                        }
                    }
                }
            }
        }
    }

}