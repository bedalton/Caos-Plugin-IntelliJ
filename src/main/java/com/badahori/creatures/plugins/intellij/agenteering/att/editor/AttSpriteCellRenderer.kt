@file:Suppress("UseJBColor")

package com.badahori.creatures.plugins.intellij.agenteering.att.editor

import com.badahori.creatures.plugins.intellij.agenteering.utils.ActionHelper
import com.badahori.creatures.plugins.intellij.agenteering.utils.height
import com.badahori.creatures.plugins.intellij.agenteering.utils.scaleNearestNeighbor
import com.badahori.creatures.plugins.intellij.agenteering.utils.width
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.JBColor
import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.KeyStroke.getKeyStroke
import kotlin.math.floor


@Suppress("UseJBColor")
private val colors = listOf(
    Color.YELLOW,
    Color.GREEN,
    Color.CYAN,
    Color.MAGENTA,
    Color(47, 170, 240),
    Color.PINK
)


data class AttSpriteCellData(
    val index: Int,
    val image: BufferedImage,
    val points: List<Pair<Int, Int>>,
    val pointNames: List<String>,
    val folded: List<Int>,
    private val changePointListener: OnChangePoint,
    private val changeCellListener: HasSelectedCell,
) {
    val isFolded: Boolean get() = index in folded

    val isFocused: Boolean get() = changeCellListener.selectedCell == index

    fun onPlace(point: Pair<Int, Int>) {
        if (changeCellListener.selectedCell == index) {
            changePointListener.onChangePoint(index, point)
        } else {
            onFocus()
        }
    }

    fun onFocus() {
        if (!isFocused) {
            changeCellListener.setSelected(index, true)
        }
    }

    fun focusPreviousCell() {
        if (index > 0) {
            changeCellListener.setSelected(index - 1, true)
        }
    }

    fun focusNextCell() {
        if (index < 16) {
            changeCellListener.setSelected(index + 1, true)
        }
    }

    fun shiftPoint(xDelta: Int, yDelta: Int) {
        changePointListener.onShiftPoint(index, Pair(xDelta, yDelta))
    }

}


internal class AttSpriteCellComponent : JPanel() {

    private var data: AttSpriteCellData? = null
    private var indexLabel: JLabel = JLabel("")
        .apply { inheritsPopupMenu = true }
    private var canvas: AttCellCanvas? = null
    private var foldedLabel: JLabel? = null
    private val isFolded: Boolean get() = data?.isFolded == true

    init {
        this.layout = BoxLayout(this, BoxLayout.X_AXIS)
        this.border = BorderFactory.createEmptyBorder(5, 16, 10, 5)
        inheritsPopupMenu = true
        this.isFocusable = true
        this.add(indexLabel)
        bindKeyboardShortcuts()
    }

