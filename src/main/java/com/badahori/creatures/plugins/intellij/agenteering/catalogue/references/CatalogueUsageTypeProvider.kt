package com.badahori.creatures.plugins.intellij.agenteering.catalogue.references

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueArray
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueCompositeElement
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueItemName
import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueTag
import com.intellij.psi.PsiElement
import com.intellij.usages.impl.rules.UsageType
import com.intellij.usages.impl.rules.UsageTypeProvider

class CatalogueUsageTypeProvider: UsageTypeProvider {
    override fun getUsageType(element: PsiElement): UsageType? {
        if (element !is CatalogueCompositeElement) {
            return null
        }
        if (!hasScope(element)) {
            return null
        }

        val parentElement = if (element is CatalogueItemName) {
            element.parent
        } else {
            element
        }

        return when (parentElement) {
            is CatalogueTag -> TAG_USAGE
            is CatalogueArray -> ARRAY_USAGE
            else -> null
        }
    }


    private fun hasScope(element: CatalogueCompositeElement): Boolean {
        return when (element) {
            is CatalogueItemName -> true
            is CatalogueTag -> true
            is CatalogueArray -> true
            else -> false
        }
    }
}

val ARRAY_USAGE by lazy {
    UsageType {
        CaosBundle.message("catalogue.usage-types.array")
    }
}

val TAG_USAGE by lazy {
    UsageType {
        CaosBundle.message("catalogue.usage-types.tag")
    }
}