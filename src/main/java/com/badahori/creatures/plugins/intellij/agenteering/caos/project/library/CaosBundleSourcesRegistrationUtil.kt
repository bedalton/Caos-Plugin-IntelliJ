
package com.badahori.creatures.plugins.intellij.agenteering.caos.project.library

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTable.ModifiableModel
import com.intellij.openapi.vfs.VirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.utils.CaosFileUtil
import com.badahori.creatures.plugins.intellij.agenteering.utils.contents
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFile
import com.badahori.creatures.plugins.intellij.agenteering.vfs.CaosVirtualFileSystem
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import java.util.logging.Logger


object CaosBundleSourcesRegistrationUtil {

    private val LOGGER = Logger.getLogger("#"+ CaosBundleSourcesRegistrationUtil::class.java)
    private const val LIBRARY_NAME = "CaosDef-Std-Lib"
    private const val VERSION_TEXT_FILE_NAME = "version.txt"

    fun register(module:Module?, project:Project) {

        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).smartInvokeLater {
                register(module, project)
            }
            return
        }
        runWriteAction {
            module?.apply { deregisterSources(this) }
            registerSourcesAsLibrary(project)
        }
    }

    fun registerSourcesWithoutModule(module:Module? = null) : Boolean {
        module?.let { deregisterSources(module) }
        val definitionsFolder = CaosFileUtil.getPluginResourceFile(BUNDLE_DEFINITIONS_FOLDER)
                ?: return false
        val vfsDefinitionsFolder = CaosVirtualFile(BUNDLE_DEFINITIONS_FOLDER, null, true).apply {
            CaosVirtualFileSystem.instance.addFile(this)
        }
        for(file in definitionsFolder.children) {
            CaosVirtualFile(file.name, file.contents, false).apply {
                vfsDefinitionsFolder.addChild(this)
            }
        }
        return CaosVirtualFileSystem.instance.exists("$BUNDLE_DEFINITIONS_FOLDER/C1-Lib.caosdef")
    }

    private fun deregisterSources(module:Module) {
        val rootModel = ModuleRootManager.getInstance(module).modifiableModel
        val modifiableModel = rootModel.moduleLibraryTable.modifiableModel
        val oldLibrary = modifiableModel.getLibraryByName(LIBRARY_NAME) ?: return
        oldLibrary.modifiableModel.removeRoot(BUNDLE_DEFINITIONS_FOLDER, OrderRootType.SOURCES)
        rootModel.commit()
    }


    private fun registerSourcesAsLibrary(project:Project) : Boolean {
        val rootModel = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
        val modifiableModel = rootModel.modifiableModel
        val libraryPath = libraryPath()
        // Check if library exists and needs no processing
        if (modifiableModel.libraries.any { it.name == LIBRARY_NAME } && isSourceCurrent(libraryPath, modifiableModel))
            return true

        // Check if library is added properly
        if (!addLibrary(modifiableModel)) {
            modifiableModel.dispose()
            LOGGER.severe("Failed to add library to modifiable model")
            return false
        }

        modifiableModel.commit()
        return true
    }

    fun addLibrary(modifiableModel: ModifiableModel) : Boolean {
        val libraryPath = libraryPath()
                ?: return false
        val library = cleanAndReturnLibrary(modifiableModel = modifiableModel)
                ?: modifiableModel.createLibrary(LIBRARY_NAME, CaosLibraryType.LIBRARY)
        val libModel = library.modifiableModel
        libModel.addRoot(libraryPath, OrderRootType.SOURCES)
        libModel.commit()
        modifiableModel.commit()
        return true
    }


    fun addLibrary(modifiableModel: ModifiableRootModel) : Boolean {
        return addLibrary(modifiableModel.moduleLibraryTable.modifiableModel)
    }

    private fun isSourceCurrent(newLibraryPath: VirtualFile?, model:ModifiableModel) : Boolean {
        val versionString = newLibraryPath?.findFileByRelativePath("version.txt")?.contents
                ?: return false
        val oldVersionString = currentLibraryVersion(model)
                ?: ""
        if (versionString.isEmpty())
            throw Exception("Caos definitions versions cannot be null")
        return versionString == oldVersionString
    }


    private fun cleanAndReturnLibrary(modifiableModel: ModifiableModel) : Library? {
        val oldLibrary = modifiableModel.getLibraryByName(LIBRARY_NAME) ?: return null
        oldLibrary.modifiableModel.removeRoot(BUNDLE_DEFINITIONS_FOLDER, OrderRootType.SOURCES)
        return oldLibrary
    }


    private fun currentLibraryVersion(model:ModifiableModel) : String? {
        return model.getLibraryByName(LIBRARY_NAME)
                ?.getFiles(OrderRootType.SOURCES)
                .orEmpty().firstOrNull { it.name == VERSION_TEXT_FILE_NAME }
                ?.contents
    }


}