    internal fun update(labels: Boolean, scale: Double, value: AttSpriteCellData, selected: Boolean) {
        // Set index label to this index
        indexLabel.text = "${value.index + 1}."

        // Set the current data for use by handlers
        this.data = value
        this.removeAll()
        this.add(indexLabel)

        // Scale image if needed
        val imageValue = value.image
        val folded = isFolded
        var width = if (folded) -1 else (imageValue.width * scale).toInt()
        var height = if (folded) -1 else (imageValue.height * scale).toInt()
        val image: Image = if (width > 0 && height > 0) {
            imageValue.scaleNearestNeighbor(scale)
        } else {
            imageValue
        }


        // If folded, show folded label only
        if (folded) {
            var foldLabel: JLabel? = this.foldedLabel
            if (foldLabel == null) {
                foldLabel = JLabel()
                this.foldedLabel = foldLabel
                add(foldLabel)
            }
            foldLabel.foreground = REPLICATED_MESSAGE_FONT_COLOR
            foldLabel.text = "  Duplicate image. Points are being replicated from cell above"
            foldLabel.isVisible = true
            width = maxOf(foldLabel.width, 10)
            height = maxOf(foldLabel.height, 160)
            this.add(foldLabel)
            foldLabel.alignmentX = Component.LEFT_ALIGNMENT
            this.add(Box.createHorizontalGlue())
            foldLabel.minimumSize = Dimension(100, 20)
        } else {
            // If not folded, update canvas
            val canvas = getCanvas(image)
            canvas.selected = selected
            canvas.image = image
            canvas.labels = labels
            canvas.pointNames = value.pointNames
            canvas.scaledPoints = scalePoints(value.points, scale)
            canvas.scale = scale
            canvas.pointNames = value.pointNames
            canvas.isVisible = true
            canvas.revalidate()
            this.add(canvas)
            canvas.alignmentX = Component.LEFT_ALIGNMENT
        }

        // Set sizing information
        val padWidth = if (folded) 0 else 10
        val padHeight = if (folded) 0 else 10
        val dimension = if (width > 0 && height > 0) {
            Dimension(width + padWidth, height + padHeight)
        } else {
            Dimension(1, 1)
        }
        this.size = dimension
        this.preferredSize = dimension
        this.minimumSize = dimension
//        this.canvas?.preferredSize = Dimension(width, height)
        this.minimumSize = dimension

        // Redraw
        revalidate()
        repaint()
        if (selected) {
            requestFocusInWindow()
        }
    }


    private fun getCanvas(image: Image): AttCellCanvas {
        canvas?.let {
            return it
        }
        val canvas = AttCellCanvas(
            false,
            image,
            emptyList(),
            false,
            emptyList(),
            1.0
        )
        add(canvas)
        canvas.alignmentX = Component.LEFT_ALIGNMENT
        initMouseListeners(canvas)
        this.canvas = canvas
        canvas.isFocusable = true
        canvas.inheritsPopupMenu = true
        this.canvas = canvas
        return canvas
    }

    private fun initMouseListeners(canvas: AttCellCanvas) {
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (isFolded) {
                    return
                }
                super.mouseClicked(e)
                if (e.button == MouseEvent.BUTTON1) {
                    data?.onFocus()
                }
            }
        })

        canvas.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (isFolded) {
                    return
                }
                super.mouseClicked(e)
                val data = data
                    ?: return
                if (e.source !== canvas) {
                    return
                }
                val point = e.point
                if (!canvas.contains(point)) {
                    return
                }
                if (e.button == MouseEvent.BUTTON1) {
                    val x = (e.x / canvas.scale).toInt()
                    val y = (e.y / canvas.scale).toInt()
                    val image = data.image
                    if (image.width < x || image.height < y) {
                        data.onFocus()
                        return
                    }
                    if (x < 0 || y < 0)
                        return
                    data.onPlace(Pair(x, y))
                }
            }
        })
    }

    private fun bindKeyboardShortcuts() {

        bindKeyStroke(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "move.up", getKeyStroke(KeyEvent.VK_UP, 0)) {
            data?.shiftPoint(0, -1)
        }
        bindKeyStroke(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "move.up", getKeyStroke(KeyEvent.VK_KP_UP, 0)) {
            data?.shiftPoint(0, -1)
        }

        bindKeyStroke(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "move.up", getKeyStroke(KeyEvent.VK_KP_UP, 0)) {
            data?.shiftPoint(0, -1)
        }

        bindKeyStroke(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "move.down", getKeyStroke(KeyEvent.VK_DOWN, 0)) {
            data?.shiftPoint(0, 1)
        }
        bindKeyStroke(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "move.down", getKeyStroke(KeyEvent.VK_KP_DOWN, 0)) {
            data?.shiftPoint(0, 1)
        }
        bindKeyStroke(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "move.right", getKeyStroke(KeyEvent.VK_RIGHT, 0)) {
            data?.shiftPoint(1, 0)
        }
        bindKeyStroke(
            WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
            "move.right",
            getKeyStroke(KeyEvent.VK_KP_RIGHT, 0)
        ) {
            data?.shiftPoint(1, 0)
        }
        bindKeyStroke(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "move.left", getKeyStroke(KeyEvent.VK_LEFT, 0)) {
            data?.shiftPoint(-1, 0)
        }
        bindKeyStroke(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "move.left", getKeyStroke(KeyEvent.VK_KP_LEFT, 0)) {
            data?.shiftPoint(-1, 0)
        }


        // Shift Focus up or down
        bindKeyStroke(
            WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
            "focus.previous",
            getKeyStroke(KeyEvent.VK_UP, InputEvent.SHIFT_DOWN_MASK)
        ) {
            data?.focusPreviousCell()
        }
        // Shift Focus up or down
        bindKeyStroke(
            WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
            "focus.previous",
            getKeyStroke(KeyEvent.VK_KP_UP, InputEvent.SHIFT_DOWN_MASK)
        ) {
            data?.focusPreviousCell()
        }

        bindKeyStroke(
            WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
            "focus.next",
            getKeyStroke(KeyEvent.VK_DOWN, InputEvent.SHIFT_DOWN_MASK)
        ) {
            data?.focusNextCell()
        }
        bindKeyStroke(
            WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
            "focus.next",
            getKeyStroke(KeyEvent.VK_KP_DOWN, InputEvent.SHIFT_DOWN_MASK)
        ) {
            data?.focusNextCell()
        }
    }

    @Suppress("SameParameterValue")
    private fun bindKeyStroke(condition: Int, name: String?, keyStroke: KeyStroke?, action: () -> Unit) {
        val im = getInputMap(condition)
        val am = actionMap
        im.put(keyStroke, name)
        am.put(name, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                action()
            }
        })
    }

    companion object {

        private val REPLICATED_MESSAGE_FONT_COLOR = JBColor(
            Color(30, 30, 30, 120),
            Color(220, 220, 220, 127)
        )

        fun scalePoints(points: List<Pair<Int, Int>>, scale: Double): List<Pair<Int, Int>> {
            return points.subList(0, minOf(6, points.size)).map {
                Pair(floor(it.first * scale).toInt(), floor(it.second * scale).toInt())
            }
        }
    }
}

