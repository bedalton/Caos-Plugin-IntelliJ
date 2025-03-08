package com.badahori.creatures.plugins.intellij.agenteering.caos.project.module

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.openapi.module.ModifiableModuleModel
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import org.jetbrains.annotations.NonNls
import java.io.IOException
import kotlin.collections.MutableList
import com.intellij.ide.util.projectWizard.SourcePathsBuilder


class NewCaosModuleBuilder : SourcePathsBuilder {
    private var myCompilerOutputPath: String? = null

    // Pair<Source Path, Package Prefix>
    private var mySourcePaths: MutableList<Pair<String?, String?>>? = null

    // Pair<Library path, Source path>
    private val myModuleLibraries: MutableList<Pair<String?, String?>> = ArrayList<Pair<String?, String?>>()
    fun setCompilerOutputPath(compilerOutputPath: String?) {
        myCompilerOutputPath = acceptParameter(compilerOutputPath)
    }

    val sourcePaths: MutableList<Pair<String?, String?>>
        get() {
            if (mySourcePaths == null) {
                val paths: MutableList<Pair<String?, String?>> = ArrayList<Pair<String?, String?>>()
                val contentEntry: String? = Objects.requireNonNull(getContentEntryPath())
                val path: @NonNls Path = Path.of(contentEntry).resolve("src")
                try {
                    NioFiles.createDirectories(path)
                } catch (e: IOException) {
                    LOG.error(e)
                    File(path.toString()).mkdirs() // maybe this will succeed...
                }
                paths.add(Pair.create(path.toString(), ""))
                return paths
            }
            return mySourcePaths!!
        }

    override fun isAvailable(): Boolean {
        return false
    }

    public override fun setSourcePaths(sourcePaths: MutableList<Pair<String?, String?>?>?) {
        mySourcePaths = if (sourcePaths != null) ArrayList<Pair<String?, String?>>(sourcePaths) else null
    }

    public override fun addSourcePath(sourcePathInfo: Pair<String?, String?>?) {
        if (mySourcePaths == null) {
            mySourcePaths = ArrayList<Pair<String?, String?>>()
        }
        mySourcePaths!!.add(sourcePathInfo!!)
    }

    override fun getModuleType(): ModuleType<*> {
        return StdModuleTypes.JAVA
    }

    override fun isSuitableSdkType(sdkType: SdkTypeId?): Boolean {
        return sdkType is JavaSdkType && !(sdkType as JavaSdkType).isDependent()
    }

    @Nullable
    override fun modifySettingsStep(@NotNull settingsStep: SettingsStep?): ModuleWizardStep {
        return StdModuleTypes.JAVA.modifySettingsStep(settingsStep, this)
    }

    @Throws(ConfigurationException::class)
    override fun setupRootModel(@NotNull rootModel: ModifiableRootModel) {
        val compilerModuleExtension =
            rootModel.getModuleExtension<CompilerModuleExtension?>(CompilerModuleExtension::class.java)
        compilerModuleExtension.setExcludeOutput(true)
        if (myJdk != null) {
            rootModel.setSdk(myJdk)
        } else {
            rootModel.inheritSdk()
        }

        val contentEntry = doAddContentEntry(rootModel)
        if (contentEntry != null) {
            val sourcePaths: MutableList<Pair<String?, String?>>? = this.sourcePaths

            if (sourcePaths != null) {
                for (sourcePath in sourcePaths) {
                    val first = sourcePath.first
                    File(first).mkdirs()
                    val sourceRoot = LocalFileSystem.getInstance()
                        .refreshAndFindFileByPath(FileUtil.toSystemIndependentName(first))
                    if (sourceRoot != null) {
                        contentEntry.addSourceFolder(sourceRoot, false, sourcePath.second!!)
                    }
                }
            }
        }

        if (myCompilerOutputPath != null) {
            // should set only absolute paths
            var canonicalPath: String?
            try {
                canonicalPath = FileUtil.resolveShortWindowsName(myCompilerOutputPath)
            } catch (e: IOException) {
                canonicalPath = myCompilerOutputPath
            }
            compilerModuleExtension
                .setCompilerOutputPath(VfsUtilCore.pathToUrl(canonicalPath!!))
        } else {
            compilerModuleExtension.inheritCompilerOutputPath(true)
        }

        val libraryTable = rootModel.getModuleLibraryTable()
        for (libInfo in myModuleLibraries) {
            val moduleLibraryPath = libInfo.first
            val sourceLibraryPath = libInfo.second
            val library: Library = libraryTable.createLibrary()
            val modifiableModel: Library.ModifiableModel = library.getModifiableModel()
            modifiableModel.addRoot(getUrlByPath(moduleLibraryPath), OrderRootType.CLASSES)
            if (sourceLibraryPath != null) {
                modifiableModel.addRoot(getUrlByPath(sourceLibraryPath), OrderRootType.SOURCES)
            }
            modifiableModel.commit()
        }
    }

    @Nullable
    public override fun commit(
        @NotNull project: Project,
        model: ModifiableModuleModel?,
        modulesProvider: ModulesProvider?,
    ): MutableList<Module?>? {
        val extension: LanguageLevelProjectExtension =
            LanguageLevelProjectExtension.getInstance(ProjectManager.getInstance().getDefaultProject())
        val aDefault: Boolean? = extension.getDefault()
        LOG.debug("commit: aDefault=" + aDefault)
        val instance: LanguageLevelProjectExtension = LanguageLevelProjectExtension.getInstance(project)
        if (aDefault != null && !aDefault) {
            instance.setLanguageLevel(extension.getLanguageLevel())
        } else {
            //setup language level according to jdk, then setup default flag
            val sdk = ProjectRootManager.getInstance(project).getProjectSdk()
            LOG.debug("commit: projectSdk=" + sdk)
            if (sdk != null) {
                val version: JavaSdkVersion? = JavaSdk.getInstance().getVersion(sdk)
                LOG.debug("commit: sdk.version=" + version)
                if (version != null) {
                    instance.setLanguageLevel(version.getMaxLanguageLevel())
                    instance.setDefault(true)
                }
            }
        }
        return super.commit(project, model, modulesProvider)
    }

    fun addModuleLibrary(moduleLibraryPath: String?, sourcePath: String?) {
        myModuleLibraries.add(Pair.create(moduleLibraryPath, sourcePath))
    }

    override fun getWeight(): Int {
        return JAVA_WEIGHT
    }

    companion object {
        private val LOG: Logger = Logger.getInstance(JavaModuleBuilder::class.java)
        val BUILD_SYSTEM_WEIGHT: Int = JVM_WEIGHT
        val JAVA_WEIGHT: Int = BUILD_SYSTEM_WEIGHT + 20
        const val JAVA_MOBILE_WEIGHT: Int = 60

        private fun getUrlByPath(path: String?): String {
            return VfsUtil.getUrlForLibraryRoot(File(path))
        }

        @get:Nullable
        protected val pathForOutputPathStep: String?
            get() = null
    }
}