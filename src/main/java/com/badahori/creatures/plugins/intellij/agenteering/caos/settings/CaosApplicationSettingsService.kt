package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent

/**
 * Service for fetching CAOS project settings
 */
interface CaosApplicationSettingsService:
    PersistentStateComponent<CaosApplicationSettingsComponent.State>,
    HasIgnoredCatalogueTags,
        Disposable
{
    var isAutoPoseEnabled: Boolean
    var lastWineDirectory: String?
    var combineAttNodes: Boolean
    var replicateAttsToDuplicateSprites: Boolean?
    override var ignoredCatalogueTags: List<String>

    override fun getState(): CaosApplicationSettingsComponent.State
    override fun loadState(state: CaosApplicationSettingsComponent.State)


    companion object {
        /**
         * Extension method to get settings from a project
         */
        @JvmStatic
        fun getInstance(): CaosApplicationSettingsService  {
            return ApplicationManager.getApplication().getService(CaosApplicationSettingsService::class.java)
        }

    }
}
