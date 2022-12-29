package com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.impl

import com.badahori.creatures.plugins.intellij.agenteering.catalogue.psi.api.CatalogueCompositeElement
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

open class CatalogueCompositeElementImpl(node:ASTNode) : ASTWrapperPsiElement(node), CatalogueCompositeElement