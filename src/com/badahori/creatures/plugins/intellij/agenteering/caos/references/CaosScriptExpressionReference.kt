package com.badahori.creatures.plugins.intellij.agenteering.caos.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.def.indices.CaosDefTypeDefinitionElementsByNameIndex
import com.badahori.creatures.plugins.intellij.agenteering.caos.def.psi.api.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.orDefault
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptArgument
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptCommandElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptExpression
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptNumber
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.impl.containingCaosFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.getSelfOrParentOfType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

class CaosScriptExpressionReference(element:CaosScriptExpression) : PsiPolyVariantReferenceBase<CaosScriptExpression>(element, TextRange.create(0, element.textLength)) {

    private val text by lazy { myElement.text }

    private val possibleTypeDefNames:List<String> by lazy lazy@{
        val argument = myElement.getParentOfType(CaosScriptArgument::class.java)
                ?: return@lazy emptyList<String>()
        val parentRaw = argument.parent as? CaosScriptCommandElement
                ?: return@lazy emptyList<String>()
        val commandDef = parentRaw.commandToken
                ?.reference
                ?.multiResolve(true)
                ?.mapNotNull { it.element?.getSelfOrParentOfType(CaosDefCommandDefElement::class.java) }
                ?.ifEmpty { null }
                ?: return@lazy emptyList<String>()
        val argumentNumber = argument.index
        val project = myElement.project
        val variant = myElement.containingCaosFile?.variant.orDefault()
        commandDef.mapNotNull map@{ def ->
            val parameter = def.parameterStructs.getOrNull(argumentNumber)
                    ?: return@map null
            parameter.type.typedef
        }
    }

    private val project:Project by lazy { myElement.project }

    override fun isReferenceTo(anElement: PsiElement): Boolean {
        if (anElement !is CaosDefTypeDefinitionKey) {
            return false
        }
        if (anElement.text != text)
            return false
        val parentDef = anElement.getParentOfType(CaosDefTypeDefinitionElement::class.java)
                ?: return false
        return parentDef.typeName in possibleTypeDefNames
    }

    override fun multiResolve(p0: Boolean): Array<ResolveResult> {
        val variant = myElement.containingCaosFile?.variant.orDefault()
        val keys = possibleTypeDefNames.flatMap map@{ typeDefName ->
            CaosDefTypeDefinitionElementsByNameIndex.Instance[typeDefName, project]
                    .filter {
                        it.isVariant(variant)
                    }
                    .mapNotNull {
                        it.typeDefinitionList.firstOrNull { it.key == text }
                    }
        }.ifEmpty { null }
                ?: return PsiElementResolveResult.EMPTY_ARRAY
        return PsiElementResolveResult.createResults(keys)
    }
}