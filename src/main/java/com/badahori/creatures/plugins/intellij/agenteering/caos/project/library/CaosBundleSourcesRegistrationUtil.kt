package com.badahori.creatures.plugins.intellij.agenteering.caos.project.library

import com.badahori.creatures.plugins.intellij.agenteering.utils.contents
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTable.ModifiableModel
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.vfs.VirtualFile
import java.util.logging.Logger


object CaosBundleSourcesRegistrationUtil {

    private val LOGGER = Logger.getLogger("#" + CaosBundleSourcesRegistrationUtil::class.java)
    private const val LIBRARY_NAME = "CaosDef-Std-Lib"
    private const val VERSION_TEXT_FILE_NAME = "version.txt"
    private var registeredOnce = false
    private var didSucceedOnce = false

    fun register(module: Module?, project: Project) {
        if (project.isDisposed) {
            return
        }

        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).smartInvokeLater {
                if (project.isDisposed) {
                    return@smartInvokeLater
                }
                register(module, project)
            }
            return
        }
        runWriteAction {
            //module?.apply { deregisterSources(this) }
            registerSourcesAsLibrary(project)
        }
    }

    /*
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

*/
    private fun registerSourcesAsLibrary(project: Project): Boolean {
        if (registeredOnce) {
            return didSucceedOnce
        }
        val rootModel = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
        val modifiableModel = rootModel.modifiableModel
        val libraryPath = libraryPath()
        // Check if library exists and needs no processing
        didSucceedOnce = if (modifiableModel.libraries.any { it.name == LIBRARY_NAME } && isSourceCurrent(
                libraryPath,
                modifiableModel
            )) {
            true
        } else if (!addLibrary(modifiableModel)) {
            // Check if library is added properly
            LOGGER.severe("Failed to add library to modifiable model")
            registeredOnce = true
            false
        } else {
           true
        }
        modifiableModel.dispose()
        registeredOnce = true
        return didSucceedOnce
    }

    private fun addLibrary(modifiableModel: ModifiableModel): Boolean {
        val libraryPath = libraryPath()
            ?: return false
        val library = cleanAndReturnLibrary(
            modifiableModel = modifiableModel,
            libraryPath = libraryPath.path
        )
            ?: modifiableModel.createLibrary(LIBRARY_NAME, CaosLibraryType.LIBRARY)
        val libModel = library.modifiableModel
        libModel.addRoot(libraryPath, OrderRootType.SOURCES)
        libModel.commit()
        modifiableModel.commit()
        return true
    }

    private fun isSourceCurrent(newLibraryPath: VirtualFile?, model: ModifiableModel): Boolean {
        val versionString = newLibraryPath?.findFileByRelativePath("version.txt")?.contents
            ?: return false
        val oldVersionString = currentLibraryVersion(model)
            ?: ""
        if (versionString.isEmpty())
            throw Exception("Caos definitions versions cannot be null")
        return model.getLibraryByName(LIBRARY_NAME)?.getUrls(OrderRootType.SOURCES)?.size == 1 && versionString == oldVersionString
    }


    private fun cleanAndReturnLibrary(modifiableModel: ModifiableModel, libraryPath: String): Library? {
        val oldLibrary = modifiableModel.getLibraryByName(LIBRARY_NAME)
            ?: return null
        val oldModifiableModel = oldLibrary.modifiableModel
        oldModifiableModel.removeRoot(BUNDLE_DEFINITIONS_FOLDER, OrderRootType.SOURCES)
        oldModifiableModel.removeRoot(libraryPath, OrderRootType.SOURCES)
        oldModifiableModel.getUrls(OrderRootType.SOURCES).forEach { url ->
            oldModifiableModel.removeRoot(url, OrderRootType.SOURCES)
            oldModifiableModel.removeRoot(url.split('!').first(), OrderRootType.SOURCES)
        }
        oldModifiableModel.commit()
        return oldLibrary
    }

    private fun currentLibraryVersion(model: ModifiableModel): String? {
        return model.getLibraryByName(LIBRARY_NAME)
            ?.getFiles(OrderRootType.SOURCES)
            .orEmpty()
            .firstOrNull { it.name == VERSION_TEXT_FILE_NAME }
            ?.contents
    }
}