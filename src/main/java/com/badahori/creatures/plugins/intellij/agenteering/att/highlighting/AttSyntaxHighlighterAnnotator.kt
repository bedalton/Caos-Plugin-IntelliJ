package com.badahori.creatures.plugins.intellij.agenteering.att.highlighting

import com.badahori.creatures.plugins.intellij.agenteering.att.highlighting.AttSyntaxHighlighter.Companion.X1
import com.badahori.creatures.plugins.intellij.agenteering.att.highlighting.AttSyntaxHighlighter.Companion.X2
import com.badahori.creatures.plugins.intellij.agenteering.att.highlighting.AttSyntaxHighlighter.Companion.X3
import com.badahori.creatures.plugins.intellij.agenteering.att.highlighting.AttSyntaxHighlighter.Companion.X4
import com.badahori.creatures.plugins.intellij.agenteering.att.highlighting.AttSyntaxHighlighter.Companion.X5
import com.badahori.creatures.plugins.intellij.agenteering.att.highlighting.AttSyntaxHighlighter.Companion.X6
import com.badahori.creatures.plugins.intellij.agenteering.att.highlighting.AttSyntaxHighlighter.Companion.Y1
import com.badahori.creatures.plugins.intellij.agenteering.att.highlighting.AttSyntaxHighlighter.Companion.Y2
import com.badahori.creatures.plugins.intellij.agenteering.att.highlighting.AttSyntaxHighlighter.Companion.Y3
import com.badahori.creatures.plugins.intellij.agenteering.att.highlighting.AttSyntaxHighlighter.Companion.Y4
import com.badahori.creatures.plugins.intellij.agenteering.att.highlighting.AttSyntaxHighlighter.Companion.Y5
import com.badahori.creatures.plugins.intellij.agenteering.att.highlighting.AttSyntaxHighlighter.Companion.Y6
import com.badahori.creatures.plugins.intellij.agenteering.att.psi.api.AttInt
import com.badahori.creatures.plugins.intellij.agenteering.att.psi.api.AttItem
import com.badahori.creatures.plugins.intellij.agenteering.caos.annotators.colorize
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import kotlin.math.floor

class AttSyntaxHighlighterAnnotator : Annotator, DumbAware {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is AttInt -> annotateInt(element, holder)
        }
    }

    private fun annotateInt(element: AttInt, holder: AnnotationHolder) {
        val index = element.parent.parent.children.filterIsInstance<AttItem>().indexOfFirst {
            it.firstChild?.isEquivalentTo(element) == true
        }
        if (index !in 0 until 12) {
            return
        }
        val isX = index % 2 == 0
        val pointIndex = floor(index / 2.0).toInt()
        val color = getColor(pointIndex, isX)
            ?: return
        holder.colorize(element, color)
    }

    private fun getColor(pointIndex: Int, isX: Boolean): TextAttributesKey? {
        return if (isX) {
            when (pointIndex) {
                0 -> X1
                1 -> X2
                2 -> X3
                3 -> X4
                4 -> X5
                5 -> X6
                else -> null
            }
        } else {
            when (pointIndex) {
                0 -> Y1
                1 -> Y2
                2 -> Y3
                3 -> Y4
                4 -> Y5
                5 -> Y6
                else -> null
            }
        }
    }
}