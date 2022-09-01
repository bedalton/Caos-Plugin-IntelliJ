package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project

/**
 * Service for fetching CAOS project settings
 */
interface CaosProjectSettingsService: PersistentStateComponent<CaosProjectSettingsComponent.State>,
        HasGameInterfaces
{
    override fun getState(): CaosProjectSettingsComponent.State
    override fun loadState(state: CaosProjectSettingsComponent.State)

    companion object {
        /**
         * Extension method to get settings from a project
         */
        @JvmStatic
        fun getInstance(project: Project): CaosProjectSettingsService  {
            return ServiceManager.getService(project, CaosProjectSettingsService::class.java)
        }

    }
}
