package com.openc2e.plugins.intellij.caos.psi.util

import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.caos.def.stubs.impl.CaosDefParameterStruct
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptExpressionList
import com.openc2e.plugins.intellij.caos.utils.orElse
import java.security.InvalidParameterException

class CaosScriptCommandGroups {

    fun parse(expressionList: CaosScriptExpressionList, variant: String): Boolean {
        val expressions = expressionList.expressionList
        if (expressions.isEmpty())
            return true
        val stack = mutableListOf<CaosScriptItem>()
        var currentItem: CaosScriptItem? = null
        var i = 0
        var count = expressions.size
        var skip = 0
        for (expression in expressions) {
            if (skip > 0) {
                skip--
                continue
            }
            while (currentItem != null && !currentItem.needsMore()) {
                stack.removeAt(0)
                currentItem = stack.getOrNull(0)
            }
            val commandToken = expression.commandToken
            var parameters: List<CaosDefParameterStruct>? = null

            if (commandToken != null) {
                val inCommand = currentItem?.consume.orElse(0) > 0
                var commandMatches = commandToken.reference
                        .multiResolve(true)
                        .mapNotNull { it.element as? CaosDefCommandDefElement }
                        .filter { variant.isEmpty() || it.isVariant(variant) }
                if (inCommand) {
                    val variables = commandMatches.filterNot {
                        it.isCommand
                    }
                    if (variables.isNotEmpty())
                        commandMatches = variables
                } else {
                    val variables = commandMatches.filter {
                        it.isCommand
                    }
                    if (variables.isNotEmpty())
                        commandMatches = variables
                }
                val parameterStructs = commandMatches.map { it.parameterStructs }
                val min = parameterStructs.map { it.size }.min().orElse(0)
                parameters = (0 until min).map { i ->
                    val set = parameterStructs.map { it[i] }.toSet()
                    set.first()
                }
            }
            val item =
            if (currentItem != null) {

            }

        }
    }
}

data class CaosScriptItem(val element: PsiElement, val parameters: List<CaosDefParameterStruct>?, var consumed: Int) {
    internal val consume: Int = parameters?.size.orElse(0)
    private val childElements = mutableListOf<CaosScriptItem>()
    val children: List<CaosScriptItem> get() = childElements
    fun addChild(item: CaosScriptItem) {
        if (++consumed > consume)
            throw InvalidParameterException("Consumed too many items")
        childElements.add(item)
    }

    val currentParam: CaosDefParameterStruct?
        get() {
            return parameters?.get(consumed)
        }

    fun needsMore() = (consume - consumed) > 1
}