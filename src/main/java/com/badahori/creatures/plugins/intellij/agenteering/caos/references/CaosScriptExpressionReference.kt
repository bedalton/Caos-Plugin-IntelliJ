package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefValuesListElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefValuesListElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefValuesListValue
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.CaosDefValuesListValueKey
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.isVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosLibs
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosValuesList
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.variant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getPreviousNonEmptySibling
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getValuesList
import com.badahori.creatures.plugins.intellij.agenteering.utils.toIntSafe
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

class CaosScriptExpressionReference(element: CaosScriptRvalue) : PsiPolyVariantReferenceBase<CaosScriptRvalue>(element, TextRange.create(0, element.textLength)) {

    private val text by lazy { myElement.text }

    private val possibleValuesListName: String? by lazy lazy@{
        getValuesListDefinition()?.let { valuesList ->
            // Only cache list name, if it actually contains a valid entry for this value
            if (valuesList[text] != null)
                valuesList.name
            else
                null
        }
    }

    /**
     * Get possible values list name from an assignment such as SETV
     * Uses assignment LValue for the scope
     */
    private fun possibleValuesListNameForAssignment(assignment: CaosScriptCAssignment): CaosValuesList? {
        val commandString = assignment.lvalue?.commandString
                ?: return null
        val variant = assignment.variant
                ?: return null
        val variantCode = variant.code
        return CaosLibs.commands(commandString)
                .firstOrNull {
                    variantCode in it.variants
                }
                ?.returnValuesList
                ?.get(variant)
    }

    /**
     * Gets values list name from
     */
    private fun possibleValuesListNameForParameter(argument: CaosScriptArgument, parentRaw: CaosScriptCommandElement): CaosValuesList? {
        val commandDefinition = parentRaw.commandDefinition
                ?: return null

        // Values list check requires variant
        val variant = myElement.variant
                ?: return null
        // Get values list for argument position
        val argumentNumber = argument.index

        // Get parameter non-null
        val parameter = commandDefinition.parameters.getOrNull(argumentNumber)
        if (parameter == null) {
            LOGGER.severe("Incorrect command returned for ${commandDefinition.command}. Expected a parameter at $argumentNumber, but returned null. BNF grammar may be out of sync")
            return null
        }

        // get values list from parameter
        return parameter.valuesList[variant]
    }

    private fun possibleValuesListNamesForEquality(): CaosValuesList? {
        val expression = myElement
        val equalityExpression = expression.parent as? CaosScriptEqualityExpressionPrime
                ?: return null
        val variant = expression.variant
                ?: return null
        return equalityExpression.getValuesList(variant, expression)
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
        return parentDef.typeName == possibleValuesListName
    }

    override fun multiResolve(p0: Boolean): Array<ResolveResult> {
        val variant = myElement.containingCaosFile?.variant
                ?: return ResolveResult.EMPTY_ARRAY
        val keyAsInt = text.toIntSafe()
        val valuesListName = possibleValuesListName
                ?: return PsiElementResolveResult.EMPTY_ARRAY
        val keys = getValuesListDefinitions(project, valuesListName)
                .filter {
                    it.isVariant(variant)
                }
                .flatMap matchKeys@{ valuesListElement:CaosDefValuesListElement ->
                    if (valuesListElement.isBitflags && keyAsInt != null) {
                        valuesListElement.valuesListValueList.filter { valuesListValueStruct ->
                            valuesListValueStruct.key.toIntSafe()?.let { key -> keyAsInt and key > 0 } ?: false
                        }
                    } else if (valuesListName == "Genus") {
                        val family = myElement.getPreviousNonEmptySibling(false)?.text?.toIntSafe()
                                ?: return@matchKeys emptyList<CaosDefValuesListValue>()
                        valuesListElement.valuesListValueList.filter { it.key == "$family $keyAsInt" }
                    } else {
                        valuesListElement.valuesListValueList.filter { it.key == text }
                    }
                }
        return PsiElementResolveResult.createResults(keys)
    }

    private fun getValuesListDefinition(): CaosValuesList? {
        // If expression is in equality expression
        if (myElement.parent is CaosScriptEqualityExpression) {
            return possibleValuesListNamesForEquality()
        }
        // Get enclosing argument. Element may be nested 2 or 3 levels down
        val argument = myElement.getParentOfType(CaosScriptArgument::class.java)
                ?: return null

        // Get argument parent command element. Required
        val parentRaw = argument.parent as? CaosScriptCommandElement
                ?: return null

        // If parent is assignment, get other side values list
        return if (parentRaw is CaosScriptCAssignment) {
            possibleValuesListNameForAssignment(parentRaw)
        } else {
            possibleValuesListNameForParameter(argument, parentRaw)
        }
    }

    private fun getValuesListDefinitions(project: Project, listName: String): List<CaosDefValuesListElement> {
        /*return CaosDefElementsSearchExecutor.getCaosDefFiles(project)
                .flatMap { file ->
                    PsiTreeUtil.collectElementsOfType(file, CaosDefValuesListElement::class.java)
                            .filter { valuesList ->
                                valuesList.typeName like listName
                            }
                } + */ return CaosDefValuesListElementsByNameIndex.Instance[listName, project]
    }
}