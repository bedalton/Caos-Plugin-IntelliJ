package com.badahori.creatures.plugins.intellij.agenteering.caos.hints

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.EqOp
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.EqOp.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getValuesList
import com.badahori.creatures.plugins.intellij.agenteering.utils.*
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

class CaosScriptDoifFoldingBuilder : FoldingBuilderEx(), DumbAware {

    /**
     * Gets command call placeholder text for folding if it should be folded
     */
    override fun getPlaceholderText(node: ASTNode): String? {
        (node.psi as? CaosScriptEqualityExpressionPrime)?.let {expression ->
            expression.getUserData(CACHE_KEY)?.let {
                if (it.first == expression.text)
                    return it.second
            }
            if (shouldFold(expression))
                return getDoifFold(expression)
        }
        return null
    }

    /**
     * Gets the folded text for this command call
     */
    private fun getDoifFold(expression: CaosScriptEqualityExpressionPrime): String? {
        val variant = expression.variant
                ?: return null

        val eqOp = EqOp.fromValue(expression.eqOp.text)
        if (eqOp == INVALID) {
            expression.putUserData(CACHE_KEY, Pair(expression.text, null))
            return null
        }
        val text = getDoifFoldingExpression(variant, expression, eqOp, expression.second, expression.first, false)
                ?: getDoifFoldingExpression(variant, expression, eqOp, expression.first, expression.second, true)
        expression.putUserData(CACHE_KEY, Pair(expression.text, text))
        return text
    }

    private fun getDoifFoldingExpression(variant: CaosVariant, expression: CaosScriptEqualityExpressionPrime, eqOp: EqOp, value: CaosScriptRvalue, other:CaosScriptRvalue, reversed:Boolean) : String? {
        val rawValueText = value.text
        val positive = (rawValueText == "0" && (eqOp == NOT_EQUAL || eqOp == GREATER_THAN))
                || (rawValueText == "1" && eqOp == EQUAL)
        val negative = (value.text == "0" && eqOp == EQUAL)
        val otherCommand = other.commandStringUpper
                ?: return null
        when (otherCommand) {
            "TOUC" -> getToucFold(positive, negative, other.arguments)
            "SEEE" -> getSeeFold(positive, negative, other.arguments)
            else->null
        }?.let { return it }

        val thisValue = formatThisValue(variant, value.rvaluePrime) ?: other.text
        val otherValue = formatOtherValue(variant, expression, eqOp, value) ?: value.text

        val eqOpText = when (eqOp) {
            EQUAL -> "is"
            NOT_EQUAL -> "is not"
            else -> expression.eqOp.text
        }
        val first = if (!reversed) thisValue else (otherValue ?: other.text)
        val second = if(!reversed) (otherValue ?: other.text) else thisValue
        val default = "$first $eqOpText $second"
        if (otherValue == null) {
            return default
        }
        when (otherCommand) {
            "CAGE" -> when (eqOp) {
                EQUAL -> "Is a $otherValue"
                NOT_EQUAL -> "Is not a $otherValue"
                GREATER_THAN -> "Is older than a $otherValue"
                LESS_THAN -> "Is younger than a $otherValue"
                GREATER_THAN_EQUAL -> "Is $otherValue or Older"
                LESS_THAN_EQUAL -> "Is $otherValue or Younger"
                else -> null
            }
            "TIME", "SEAN", "TMOD" -> when (eqOp) {
                EQUAL -> "Is $otherValue"
                NOT_EQUAL -> "Is not $otherValue"
                GREATER_THAN -> "Is later than $otherValue"
                LESS_THAN -> "Is earlier than $otherValue"
                GREATER_THAN_EQUAL -> "Is $otherValue or Later"
                LESS_THAN_EQUAL -> "Is $otherValue or Earlier"
                else -> null
            }
            "DEAD", "ASLP", "DREAM" -> when (eqOp) {
                EQUAL -> "is $otherValue"
                NOT_EQUAL -> "is not $otherValue"
                else -> null
            }
            "GRAV" -> when (eqOp) {
                EQUAL -> "Gravity is $otherValue"
                NOT_EQUAL -> "Gravity is not $otherValue"
                else -> null
            }
            "FRZN" -> when(eqOp) {
                EQUAL -> if (otherValue like "true")
                    "Is Frozen"
                else
                    "Is Not Frozen"
                NOT_EQUAL -> if (otherValue like "true")
                    "Is Not Frozen"
                else
                    "Is Frozen"
                else -> null
            }
            "ISAR" -> value.rvaluePrime?.arguments?.firstOrNull()?.text?.toIntSafe()?.let {
                "Room #$it $otherValue"
            } ?: "${value.text} $otherValue"
            else -> null
        }?.let {
            return it
        }
        return default

    }

