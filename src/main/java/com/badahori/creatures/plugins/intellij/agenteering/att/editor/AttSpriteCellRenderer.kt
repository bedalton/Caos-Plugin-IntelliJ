package com.badahori.creatures.plugins.intellij.agenteering.att.editor

import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.JBColor
import javafx.scene.input.MouseButton
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.JPanel
import kotlin.math.floor
import java.awt.Dimension


private val colors = listOf(
    Color.YELLOW,
    Color.GREEN,
    Color.CYAN,
    Color.MAGENTA,
    Color(47,170, 240),
    Color.PINK
)


data class AttSpriteCellData(
    val index: Int,
    val image: BufferedImage,
    val points: List<Pair<Int, Int>>,
    val pointNames: List<String>,
    private val callback: OnChangePoint
) {
    fun onPlace(point: Pair<Int, Int>) {
        callback.onChangePoint(index, point)
    }
}


internal class AttSpriteCellComponent() : JPanel() {
    init {
        this.layout = BoxLayout(this, BoxLayout.X_AXIS)
        this.border = BorderFactory.createEmptyBorder(5, 16, 10, 5)
    }

    internal fun update(labels:Boolean, scale: Double, value: AttSpriteCellData) {
        removeAll()
        this.add(JLabel("${value.index}."))
        val imageValue = value.image
        val width = imageValue.width * scale
        val height = imageValue.height * scale
        val image = if (scale > 1.01) {
            imageValue.getScaledInstance(
                width.toInt(),
                height.toInt(),
                Image.SCALE_AREA_AVERAGING
            ) // scale it the smooth way
        } else {
            imageValue
        }

        val scaledPoints: List<Pair<Int, Int>> = value.points.map {
            Pair(floor(it.first * scale).toInt(), floor(it.second * scale).toInt())
        }

        val canvas = object : JPanel() {
            var scaledFont: Font? = null
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                g.drawImage(image, 0, 0, this)
                val g2 = g as Graphics2D
                scaledPoints.forEachIndexed { index, point ->
                    val label: String = if (labels)
                        value.pointNames.getOrNull(index)?.let { name -> "${index + 1}: $name" } ?: "Point $index"
                    else
                        "${index+1}"
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
        }.apply {
            val imageDimension = Dimension(width.toInt() + 50, height.toInt() + 40)
            size = imageDimension
            preferredSize = imageDimension
            minimumSize = imageDimension
            background = Color.BLACK;
        }
        val dimension = Dimension(width.toInt() + 120, height.toInt() + 40)
        this.size = dimension
        this.preferredSize = dimension
        this.minimumSize = dimension
        canvas.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                super.mouseClicked(e)
                if (e.source !== canvas)
                    return
                val point = e.point
                if (!canvas.contains(point)) {
                    return
                }
                if (e.button == MouseEvent.BUTTON1) {
                    val x = (e.x / scale).toInt()
                    val y = (e.y / scale).toInt()

                    if (imageValue.width < x || imageValue.height < y) {
                        return
                    }
                    value.onPlace(Pair(x, y))
                }
            }
        })
        canvas.isFocusable = true
        add(canvas)
        revalidate()
        repaint()
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
    var labels:Boolean = true
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
            val component = AttSpriteCellComponent();
            pool.add(component)
            add(component)
        }
        return pool.get(index);
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
        reload();
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
        panel.update(labels, scale, value)
    }
}

interface OnChangePoint {
    fun onChangePoint(index: Int, newPoint: Pair<Int, Int>)
}
