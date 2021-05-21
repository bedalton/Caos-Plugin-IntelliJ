package com.badahori.creatures.plugins.intellij.agenteering.sprites.editor

import com.intellij.ui.components.JBList
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.ClipboardOwner
import java.awt.datatransfer.Transferable
import java.awt.dnd.*
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.Action
import javax.swing.DefaultListModel
import javax.swing.DefaultListSelectionModel
import javax.swing.TransferHandler


internal class ImageListPanel<T:HasImage>(private var list: List<T>) : JBList<T>(),
        DragSourceListener,
        DragGestureListener,
        KeyListener,
        ClipboardOwner
{

    init {
        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, this)
        this.selectionModel = object : DefaultListSelectionModel() {
            var gestureStarted = false
            override fun setSelectionInterval(index0: Int, index1: Int) {
                if (!gestureStarted) {
                    super.setSelectionInterval(index0, index1)
                }
                gestureStarted = true
            }

            override fun setValueIsAdjusting(isAdjusting: Boolean) {
                if (!isAdjusting) {
                    gestureStarted = false
                }
            }
        }
        this.model = DefaultListModel<T>().apply {
            list.forEach { addElement(it) }
        }
        actionMap.put(TransferHandler.getCopyAction().getValue(Action.NAME), TransferHandler.getCopyAction())
        transferHandler = SpriteListTransferHandler
    }

    override fun dragEnter(dsde: DragSourceDragEvent?) {

    }

    override fun dragOver(dsde: DragSourceDragEvent?) {

    }

    override fun dropActionChanged(dsde: DragSourceDragEvent?) {

    }

    override fun dragExit(dse: DragSourceEvent?) {
    }

    override fun dragDropEnd(dsde: DragSourceDropEvent?) {
    }

    override fun dragGestureRecognized(e: DragGestureEvent) {
        e.startDrag(DragSource.DefaultCopyDrop,
                SpriteListTransferHandler.createTransferable(this),
                this
        )
    }

    override fun keyTyped(e: KeyEvent?) {
    }

    override fun keyPressed(e: KeyEvent) {
        if (e.isMetaDown && (e.keyChar == 'c')) {
            val data:Transferable? = SpriteListTransferHandler.createTransferable(this)
            if (data != null) {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(data, this@ImageListPanel)
            }
        }
    }

    override fun keyReleased(e: KeyEvent?) {
    }

    override fun lostOwnership(clipboard: Clipboard?, contents: Transferable?) {
    }
}