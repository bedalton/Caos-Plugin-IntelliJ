package com.openc2e.plugins.intellij.caos.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.openc2e.plugins.intellij.caos.def.indices.CaosDefCommandElementsByNameIndex
import com.openc2e.plugins.intellij.caos.lang.CaosBundle
import com.openc2e.plugins.intellij.caos.psi.api.CaosEnumSceneryStatement
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCommandCall
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCommandHead
import com.openc2e.plugins.intellij.caos.psi.api.CaosScriptCommandToken

class CaosScriptCommandAnnotator : Annotator {


    override fun annotate(element: PsiElement, annotationHolder: AnnotationHolder) {
        //
    }

    fun annotateCommand() {
    }
a
    fun annotateInvalidCommandLength(word:CaosScriptCommandToken) {
        val command = (word.parent as? CaosScriptCommandHead)
                ?: return
        val exists = CaosDefCommandElementsByNameIndex.Instance[command.commandTokenList.joinTo(" ") { it.text }]
        val variant =
    }

    fun annotateSceneryEnum(element:CaosEnumSceneryStatement, annotationHolder: AnnotationHolder) {
        if (element.containingCaosFile.variant == "C2")
            return
        annotationHolder.createErrorAnnotation(element.escn, CaosBundle.message("caos.annotator.command-annotator.escn-only-on-c2-error-message"))
    }
}