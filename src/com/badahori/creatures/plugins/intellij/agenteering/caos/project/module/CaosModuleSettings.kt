package com.badahori.creatures.plugins.intellij.agenteering.caos.project.module

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.util.xml.ConvertContext
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.annotations.Attribute


@State(
        name = "CaosModuleSettings",
        storages = [Storage(file = StoragePathMacros.MODULE_FILE)]
)
class CaosModuleSettingsComponent : PersistentStateComponent<CaosModuleSettings> {

    private var settings = CaosModuleSettings()

    override fun getState(): CaosModuleSettings {
        return settings
    }

    override fun loadState(settingsIn: CaosModuleSettings) {
        this.settings = settingsIn
    }
}

data class CaosModuleSettings(
        @Attribute("com.badahori.creatures.caos.variant", converter = CaosVariantConverter::class)
        var variant: CaosVariant? = null
)

class CaosVariantConverter : Converter<CaosVariant>() {
    override fun toString(variant: CaosVariant): String? {
        return variant.code
    }

    override fun fromString(value: String): CaosVariant? {
        return CaosVariant.fromVal(value)
    }
}