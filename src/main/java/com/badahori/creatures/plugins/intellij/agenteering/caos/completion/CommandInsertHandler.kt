package com.badahori.creatures.plugins.intellij.agenteering.caos.completion

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosParameter
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.EmptyNode
import com.intellij.codeInsight.template.impl.TemplateImpl
import com.intellij.codeInsight.template.impl.VariableNode

class CommandInsertHandler(private val command:String, private val parameters:List<CaosParameter>, private val withSpace:Boolean) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, p1: LookupElement) {
        //val tail = if (withSpace) " " else "" // Never needs tail as parameters are included
        val templateText = " " + parameters.joinToString(" ") { "\$${it.name}\$"}// + tail
        val template = TemplateImpl("", templateText, "")
        for(i in 0 ..parameters.lastIndex) {
            val parameter = parameters[i]
            template.addVariable(parameter.name, VariableNode(parameter.name, EmptyNode()), false)
        }
        val editor = context.editor
        TemplateManager.getInstance(context.project).startTemplate(editor, template)
    }
}