/**
 * Cell renderer for individual sprite image in Sprite file list
 */
internal class AttSpriteCellList(
    private var listItems: List<AttSpriteCellData>,
    private var scale: Double = 1.0,
    private var maxWidth: Int = 300,
    private var maxHeight: Int = 300,
    var labels: Boolean = true,
) : JPanel() {

    private val pool: MutableList<AttSpriteCellComponent> = mutableListOf()

    private var color: Color = EditorColorsManager.getInstance().globalScheme.defaultBackground

    init {
        if (color.alpha > 0) {
            this.background = color
            this.isOpaque = true
        }
        this.isFocusable = true
        initKeyListeners()
    }

    fun get(index: Int): AttSpriteCellComponent {
        while (index >= pool.size) {
            val component = AttSpriteCellComponent()
            pool.add(component)
            add(component)
        }
        return pool[index]
    }

    fun setScale(scale: Double) {
        this.scale = scale
        reload()
    }

    fun setMaxWidthHeight(width: Int, height: Int) {
        this.maxWidth = width
        this.maxHeight = height
        reload()
    }

    fun setItems(newItems: List<AttSpriteCellData>) {
        this.listItems = newItems
        val size = newItems.size
        if (pool.size > size) {
            pool.slice(size..pool.size).forEach { component ->
                component.isVisible = false
            }
        }
        reload()
    }

    fun focusCell(index: Int) {
        if (this.listItems.size > index) {
            invokeLater {
                val item = get(index)
                item.requestFocus()
                item.requestFocusInWindow()
            }
        }
    }

    private fun focusCell() {
        invokeLater {
            val focused = getCellForSelectedItem()
                ?: return@invokeLater

            focused.requestFocus()
            focused.requestFocusInWindow()
        }
    }

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        reload()
    }

    private fun nextCell() {
        getSelectedItem()?.focusNextCell()
    }

    private fun previousCell() {
        getSelectedItem()?.focusPreviousCell()
    }

    private fun initKeyListeners() {
        initBasicKeyListeners(WHEN_IN_FOCUSED_WINDOW, this)
    }

    fun reload() {
        var focused = -1
        listItems.forEachIndexed { i, item ->
            setCell(item, i)
            if (item.isFocused) {
                focused = i
            }
        }
        revalidate()
        repaint()

        invokeLater {
            if (focused >= 0) {
                focusCell(focused)
            }
        }
    }

    private fun getSelectedItem(): AttSpriteCellData? {
        return listItems.firstOrNull { it.isFocused }
    }

    private fun getCellForSelectedItem(): AttSpriteCellComponent? {
        val selectedItem = listItems.firstOrNull { it.isFocused }
            ?: return null
        val cell = get(selectedItem.index)
        return if (cell.isVisible) {
            cell
        } else {
            null
        }
    }

    private fun setCell(value: AttSpriteCellData, index: Int) {
        val panel = get(index)
        panel.update(labels, scale, value, value.isFocused)
    }

    @Suppress("UNUSED_PARAMETER")
    fun scrollTo(pose: Int) {
//        TODO: Implement properly without it being frustrating to the end user
//        val item = get(pose)
//        invokeLater later@{
//            (parent as? JPanel)?.doLayout()
//            val bounds = item.bounds
//            val visibleRect = visibleRect
//            if (visibleRect.contains(bounds).orTrue())
//                return@later
//            val offset = visibleRect.intersection(bounds)
////            (parent?.parent as? JScrollPane)?.verticalScrollBar?.value = offset
//
//        }
    }

    private fun shiftPoint(xDelta: Int, yDelta: Int) {
        getSelectedItem()?.shiftPoint(xDelta, yDelta)
    }

    fun initBasicKeyListeners(condition: Int, component: JComponent) {
        val actionMap = component.actionMap
        val inputMap = component.getInputMap(condition)

        ActionHelper.addKeyAction(
            actionMap,
            inputMap,
            "Select previous cell",
            getKeyStroke(KeyEvent.VK_UP, InputEvent.SHIFT_DOWN_MASK),
            this::previousCell
        )
        ActionHelper.addKeyAction(
            actionMap,
            inputMap,
            "Select previous cell (Keypad)",
            getKeyStroke(KeyEvent.VK_KP_UP, InputEvent.SHIFT_DOWN_MASK),
            this::previousCell
        )

        ActionHelper.addKeyAction(
            actionMap,
            inputMap,
            "Select next cell",
            getKeyStroke(KeyEvent.VK_DOWN, InputEvent.SHIFT_DOWN_MASK),
            this::nextCell
        )
        ActionHelper.addKeyAction(
            actionMap,
            inputMap,
            "Select next cell (KP)",
            getKeyStroke(KeyEvent.VK_KP_DOWN, InputEvent.SHIFT_DOWN_MASK),
            this::nextCell
        )

        ActionHelper.addKeyAction(
            actionMap,
            inputMap,
            "Focus cell",
            getKeyStroke(KeyEvent.VK_SPACE, 0),
            this::focusCell
        )

        ActionHelper.addKeyAction(
            actionMap, inputMap,
            "move.up",
            getKeyStroke(KeyEvent.VK_UP, 0)
        ) {
            shiftPoint(0, -1)
        }

        ActionHelper.addKeyAction(
            actionMap, inputMap,
            "move.up",
            getKeyStroke(KeyEvent.VK_KP_UP, 0)
        ) {
            shiftPoint(0, -1)
        }

        ActionHelper.addKeyAction(
            actionMap, inputMap,
            "move.up",
            getKeyStroke(KeyEvent.VK_KP_UP, 0)
        ) {
            shiftPoint(0, -1)
        }

        ActionHelper.addKeyAction(
            actionMap, inputMap,
            "move.down",
            getKeyStroke(KeyEvent.VK_DOWN, 0)
        ) {
            shiftPoint(0, 1)
        }
        ActionHelper.addKeyAction(
            actionMap, inputMap,
            "move.down",
            getKeyStroke(KeyEvent.VK_KP_DOWN, 0)
        ) {
            shiftPoint(0, 1)
        }
        ActionHelper.addKeyAction(
            actionMap, inputMap,
            "move.right",
            getKeyStroke(KeyEvent.VK_RIGHT, 0)
        ) {
            shiftPoint(1, 0)
        }

        ActionHelper.addKeyAction(
            actionMap, inputMap,
            "move.right",
            getKeyStroke(KeyEvent.VK_KP_RIGHT, 0)
        ) {
            shiftPoint(1, 0)
        }

        ActionHelper.addKeyAction(
            actionMap, inputMap,
            "move.left",
            getKeyStroke(KeyEvent.VK_LEFT, 0)
        ) {
            shiftPoint(-1, 0)
        }
        ActionHelper.addKeyAction(
            actionMap, inputMap,
            "move.left",
            getKeyStroke(KeyEvent.VK_KP_LEFT, 0)
        ) {
            shiftPoint(-1, 0)
        }
        component.actionMap = actionMap
        component.setInputMap(condition, inputMap)
    }

}

