package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent

/**
 * Service for fetching CAOS project settings
 */
interface CaosApplicationSettingsService: PersistentStateComponent<CaosApplicationSettingsComponent.State> {
    var isAutoPoseEnabled: Boolean
    var lastWineDirectory: String?
    var combineAttNodes: Boolean
    var replicateAttsToDuplicateSprites: Boolean?

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
