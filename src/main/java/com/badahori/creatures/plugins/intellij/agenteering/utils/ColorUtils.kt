@file:Suppress("UseJBColor")

package com.badahori.creatures.plugins.intellij.agenteering.utils

import com.intellij.ui.JBColor
import java.awt.Color

/**
 * returns a brightened version of the color using a value of 0-100
 */
fun Color.brighten(factorIn:Int): Color? {
    val factor = (100-factorIn) / 100.0
    var r: Int = red
    var g: Int = green
    var b: Int = blue
    val alpha: Int = alpha

    /* From 2D group:
         * 1. black.brighter() should return grey
         * 2. applying brighter to blue will always return blue, brighter
         * 3. non pure color (non zero rgb) will eventually return white
         */
    val i = (1.0 / (1.0 - factor)).toInt()
    if (r == 0 && g == 0 && b == 0) {
        return Color(i, i, i, alpha)
    }
    if (r in 1 until i) r = i
    if (g in 1 until i) g = i
    if (b in 1 until i) b = i
    return Color(Math.min((r / factor).toInt(), 255),
            Math.min((g / factor).toInt(), 255),
            Math.min((b / factor).toInt(), 255),
            alpha)
}

/**
 * Creates a new `Color` that is a darker version of this
 * `Color`.
 *
 *
 * This method applies an arbitrary scale factor to each of the three RGB
 * components of this `Color` to create a darker version of
 * this `Color`.
 * The `alpha` value is preserved.
 * Although `brighter` and
 * `darker` are inverse operations, the results of a series
 * of invocations of these two methods might be inconsistent because
 * of rounding errors.
 * @return  a new `Color` object that is
 * a darker version of this `Color`
 * with the same `alpha` value.
 * @see java.awt.Color.brighter
 *
 * @since      JDK1.0
 */
fun Color.darken(factorIn:Int): Color? {
    val factor = factorIn / 100.0
    return Color(Math.max((red * factor).toInt(), 0),
            Math.max((green * factor).toInt(), 0),
            Math.max((blue * factor).toInt(), 0),
            alpha)
}

val PANEL_TRANSPARENT_BLACK = JBColor {
    val color = JBColor.PanelBackground
    Color(color.red, color.green, color.blue, 0)
}