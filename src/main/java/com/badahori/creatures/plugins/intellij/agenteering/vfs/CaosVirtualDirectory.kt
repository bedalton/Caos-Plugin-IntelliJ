package com.badahori.creatures.plugins.intellij.agenteering.vfs

import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.invokeLater
import com.badahori.creatures.plugins.intellij.agenteering.utils.randomString
import com.bedalton.common.util.className
import com.bedalton.common.util.ensureEndsWith
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class CaosVirtualDirectory(
    private val project: Project,
    parent: VirtualFile?,
    name: String,
    children: List<VirtualFile>
): VirtualFile(), ModificationTracker {

    private var mModificationCount = 0L
    private val mName = name
    private val mParent: VirtualFile? = parent

    private val  mTimeStamp = Date().time

    internal var children = children.associateBy { it.name }.toMutableMap()

    val isEmpty: Boolean get() = children.isEmpty()

    override fun getName(): String {
        return mName
    }

    override fun getFileSystem(): VirtualFileSystem {
        return mParent?.fileSystem ?: CaosVirtualFileSystem.instance
    }

    override fun getPath(): String {
        return mParent?.path.orEmpty().ensureEndsWith('/') + name
    }

    override fun isWritable(): Boolean {
        return true
    }

    override fun isDirectory(): Boolean {
        return true
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun getParent(): VirtualFile? {
        return mParent
    }

    override fun getChildren(): Array<VirtualFile> {
        return children.values.toTypedArray()
    }

    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream {
        throw IllegalStateException("CaosVirtualFolder does not have an output stream")
    }

    override fun contentsToByteArray(): ByteArray {
        throw IllegalStateException("CaosVirtualFolder does not byte contents")
    }

    override fun getTimeStamp(): Long {
        return children.values.maxOfOrNull { it.timeStamp } ?: mTimeStamp
    }

    override fun getLength(): Long {
        return 0
    }

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {
        val keys = children.keys
        val all = children.values
        var changed = false
        for (key in keys) {
            val child = children[key]
                ?: continue
            if (child.parent != mParent) {
                children.remove(key)
                changed = true
            }
        }

        if (changed) {
            all.forEach { it.refresh(asynchronous, recursive, postRunnable) }
            mParent?.refresh(true, true)
            mModificationCount++
        }
    }

    private fun refreshProjectView() {
        if (project.isDisposed) {
            return
        }
        invokeLater {
            if (project.isDisposed) {
                return@invokeLater
            }
            val view = ProjectView.getInstance(project)
            view.refresh()
        }
    }

    override fun getInputStream(): InputStream {
        throw IllegalStateException("CaosVirtualFolder does not have an input stream")
    }

    override fun getModificationCount(): Long {
        return mModificationCount
    }

    override fun delete(requestor: Any?) {
        if (project.isDisposed) {
            return
        }
        WriteCommandAction.runWriteCommandAction(
            project,
            "Delete Files",
            "delete-files-" + randomString(8), {
                deleteWithoutCommand(requestor)
        })
    }

    private fun deleteWithoutCommand(requestor: Any?) {
        for (child in children.values) {
            if (child is CaosVirtualDirectory) {
                child.deleteWithoutCommand(requestor)
            } else {
                child.delete(requestor)
            }
        }
        children.clear()
    }

    override fun move(requestor: Any?, newParent: VirtualFile) {
        if (project.isDisposed) {
            return
        }
        if (newParent is CaosVirtualFile || newParent is CaosVirtualDirectory) {
            throw IllegalStateException("Cannot move files into non-physical file")
        }

        val fileSystem = newParent.fileSystem as? LocalFileSystem
            ?: throw IllegalStateException("Cannot move files into non-local filesystem")

        val children = children.values
        for (child in children) {
            if (child !is CaosVirtualDirectory) {
                fileSystem.moveFile(requestor, child, newParent)
            } else {
                child.move(requestor, newParent)
            }
        }
        for (child in children.filterIsInstance<CaosVirtualDirectory>()) {
            if (child.isEmpty) {
                child.delete(child)
            } else {
                LOGGER.severe("Virtual directory is not empty after move. Contains: ${child.children.values.joinToString { it.className + ": " + it.name }}")
            }
        }
        try {
            if (isEmpty) {
                delete(requestor)
            }
        } catch (_: Exception) {
        }
        children.clear()
        this.refresh(true, true)
        mParent?.refresh(true, true)
        newParent.refresh(true, true)
        refreshProjectView()
        fileSystem.refreshFiles(listOf(newParent, mParent, this) + children, true, true, null)
        mModificationCount++
    }
}