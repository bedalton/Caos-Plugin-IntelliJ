package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefValuesListElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getPreviousNonEmptySibling
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getSelfOrParentOfType
import com.badahori.creatures.plugins.intellij.agenteering.utils.equalsIgnoreCase
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.badahori.creatures.plugins.intellij.agenteering.utils.toIntSafe
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

class CaosScriptExpressionReference(element: CaosScriptExpression) : PsiPolyVariantReferenceBase<CaosScriptExpression>(element, TextRange.create(0, element.textLength)) {

    private val text by lazy { myElement.text }

    private val possibleValuesListNames: List<String> by lazy lazy@{
        // If expression is in equality expression
        if (myElement.parent is CaosScriptEqualityExpression) {
            return@lazy possibleValuesListNamesForEquality()
        }

        val argument = myElement.getParentOfType(CaosScriptArgument::class.java)
                ?: return@lazy  emptyList<String>()
        val parentRaw = argument.parent as? CaosScriptCommandElement
                ?: return@lazy  emptyList<String>()
        // If parent is assignment, get other side values list
        if (parentRaw is CaosScriptCAssignment) {
            possibleValuesListNamesForAssignment(parentRaw)
        } else {
            possibleValuesListNamesForParameter(argument, parentRaw)
        }

    }

    private fun possibleValuesListNamesForAssignment(assignment:CaosScriptCAssignment) : List<String> {
        val commandToken = assignment.lvalue?.commandToken
                ?: return emptyList()
        return commandToken
                .reference
                .multiResolve(true)
                .mapNotNull { it.element
                        ?.getSelfOrParentOfType(CaosDefCommandDefElement::class.java)
                        ?.returnTypeStruct
                        ?.type
                        ?.valuesList
                }
    }

    private fun possibleValuesListNamesForParameter(argument:CaosScriptArgument, parentRaw:CaosScriptCommandElement) : List<String> {
        // Expression is in parameter
        val commandToken = parentRaw.commandToken
                ?: return emptyList()
        // Find command definition
        val commandDef = commandToken
                .reference
                .multiResolve(true)
                .mapNotNull { it.element?.getSelfOrParentOfType(CaosDefCommandDefElement::class.java) }
                .ifEmpty { null }
                ?: return emptyList()

        // Get values list for argument position
        val argumentNumber = argument.index
        return commandDef.mapNotNull map@{ def ->
            val parameters = def.parameterStructs
            val parameter = parameters.getOrNull(argumentNumber)
                    ?: return@map null
            if (parameter.name.equalsIgnoreCase("genus") && parameters.getOrNull(argumentNumber - 1)?.name?.equalsIgnoreCase("family").orFalse()) {
                "Genus"
            } else
                parameter.type.valuesList
        }
    }

    private fun possibleValuesListNamesForEquality() : List<String> {
        val expression = myElement
        val equalityExpression = expression.parent as? CaosScriptEqualityExpression ?: return emptyList()
        val other = equalityExpression.expressionList.firstOrNull { it != expression }
                ?: return emptyList()
        val token = other.rvaluePrime?.getChildOfType(CaosScriptIsCommandToken::class.java)
                ?: return emptyList()
        return token
                .reference
                .multiResolve(true)
                .mapNotNull { it.element?.getSelfOrParentOfType(CaosDefCommandDefElement::class.java)?.returnTypeStruct?.type?.valuesList }
    }

    private val project: Project by lazy { myElement.project }

    override fun isReferenceTo(anElement: PsiElement): Boolean {
        if (anElement !is CaosDefValuesListValueKey) {
            return false
        }
        if (anElement.text != text)
            return false
        val parentDef = anElement.getParentOfType(CaosDefValuesListElement::class.java)
                ?: return false
        return parentDef.typeName in possibleValuesListNames
    }

    override fun multiResolve(p0: Boolean): Array<ResolveResult> {
        val variant = myElement.containingCaosFile?.variant
                ?: return ResolveResult.EMPTY_ARRAY
        val keyAsInt = text.toIntSafe()
        val keys = possibleValuesListNames.flatMap map@{ valuesListName ->
            CaosDefValuesListElementsByNameIndex.Instance[valuesListName, project]
                    .filter {
                        it.isVariant(variant)
                    }
                    .flatMap matchKeys@{ valuesListElement ->
                        if (valuesListElement.isBitflags && keyAsInt != null) {
                            valuesListElement.valuesListValueList.filter { valuesListValueStruct ->
                                valuesListValueStruct.key.toIntSafe()?.let { key -> keyAsInt and key > 0 } ?: false
                            }
                        } else if (valuesListName == "Genus") {
                            val family = myElement.getPreviousNonEmptySibling(false)?.text?.toIntSafe() ?: return@matchKeys emptyList<CaosDefValuesListValue>()
                            valuesListElement.valuesListValueList.filter { it.key == "$family $keyAsInt" }
                        } else {
                            valuesListElement.valuesListValueList.filter { it.key == text }
                        }
                    }
        }.ifEmpty { null }
                ?: return PsiElementResolveResult.EMPTY_ARRAY
        return PsiElementResolveResult.createResults(keys)
    }
}