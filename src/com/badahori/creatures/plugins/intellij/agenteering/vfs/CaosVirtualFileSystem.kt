package com.badahori.creatures.plugins.intellij.agenteering.vfs

/*
 * Copyright 2007 Steve Chaloner
 * @modifiedBy Daniel Badal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

import com.badahori.creatures.plugins.intellij.agenteering.utils.contents
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.*
import java.io.IOException
import java.util.*


/**
 * A file system for content that resides only in memory.
 *
 * @author Steve Chaloner
 */
class CaosVirtualFileSystem : DeprecatedVirtualFileSystem() {
    /**
     * The files.
     */
    private val files: MutableMap<String, CaosVirtualFile> = HashMap()

    /**
     * Listeners for file system events.
     */
    private val listeners: MutableList<VirtualFileListener> = ArrayList()

    /** {@inheritDoc}  */
    override fun addVirtualFileListener(virtualFileListener: VirtualFileListener) {
        super.addVirtualFileListener(virtualFileListener)
        listeners.add(virtualFileListener)
    }

    /** {@inheritDoc}  */
    override fun removeVirtualFileListener(virtualFileListener: VirtualFileListener) {
        super.removeVirtualFileListener(virtualFileListener)
        listeners.remove(virtualFileListener)
    }

    /**
     * Add a file to the file system.
     *
     * @param file the file to add
     */
    fun addFile(file: CaosVirtualFile) {
        files[file.name] = file
        fireFileCreated(file)
    }

    /**
     * Notifies listeners of a new file.
     *
     * @param file the new file
     */
    private fun fireFileCreated(file: CaosVirtualFile) {
        val e = VirtualFileEvent(this,
                file,
                file.name,
                file.getParent())
        for (listener in listeners) {
            listener.fileCreated(e)
        }
    }

    /** {@inheritDoc}  */
    override fun getProtocol(): String {
        return CAOS_VFS_PROTOCOL
    }

    /** {@inheritDoc}  */
    override fun findFileByPath(string: String): VirtualFile? {
        // todo rewrite this so it doesn't look like crap
        var file: VirtualFile? = null
        if (!StringUtil.isEmptyOrSpaces(string)) {
            val path = VirtualFileManager.extractPath(string)
            val st = StringTokenizer(path, "/")
            var currentFile: VirtualFile? = files[CAOS_VFS_ROOT]
            var keepLooking = currentFile != null
            var targetName: String? = null
            while (keepLooking && st.hasMoreTokens()) {
                val element = st.nextToken()
                if (!st.hasMoreTokens()) {
                    targetName = element
                }
                val child = currentFile!!.findChild(element)
                if (child != null) {
                    currentFile = child
                } else {
                    keepLooking = false
                }
            }
            if (currentFile != null && targetName != null && targetName == currentFile.name) {
                file = currentFile
            }
        }
        return file
    }

    /** {@inheritDoc}  */
    override fun refresh(b: Boolean) {}

    /** {@inheritDoc}  */
    override fun refreshAndFindFileByPath(string: String): VirtualFile? {
        return files[string]
    }

    /** {@inheritDoc}  */
    @Throws(IOException::class)
    public override fun deleteFile(requestor: Any?,
                                   virtualFile: VirtualFile) {
        files.remove(virtualFile.name)
        (virtualFile.parent as? CaosVirtualFile)
                ?.deleteChild(virtualFile as CaosVirtualFile)
    }

    /** {@inheritDoc}  */
    @Throws(IOException::class)
    public override fun moveFile(requestor: Any?,
                                 virtualFile: VirtualFile,
                                 virtualFile1: VirtualFile) {
        files.remove(virtualFile.name)
        files[virtualFile1.name] = virtualFile1 as CaosVirtualFile
    }

    /** {@inheritDoc}  */
    @Throws(IOException::class)
    public override fun renameFile(requestor: Any?,
                                   virtualFile: VirtualFile,
                                   string: String) {
        files.remove(virtualFile.name)
        files[string] = virtualFile as CaosVirtualFile
    }

    /** {@inheritDoc}  */
    @Throws(IOException::class)
    override fun createChildFile(requestor: Any?,
                                 parent: VirtualFile,
                                 name: String): CaosVirtualFile {
        val file = CaosVirtualFile(name,null)
        file.parent = parent
        addFile(file)
        return file
    }

    /** {@inheritDoc}  */
    @Throws(IOException::class)
    override fun createChildDirectory(requestor: Any?,
                                      parent: VirtualFile,
                                      name: String): CaosVirtualFile {
        val file = CaosVirtualFile(name, null, true)
        (parent as CaosVirtualFile).addChild(file)
        addFile(file)
        return file
    }

    /**
     * For a given package, e.g. net.stevechaloner.intellijad, get the file corresponding
     * to the last element, e.g. intellijad.  If the file or any part of the directory tree
     * does not exist, it is created dynamically.
     *
     * @param packageName the name of the package
     * @return the file corresponding to the final location of the package
     */
    fun getFileForPackage(packageName: String): CaosVirtualFile? {
        val st = StringTokenizer(packageName, ".")
        val names: MutableList<String> = ArrayList()
        while (st.hasMoreTokens()) {
            names.add(st.nextToken())
        }
        return files[CAOS_VFS_ROOT]?.let { getFileForPackage(names, it) }
    }

    /**
     * Recursively search for, and if necessary create, the final file in the
     * name list.
     *
     * @param namesIn  the name list
     * @param parent the parent file
     * @return a file corresponding to the last entry in the name list
     */
    private fun getFileForPackage(namesIn: List<String>,
                                  parent: CaosVirtualFile): CaosVirtualFile? {
        val names = namesIn.toMutableList()
        var child: CaosVirtualFile? = null
        if (names.isNotEmpty()) {
            val name: String = names.removeAt(0)
            child = parent.getChild(name)
            if (child == null) {
                try {
                    child = createChildDirectory(null,
                            parent,
                            name)
                } catch (e: IOException) {
                    Logger.getInstance(javaClass.name).error(e)
                }
            }
        }
        if (child != null && names.isNotEmpty()) {
            child = getFileForPackage(names,
                    child)
        }
        return child
    }

    fun getFileAtPath(path:String) : CaosVirtualFile? {
        return files[CAOS_VFS_ROOT]?.let { getFileForPackage(path.split("/"), it) }
    }

    fun exists(path:String) : Boolean {
        return files[CAOS_VFS_ROOT]?.let { getFileForPackage(path.split("/"), it) } != null
    }

    override fun isCaseSensitive(): Boolean {
        return true
    }

    /** {@inheritDoc}  */
    @Throws(IOException::class)
    override fun copyFile(o: Any,
                          virtualFile: VirtualFile,
                          newParent: VirtualFile,
                          copyName: String): VirtualFile {
        return CaosVirtualFile(copyName, virtualFile.contents, true).apply {
            parent = newParent
        }
    }

    /** {@inheritDoc}  */
    override fun isReadOnly(): Boolean {
        return false
    }

    /** {@inheritDoc}  */
    fun exists(virtualFile: VirtualFile?): Boolean {
        return files.containsValue(virtualFile)
    }

    /** {@inheritDoc}  */
    fun isDirectory(virtualFile: VirtualFile): Boolean {
        return virtualFile.isDirectory
    }

    companion object {
        val instance:CaosVirtualFileSystem by lazy {
            VirtualFileManager.getInstance().getFileSystem(CAOS_VFS_PROTOCOL) as CaosVirtualFileSystem
        }
        /**
         * The name of the component.
         */
        private const val COMPONENT_NAME = "CaosVirtualFileSystem"
    }
}
