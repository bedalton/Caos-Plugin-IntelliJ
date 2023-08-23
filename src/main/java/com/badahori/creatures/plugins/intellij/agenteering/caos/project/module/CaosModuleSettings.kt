@file:Suppress("DEPRECATION")

package com.badahori.creatures.plugins.intellij.agenteering.caos.project.module

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.HasIgnoredCatalogueTags
import com.badahori.creatures.plugins.intellij.agenteering.utils.CaosVariantConverter
import com.badahori.creatures.plugins.intellij.agenteering.utils.StringListConverter
import com.intellij.openapi.components.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleServiceManager
import com.intellij.util.xmlb.annotations.Attribute

interface CaosModuleSettingsService : HasIgnoredCatalogueTags {
    fun getState(): CaosModuleSettings
    fun loadState(settingsIn: CaosModuleSettings)

    companion object {
        @JvmStatic
        fun getInstance(module: Module): CaosModuleSettingsService? {
            return ModuleServiceManager.getService(module, CaosModuleSettingsService::class.java)
        }
    }
}

@State(
    name = "CaosModuleSettings",
    storages = [Storage(StoragePathMacros.MODULE_FILE)]
)
class CaosModuleSettingsComponent : PersistentStateComponent<CaosModuleSettings>, CaosModuleSettingsService {

    private var settings = CaosModuleSettings()

    override fun getState(): CaosModuleSettings {
        return settings
    }

    override fun loadState(settingsIn: CaosModuleSettings) {
        this.settings = settingsIn
    }

    override var ignoredCatalogueTags: List<String>
        get() = getState().ignoredCatalogueTags
        set(ignoredTags) {
            val state = getState()
            this.loadState(
                state.copy(
                    ignoredCatalogueTags = ignoredTags.distinct()
                )
            )
        }

    companion object {
        @JvmStatic
        fun getInstance(module: Module): CaosModuleSettingsComponent? {
            return ModuleServiceManager.getService(module, CaosModuleSettingsComponent::class.java)
        }
    }
}


data class CaosModuleSettings(
    @Attribute("com.badahori.creatures.caos.variant", converter = CaosVariantConverter::class)
    val variant: CaosVariant? = null,
    @Attribute("com.badahori.creatures.caos.ignored-files", converter = StringListConverter::class)
    val ignoredFiles: List<String> = mutableListOf(),
    val lastGameInterfaceName: String? = null,
    @Attribute(converter = StringListConverter::class)
    val ignoredCatalogueTags: List<String> = emptyList()
) {
    companion object {
    }
}
