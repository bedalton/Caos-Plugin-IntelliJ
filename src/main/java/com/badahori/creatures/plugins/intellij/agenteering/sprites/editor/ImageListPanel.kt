package com.badahori.creatures.plugins.intellij.agenteering.sprites.editor

import com.intellij.ui.components.JBList
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.ClipboardOwner
import java.awt.datatransfer.Transferable
import java.awt.dnd.*
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.Action
import javax.swing.DefaultListModel
import javax.swing.DefaultListSelectionModel
import javax.swing.TransferHandler
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


internal class ImageListPanel<T : HasImage>(private var list: List<T>) : JBList<T>(),
    DragSourceListener,
    DragGestureListener,
    KeyListener,
    ClipboardOwner,
    MouseListener {

    var didDrag = false
    var dragging = false
    var dragXStart: Int? = null
    var dragYStart: Int? = null
    var lastItemClickedMin: Int? = null
    var lastItemClickedMax: Int? = null
    var setSelectionIntervalActual: (e:MouseEvent,min: Int, max: Int) -> Unit

    init {
        DragSource.getDefaultDragSource()
            .createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, this)
        this.selectionModel = object : DefaultListSelectionModel() {

            init {
                this@ImageListPanel.setSelectionIntervalActual = { e:MouseEvent, min: Int, max: Int ->
                    if (e.isMetaDown || e.isControlDown) {
                        addSelectionInterval(min, max)
                    } else
                        super.setSelectionInterval(min, max)
                }
            }

            override fun setSelectionInterval(index0: Int, index1: Int) {
                if (dragging) {
                    lastItemClickedMin = index0
                    lastItemClickedMax = index1
                    return
                } else {
                    super.setSelectionInterval(index0, index1)
                }
            }
        }
        this.model = DefaultListModel<T>().apply {
            list.forEach { addElement(it) }
        }
        addMouseListener(this)
        actionMap.put(TransferHandler.getCopyAction().getValue(Action.NAME), TransferHandler.getCopyAction())
        transferHandler = SpriteListTransferHandler
    }

    override fun dragEnter(dsde: DragSourceDragEvent?) {

    }

    override fun dragOver(dsde: DragSourceDragEvent?) {

    }

    override fun dropActionChanged(dsde: DragSourceDragEvent?) {

    }

    override fun dragExit(dse: DragSourceEvent) {
    }

    override fun dragDropEnd(dsde: DragSourceDropEvent?) {
    }

    override fun dragGestureRecognized(e: DragGestureEvent) {
        if (selectedIndices.isEmpty())
            return
        e.startDrag(
            DragSource.DefaultCopyDrop,
            SpriteListTransferHandler.createTransferable(this),
            this
        )
    }

    override fun keyTyped(e: KeyEvent?) {
    }

    override fun keyPressed(e: KeyEvent) {
        if (e.isMetaDown && (e.keyChar == 'c')) {
            val data: Transferable? = SpriteListTransferHandler.createTransferable(this)
            if (data != null) {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(data, this@ImageListPanel)
            }
        }
    }

    override fun keyReleased(e: KeyEvent?) {
    }

    override fun lostOwnership(clipboard: Clipboard?, contents: Transferable?) {
    }

    override fun mouseClicked(e: MouseEvent) {
        dragging = false
        lastItemClickedMin = null
        lastItemClickedMax = null
    }

    override fun mousePressed(e: MouseEvent) {
        dragging = true
        dragXStart = e.x
        dragYStart = e.y
    }

    override fun mouseReleased(e: MouseEvent) {
        dragging = false
        val distanceX = dragXStart?.let { abs(max(it, e.x) - min(it, e.x)) } ?: 0
        dragXStart = null
        val distanceY = dragYStart?.let { abs(max(it, e.y) - min(it, e.y)) } ?: 0
        dragYStart = null
        didDrag = distanceX + distanceY > 10
        if (!didDrag) {
            val selectedMin = lastItemClickedMin
            lastItemClickedMin = null
            val selectedMax = lastItemClickedMax
            lastItemClickedMax = null
            if (selectedMin != null && selectedMax != null)
                setSelectionIntervalActual(e, selectedMin, selectedMax)
        }
    }

    override fun mouseEntered(e: MouseEvent?) {
    }

    override fun mouseExited(e: MouseEvent?) {
    }
}