interface OnChangePoint {
    fun onChangePoint(lineNumber: Int, newPoint: Pair<Int, Int>)
    fun onShiftPoint(lineNumber: Int, offset: Pair<Int, Int>)
}

interface HasSelectedCell {
    val selectedCell: Int

    /**
     * Attempts to set selected to index, but returns actually set index
     * If point replication is set, index may be different if skipping duplicates
     * @return actually selected index
     */
    fun setSelected(index: Int, sender: Any?): Int
}


/**
 * Draw a String centered in the middle of a Rectangle.
 * @param text The String to draw.
 * @param rect The Rectangle to center the text in.
 * @param font A font to use when drawing the centered text
 */
fun Graphics.drawCenteredString(text: String, rect: Rectangle, font: Font? = null) {
    if (font != null) {
        // Set the font
        this.font = font
    }
    // Get the FontMetrics
    val metrics = getFontMetrics(this.font)
    // Determine the X coordinate for the text
    val x = rect.x + (rect.width - metrics.stringWidth(text)) / 2
    // Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top of the screen)
    val y = rect.y + (rect.height - metrics.height) / 2 + metrics.ascent

    // Draw the String
    drawString(text, x, y)
}


private class AttCellCanvas(
    var selected: Boolean,
    var image: Image,
    var scaledPoints: List<Pair<Int, Int>>,
    var labels: Boolean,
    var pointNames: List<String>,
    var scale: Double,
) : JPanel() {

    var scaledFont: Font? = null


    init {
        val imageDimension = Dimension(image.width + 60, image.height + 60)
        size = imageDimension
        preferredSize = imageDimension
        minimumSize = imageDimension
        background = Color.BLACK
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        g.drawImage(image, 0, 0, this)
        if (!selected) {
            g.color = Color(0, 0, 0, 60)
            g.fillRect(0, 0, width, height)
            g.color = Color(255, 255, 255, 255)
            g.drawCenteredString("Click to Edit", Rectangle(0, 0, width, height))
        }
        val g2 = g as Graphics2D
        if (selected) {
            scaledPoints.forEachIndexed { index, point ->
                val label: String = if (labels)
                    pointNames.getOrNull(index)?.let { name -> "${index + 1}: $name" } ?: "Point $index"
                else
                    "${index + 1}"
                g2.color = colors[index]
                if (scaledFont == null)
                    scaledFont = g2.font.deriveFont(8)
                g2.font = scaledFont!!
                g2.drawString(label, point.first + 4, point.second + 4)
            }
            g2.stroke = BasicStroke(maxOf(0.8f * scale.toFloat(), 2.0f))
            scaledPoints.forEachIndexed { index, point ->
                g2.color = colors[index]
                g2.drawLine(point.first, point.second, point.first, point.second)
            }
        }
    }
}