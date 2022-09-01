@file:Suppress("SpellCheckingInspection")

package com.badahori.creatures.plugins.intellij.agenteering.att.highlighting

import com.badahori.creatures.plugins.intellij.agenteering.utils.rand
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import icons.CaosScriptIcons
import javax.swing.Icon

class AttColorSettingsPage : ColorSettingsPage {

    override fun getIcon(): Icon? {
        return CaosScriptIcons.CAOS_FILE_ICON
    }

    override fun getHighlighter(): SyntaxHighlighter {
        return AttSyntaxHighlighter()
    }

    override fun getDemoText(): String {
        return mDemoText
    }

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> {
        return DESCRIPTORS
    }

    override fun getColorDescriptors(): Array<ColorDescriptor> {
        return ColorDescriptor.EMPTY_ARRAY
    }

    override fun getDisplayName(): String {
        return "Caos Script"
    }

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> {
        return XMLDESCRIPTORS
    }

    companion object {

        private val mDemoText: String by lazy {
            val builder = StringBuilder()
            repeat(10) {
                repeat(6) { point ->
                    val xInt = rand(0, 36)
                    val x: Any = if (xInt < 10) {
                        "0$xInt"
                    } else {
                        xInt
                    }

                    val yInt = rand(0,28)
                    val y: Any = if (xInt < 10) {
                        "0$yInt"
                    } else {
                        yInt
                    }

                    builder
                        .append("<x").append(point).append('>')
                        .append(x)
                        .append("</x").append(point).append('>')
                        .append("<y").append(point).append('>')
                        .append(y)
                        .append("</y").append(point).append('>')
                }
                builder.appendLine()
            }
            builder.appendLine()
            builder.toString()
        }
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Point 1: X", AttSyntaxHighlighter.X1),
            AttributesDescriptor("Point 1: Y", AttSyntaxHighlighter.Y1),
            AttributesDescriptor("Point 2: X", AttSyntaxHighlighter.X2),
            AttributesDescriptor("Point 2: Y", AttSyntaxHighlighter.Y2),
            AttributesDescriptor("Point 3: X", AttSyntaxHighlighter.X3),
            AttributesDescriptor("Point 3: Y", AttSyntaxHighlighter.Y3),
            AttributesDescriptor("Point 4: X", AttSyntaxHighlighter.X4),
            AttributesDescriptor("Point 4: Y", AttSyntaxHighlighter.Y4),
            AttributesDescriptor("Point 5: X", AttSyntaxHighlighter.X5),
            AttributesDescriptor("Point 5: Y", AttSyntaxHighlighter.Y5),
            AttributesDescriptor("Point 6: X", AttSyntaxHighlighter.X6),
            AttributesDescriptor("Point 6: Y", AttSyntaxHighlighter.Y6),
        )

        private val XMLDESCRIPTORS: HashMap<String, TextAttributesKey> = hashMapOf(
            "x1" to AttSyntaxHighlighter.X1,
            "y1" to AttSyntaxHighlighter.Y1,
            "x2" to AttSyntaxHighlighter.X2,
            "y2" to AttSyntaxHighlighter.Y2,
            "x3" to AttSyntaxHighlighter.X3,
            "y3" to AttSyntaxHighlighter.Y3,
            "x4" to AttSyntaxHighlighter.X4,
            "y4" to AttSyntaxHighlighter.Y4,
            "x5" to AttSyntaxHighlighter.X5,
            "y5" to AttSyntaxHighlighter.Y5,
            "x6" to AttSyntaxHighlighter.X6,
            "y6" to AttSyntaxHighlighter.Y6,
        )
    }
}
