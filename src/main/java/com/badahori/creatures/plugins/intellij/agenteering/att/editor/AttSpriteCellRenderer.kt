package com.badahori.creatures.plugins.intellij.agenteering.att.editor

import com.badahori.creatures.plugins.intellij.agenteering.utils.invokeLater
import com.badahori.creatures.plugins.intellij.agenteering.utils.orTrue
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.soywiz.korma.math.min
import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import javax.swing.*
import kotlin.math.floor


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
    private val changePointListener: OnChangePoint,
    private val changeCellListener: HasSelectedCell,
) {
    val isFocused: Boolean get() = changeCellListener.selectedCell == index
    fun onPlace(point: Pair<Int, Int>) {
        if (changeCellListener.selectedCell == index)
            changePointListener.onChangePoint(index, point)
        else
            onFocus()
    }

    fun onFocus() {
        if (!isFocused)
            changeCellListener.setSelected(index)
    }

    fun shiftPoint(xDelta: Int, yDelta: Int) {
        changePointListener.onShiftPoint(index, Pair(xDelta, yDelta))
    }

}


internal class AttSpriteCellComponent : JPanel() {
    init {
        this.layout = BoxLayout(this, BoxLayout.X_AXIS)
        this.border = BorderFactory.createEmptyBorder(5, 16, 10, 5)
        inheritsPopupMenu = true
        this.isFocusable = true
    }

    internal fun update(labels: Boolean, scale: Double, value: AttSpriteCellData, selected: Boolean) {
        removeAll()
        this.add(JLabel("${value.index + 1}.").apply { inheritsPopupMenu = true })
        val imageValue = value.image
        val width = imageValue.width * scale
        val height = imageValue.height * scale
        val image = if (width > 0 && height > 0) {
            imageValue.getScaledInstance(
                width.toInt(),
                height.toInt(),
                Image.SCALE_AREA_AVERAGING
            ) // scale it the smooth way
        } else {
            imageValue
        }

        val scaledPoints: List<Pair<Int, Int>> = value.points.subList(0, minOf(6, value.points.size)).map {
            Pair(floor(it.first * scale).toInt(), floor(it.second * scale).toInt())
        }

        val canvas = object : JPanel() {
            var scaledFont: Font? = null
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                g.drawImage(image, 0, 0, this)
                if (!selected) {
                    g.color = Color(0, 0, 0, 60)
                    g.fillRect(0, 0, width.toInt(), height.toInt())
                    g.color = Color(255, 255, 255, 255)
                    g.drawCenteredString("Click to Edit", Rectangle(0, 0, width.toInt(), height.toInt()))
                }
                val g2 = g as Graphics2D
                if (selected) {
                    scaledPoints.forEachIndexed { index, point ->
                        val label: String = if (labels)
                            value.pointNames.getOrNull(index)?.let { name -> "${index + 1}: $name" } ?: "Point $index"
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
        }.apply {
            val imageDimension = Dimension(width.toInt() + 50, height.toInt() + 40)
            size = imageDimension
            preferredSize = imageDimension
            minimumSize = imageDimension
            background = Color.BLACK
        }
        val dimension = Dimension(width.toInt() + 120, height.toInt() + 40)
        this.size = dimension
        this.preferredSize = dimension
        this.minimumSize = dimension

        isFocusable = true
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                super.mouseClicked(e)
                if (e.button == MouseEvent.BUTTON1) {
                    value.onFocus()
                }
            }
        })
        canvas.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                super.mouseClicked(e)
                if (e.source !== canvas) {
                    return
                }
                val point = e.point
                if (!canvas.contains(point)) {
                    return
                }
                if (e.button == MouseEvent.BUTTON1) {
                    val x = (e.x / scale).toInt()
                    val y = (e.y / scale).toInt()

                    if (imageValue.width < x || imageValue.height < y) {
                        value.onFocus()
                        return
                    }
                    if (x < 0 || y < 0)
                        return
                    value.onPlace(Pair(x, y))
                }
            }
        })

        bindKeyStroke(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "move.up", KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)) {
            value.shiftPoint(0, -1)
        }
        bindKeyStroke(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "move.up", KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, 0)) {
            value.shiftPoint(0, -1)
        }
        bindKeyStroke(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "move.down", KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)) {
            value.shiftPoint(0, 1)
        }
        bindKeyStroke(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "move.down", KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0)) {
            value.shiftPoint(0, 1)
        }
        bindKeyStroke(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "move.right", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0)) {
            value.shiftPoint(1, 0)
        }
        bindKeyStroke(
            WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
            "move.right",
            KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, 0)
        ) {
            value.shiftPoint(1, 0)
        }
        bindKeyStroke(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "move.left", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0)) {
            value.shiftPoint(-1, 0)
        }
        bindKeyStroke(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "move.left", KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, 0)) {
            value.shiftPoint(-1, 0)
        }
        canvas.isFocusable = true
        canvas.inheritsPopupMenu = true
        add(canvas)
        revalidate()
        repaint()
        if (selected)
            requestFocus()
    }

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
}

/**
 * Cell renderer for individual sprite image in Sprite file list
 */
internal class AttSpriteCellList(
    private var listItems: List<AttSpriteCellData>,
    private var scale: Double = 1.0,
    var maxWidth: Int = 300,
    var maxHeight: Int = 300,
    var labels: Boolean = true
) : JPanel() {

    private val pool: MutableList<AttSpriteCellComponent> = mutableListOf()

    private var color: Color = EditorColorsManager.getInstance().globalScheme.defaultBackground

    init {
        if (color.alpha > 0) {
            this.background = color
            this.isOpaque = true
        }
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

    fun setItems(newItems: List<AttSpriteCellData>) {
        this.listItems = newItems
        val size = newItems.size
        if (pool.size > size) {
            pool.forEachIndexed { i, component ->
                if (i < size)
                    return
                component.isVisible = false
            }
        }
        reload()
    }

    init {
        layout = GridLayout(0, 1)
        reload()
    }

    fun reload() {
        listItems.forEachIndexed { i, item ->
            setCell(item, i)
        }
        revalidate()
        repaint()
    }

    private fun setCell(value: AttSpriteCellData, index: Int) {
        val panel = get(index)
        panel.update(labels, scale, value, value.isFocused)
    }

    fun scrollTo(pose: Int) {
        val item = get(pose)
        invokeLater later@{
            (parent as? JPanel)?.doLayout()
            val bounds = item.bounds
            if (visibleRect.contains(bounds).orTrue())
                return@later
            val offset = ((pose * 0.85) * item.height).toInt()
            (parent?.parent as? JScrollPane)?.verticalScrollBar?.value = offset

        }
    }
}

interface OnChangePoint {
    fun onChangePoint(lineNumber: Int, newPoint: Pair<Int, Int>)
    fun onShiftPoint(lineNumber: Int, offset: Pair<Int, Int>)
}

interface HasSelectedCell {
    val selectedCell: Int
    fun setSelected(index: Int)
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