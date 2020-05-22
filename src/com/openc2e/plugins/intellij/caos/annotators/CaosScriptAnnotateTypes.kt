package com.openc2e.plugins.intellij.caos.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.caos.deducer.CaosVar
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCommandDefElement
import com.openc2e.plugins.intellij.caos.def.psi.api.CaosDefCompositeElement
import com.openc2e.plugins.intellij.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCommandElement
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptIsCommandToken
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptLvalue
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptRvalue

class CaosScriptAnnotateTypes : Annotator {
    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        val annotationWrapper = AnnotationHolderWrapper(annotationHolder)
        when (element) {
            is CaosScriptRvalue -> annotateRValue(element, annotationWrapper)
            is CaosScriptLvalue -> annotateLValue(element, annotationWrapper)
            else -> return
        }

    }

    private fun annotateRValue(element: CaosScriptRvalue, annotationWrapper: AnnotationHolderWrapper) {

    }

    private fun annotateLValue(element: CaosScriptLvalue, annotationWrapper: AnnotationHolderWrapper) {
        val parent = element.getParentOfType(CaosScriptCommandElement::class.java)
        val containingToken = parent
                ?.commandToken
                ?.reference
                ?.multiResolve(true)
                ?.firstOrNull()
                ?.element as? CaosDefCompositeElement
                ?: return
        val index = element.index
        val containingCommand = containingToken.getParentOfType(CaosDefCommandDefElement::class.java)
        val matchingParameter = containingCommand?.parameterStructs?.getOrNull(index)
                ?: return
        element.varToken?.let {
            // todo check var assignment
            return
        }
        var type:String? = element.getChildOfType(CaosScriptIsCommandToken::class.java)?.let {
            val temp = it
                    .reference
                    ?.multiResolve(true)
                    ?.firstOrNull()
                    ?.element as? CaosDefCompositeElement
            temp?.getParentOfType(CaosDefCommandDefElement::class.java)?.returnTypeString
        } ?: element.expectsAgent
        val caosVar = element.toCaosVar()
        val isValid = when (caosVar) {
            is CaosVar.CaosCommandCall ->
            else -> true
        }
        if (isValid)
            return
        val parameterName = matchingParameter.name
        val expectedType = matchingParameter.type.type
        val actualType = caosVar.
        val message = CaosBundle.message("caos.annotator.command-annotator.incorrect-parameter-type-message")
        annotationWrapper.newWarningAnnotation()

                .range(element)
                .create()

    }
}