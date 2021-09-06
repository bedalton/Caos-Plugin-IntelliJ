@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.utils

import java.awt.BorderLayout
import java.awt.Color
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JLabel
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.Document
import javax.swing.text.JTextComponent


/**
 * The TextPrompt class will display a prompt over top of a text component when
 * the Document of the text field is empty. The Show property is used to
 * determine the visibility of the prompt.
 *
 * The Font and foreground Color of the prompt will default to those properties
 * of the parent text component. You are free to change the properties after
 * class construction.
 */
class TextPrompt @JvmOverloads constructor(
    text: String,
    private val component: JTextComponent,
    show: Show = Show.ALWAYS
) :
    JLabel(), FocusListener, DocumentListener {
    enum class Show {
        ALWAYS, FOCUS_GAINED, FOCUS_LOST
    }

    private val document: Document = component.document

    /**
     * Set the prompt Show property to control when the prompt is shown.
     * Valid values are:
     *
     * Show.ALWAYS (default) - always show the prompt
     * Show.Focus_GAINED - show the prompt when the component gains focus
     * (and hide the prompt when focus is lost)
     * Show.Focus_LOST - show the prompt when the component loses focus
     * (and hide the prompt when focus is gained)
     */
    var show: Show? = show

    /**
     * Show the prompt once. Once the component has gained/lost focus
     * once, the prompt will not be shown again.
     *
     * when true the prompt will only be shown once,
     * otherwise it will be shown repeatedly.
     */
    private var showPromptOnce = false
    private var focusLost = 0

    /**
     * Convenience method to change the alpha value of the current foreground
     * Color to the specific value.
     *
     * @param alpha value in the range of 0 - 1.0.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun changeAlpha(alpha: Float) {
        changeAlpha((alpha * 255).toInt())
    }

    /**
     * Convenience method to change the alpha value of the current foreground
     * Color to the specific value.
     *
     * @param alpha value in the range of 0 - 255.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun changeAlpha(alpha: Int) {
        val correctedAlpha = if (alpha > 255) 255 else if (alpha < 0) 0 else alpha
        val foreground = foreground
        val red = foreground.red
        val green = foreground.green
        val blue = foreground.blue
        val withAlpha = Color(red, green, blue, correctedAlpha)
        super.setForeground(withAlpha)
    }


    /**
     * Convenience method to change the style of the current Font. The style
     * values are found in the Font class. Common values might be:
     * Font.BOLD, Font.ITALIC and Font.BOLD + Font.ITALIC.
     *
     * @param style value representing the new style of the Font.
     */
    fun changeStyle(style: Int) {
        font = font.deriveFont(style)
    }

    /**
     * Check whether the prompt should be visible or not. The visibility
     * will change on updates to the Document and on focus changes.
     */
    private fun checkForPrompt() {
        //  Text has been entered, remove the prompt
        if (document.length > 0) {
            isVisible = false
            return
        }

        //  Prompt has already been shown once, remove it
        if (showPromptOnce && focusLost > 0) {
            isVisible = false
            return
        }

        //  Check the Show property and component focus to determine if the
        //  prompt should be displayed.
        isVisible = if (component.hasFocus()) {
            (show == Show.ALWAYS || show == Show.FOCUS_GAINED)
        } else {
            (show == Show.ALWAYS || show == Show.FOCUS_LOST)
        }
    }

    //  Implement FocusListener
    override fun focusGained(e: FocusEvent) {
        checkForPrompt()
    }

    override fun focusLost(e: FocusEvent) {
        focusLost++
        checkForPrompt()
    }

    //  Implement DocumentListener
    override fun insertUpdate(e: DocumentEvent) {
        checkForPrompt()
    }

    override fun removeUpdate(e: DocumentEvent) {
        checkForPrompt()
    }

    override fun changedUpdate(e: DocumentEvent) {}

    init {
        setText(text)
        font = component.font
        foreground = component.foreground
        border = EmptyBorder(component.insets)
        horizontalAlignment = LEADING
        component.addFocusListener(this)
        document.addDocumentListener(this)
        component.layout = BorderLayout()
        component.add(this)
        checkForPrompt()
    }
}