    private fun formatOtherValue(variant:CaosVariant, expression: CaosScriptEqualityExpressionPrime, eqOp: EqOp, value:CaosScriptRvalue) : String? {
        val valuesList = expression.getValuesList(variant, value)
                ?: return null
        val isBool = valuesList.extensionType like "bool"
        val usePositive = isBool && (eqOp == NOT_EQUAL || eqOp == GREATER_THAN)
        return (if (usePositive)
            valuesList[1] ?: valuesList.values.firstOrNull { it.value.notEquals("0", false) }
        else
            valuesList[value.text])
                ?.name
                .nullIfEmpty()
    }

    private fun formatThisValue(variant:CaosVariant, rvaluePrime:CaosScriptRvaluePrime?) : String? {
        if (rvaluePrime == null)
            return null
        val commandString = rvaluePrime.commandStringUpper
        if (commandString != "CHEM" && commandString != "DRIV")
            return null
        val chemicalIndex = rvaluePrime.arguments.firstOrNull()?.text
                ?: return null
        return when (commandString) {
            "CHEM" -> CaosLibs[variant].valuesList("Chemicals")?.get(chemicalIndex)?.name
            "DRIV" -> CaosLibs[variant].valuesList("Drives")?.get(chemicalIndex)?.name
            else -> null
        }
    }

    private fun getToucFold(positive:Boolean, negative:Boolean, arguments:List<CaosScriptArgument>) : String? {
        if (arguments.size < 2)
            return null
        val first = arguments.first().text.let {
            if (it.contains(" "))
                "($it)"
            else
                it
        }
        val second = arguments.last().text.let {
            if (it.contains(" "))
                "($it)"
            else
                it
        }
        if (positive) {
            return "$first touching $second"
        }
        if (negative) {
            return "$first not touching $second"
        }
        return null
    }

    private fun getSeeFold(positive:Boolean, negative:Boolean, arguments:List<CaosScriptArgument>) : String? {
        if (arguments.size < 2)
            return null
        val first = arguments.first().text.let {
            if (it.contains(" "))
                "($it)"
            else
                it
        }
        val second = arguments.last().text.let {
            if (it.contains(" "))
                "($it)"
            else
                it
        }
        if (positive) {
            return "$first can see $second"
        }
        if (negative) {
            return "$first cannot see $second"
        }
        return null
    }

    /**
     * Base method to build the regions that should be folded
     */
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val group = FoldingGroup.newGroup("CaosScript_STIM_FOLDING")
        // Get a collection of the literal expressions in the document below root
        val children = PsiTreeUtil.findChildrenOfType(root, CaosScriptEqualityExpressionPrime::class.java)
        return children
                .filter {
                    shouldFold(it)
                }
                .mapNotNull {
                    ProgressIndicatorProvider.checkCanceled()
                    getFoldingRegion(it, group)
                }
                .toTypedArray()
    }

    // Helper function to get actual folding regions for command calls
    private fun getFoldingRegion(expression: CaosScriptEqualityExpressionPrime, group: FoldingGroup): FoldingDescriptor? {
        if (!shouldFold(expression))
            return null
        return FoldingDescriptor(expression.node, expression.textRange, group)
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        return true
    }

    private fun shouldFold(expression: CaosScriptEqualityExpressionPrime): Boolean {
        val variant = expression.variant
                ?: return false
        expression.getUserData(CACHE_KEY)?.let {
            if (it.first == expression.text)
                return it.second != null
        }
        val first = expression.first
                ?: return false
        val second = expression.second
                ?: return false
        if (expression.getValuesList(variant, first) != null || expression.getValuesList(variant, second) != null)
            return true
        return listOf(first.commandStringUpper, second.commandStringUpper).intersect(listOf("CHEM", "DRIV")).isNotEmpty()
    }

    companion object {
        private val CACHE_KEY = Key<Pair<String,String?>>("com.badahori.creatures.plugins.intellij.agenteering.caos.DOIF_FOLDING_STRING")
    }

}