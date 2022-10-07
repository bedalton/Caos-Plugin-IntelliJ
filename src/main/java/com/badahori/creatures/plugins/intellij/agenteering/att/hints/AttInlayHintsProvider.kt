@file:Suppress("UnstableApiUsage")

package com.badahori.creatures.plugins.intellij.agenteering.att.hints

import bedalton.creatures.util.toListOf
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.AttEditorModel
import com.badahori.creatures.plugins.intellij.agenteering.att.editor.pose.PoseEditorSupport
import com.badahori.creatures.plugins.intellij.agenteering.att.lang.AttLanguage
import com.badahori.creatures.plugins.intellij.agenteering.att.psi.api.AttInt
import com.badahori.creatures.plugins.intellij.agenteering.att.psi.api.AttItem
import com.badahori.creatures.plugins.intellij.agenteering.att.psi.api.AttLine
import com.badahori.creatures.plugins.intellij.agenteering.caos.hints.EMPTY_INLAY_LIST
import com.badahori.creatures.plugins.intellij.agenteering.common.InlayHintGenerator
import com.badahori.creatures.plugins.intellij.agenteering.common.AbstractInlayHintsProvider
import com.badahori.creatures.plugins.intellij.agenteering.indices.BreedPartKey
import com.badahori.creatures.plugins.intellij.agenteering.utils.className
import com.badahori.creatures.plugins.intellij.agenteering.utils.startOffset
import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.Option
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import kotlin.math.floor


internal class AttInlayHintsProvider : AbstractInlayHintsProvider() {
    override val inlayHintGenerators: List<InlayHintGenerator> by lazy {
        AttInlayHints.values().toList()
    }

}

enum class AttInlayHints(description: String, defaultEnabled: Boolean, override val priority: Int = 0) : InlayHintGenerator, DumbAware {

    ATT_POSITION_INDICES("Identifies ATT x/y point sets by point index", true, 10) {
        override fun isApplicable(element: PsiElement): Boolean {
            return isAttX(element)
        }

        override fun provideHints(element: PsiElement): List<InlayInfo> {
            val index = getPointIndex(element)
                ?: EMPTY_INLAY_LIST
            return InlayInfo("$index", element.startOffset)
                .toListOf()
        }

        override fun getHintInfo(element: PsiElement): HintInfo? {
            val index = getPointIndex(element)
                ?: return null
            val text = "point:$index"
            return HintInfo.MethodInfo(this.className, text.toListOf(), AttLanguage)
        }

    },
    ATT_POSITION_NAMES("ATT point x/y part names", false, 20) {
        override fun isApplicable(element: PsiElement): Boolean {

            if (!element.isValid) {
                return false
            }

            if (!isAttX(element)) {
                return false
            }

            // Cannot get part name if file has no real part char
            val part = BreedPartKey.fromFileName(element.containingFile.name)
                ?.part
                ?.lowercaseChar()
                ?: return false
            return part in 'a'..'q'
        }

        /**
         * Actually provide hint
         */
        override fun provideHints(element: PsiElement): List<InlayInfo> {
            val index = getPointIndex(element)
                ?: return EMPTY_INLAY_LIST
            val partName = getPointName(element, index)
                ?: return EMPTY_INLAY_LIST
            return InlayInfo(partName, element.startOffset)
                .toListOf()
        }

        /**
         * Get the hint info used to blacklist values
         */
        override fun getHintInfo(element: PsiElement): HintInfo? {
            val index = getPointIndex(element)
                ?: return null
            val pointName = getPointName(element, index)
                ?: return null
            val part = getPartName(element)
                ?: return null
            return HintInfo.MethodInfo(this.className, listOf(part, pointName), AttLanguage)
        }


        /**
         * Gets the name of the part associated with this point
         */
        private fun getPointName(element: PsiElement, index: Int): String? {
            if (!element.isValid) {
                return null
            }
            val file = element.containingFile
                ?: return null

            if (!file.isValid) {
                return null
            }
            val part = BreedPartKey.fromFileName(file.name)?.part?.lowercaseChar()
                ?: return null
            if (part !in 'a'..'q') {
                return null
            }
            return AttEditorModel.pointNames(part).getOrNull(index)
        }

        /**
         * Gets the name of this files part
         */
        private fun getPartName(element: PsiElement): String? {
            if (!element.isValid) {
                return null
            }
            val file = element.containingFile
                ?: return null

            if (!file.isValid) {
                return null
            }
            val part = BreedPartKey.fromFileName(file.name)?.part
                ?: return null
            return PoseEditorSupport.getPartName(part)
        }

    };


    override val enabled: Boolean
        get() = option.isEnabled()

    override val option: Option = Option("SHOW_ATT_${this.name}", description, defaultEnabled)


}

private fun getPointIndex(element: PsiElement?): Int? {

    if (element?.isValid != true) {
        return null
    }

    // Ensure element is an int (should have been done in the check
    val int = element as? AttInt
        ?: return null

    // Int parent is AttItem
    val item = element.parent as? AttItem
        ?: return null

    // Item parent as AttLine
    val line = item.parent as? AttLine
        ?: return null

    // Get index of Line child -> Item (which contains our int)
    val slot = line.children.indexOf(item)
    if (slot < 0) {
        return null
    }
    return floor(slot / 2.0).toInt()
}

private fun isAttX(element: PsiElement): Boolean {
    if (!element.isValid) {
        return false
    }

    if (element !is AttInt) {
        return false
    }

    // Trying to get int position in line
    // it goes Int ^ Item ^ Line

    // Int parent is AttItem
    val item = element.parent as? AttItem
        ?: return false

    // Item parent as AttLine
    val line = item.parent as? AttLine
        ?: return false

    // Get index of Line child -> Item (which contains our int)
    val slot = line.children.indexOf(item)

    // Check that int represents an X (start of a point for label)
    return slot >= 0 && slot % 2 == 0
}