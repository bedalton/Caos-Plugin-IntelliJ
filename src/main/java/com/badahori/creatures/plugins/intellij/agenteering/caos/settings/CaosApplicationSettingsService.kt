@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosApplicationSettingsService.CaosApplicationSettings
import com.badahori.creatures.plugins.intellij.agenteering.utils.ApplicationSettingsConverter
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.StringListConverter
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.annotations.Attribute
import kotlinx.serialization.Serializable
import java.util.*


/**
 * State container responsible for getting/setting project state
 */
@com.intellij.openapi.components.State(
    name = "caos-application-settings",
    storages = [Storage(
        value = "CAOS.xml",
        roamingType = RoamingType.DEFAULT
    )],
)
class CaosApplicationSettingsService :
    PersistentStateComponent<CaosApplicationSettings>,
    HasIgnoredCatalogueTags {

    @Serializable
    data class CaosApplicationSettings(
        val isAutoPoseEnabled: Boolean = false,
        val combineAttNodes: Boolean = false,
        val replicateAttsToDuplicateSprites: Boolean? = null,
        @Attribute(converter = StringListConverter::class)
        val ignoredCatalogueTags: List<String> = emptyList()
    )

    @Attribute(converter = ApplicationSettingsConverter::class)
    private var myState: CaosApplicationSettings = CaosApplicationSettings()

    override fun getState(): CaosApplicationSettings {
        return myState
    }

    override fun loadState(state: CaosApplicationSettings) {

        // Copy old state for use in listeners
        val oldState = myState

        myState = state

        // Only load state if changed
        if (state != oldState) {
            // Notify listeners of update
            onUpdate(oldState, state)
        }

        LOGGER.info("Applying application settings; $myState")
    }


    var combineAttNodes: Boolean
        get() = getState().combineAttNodes
        set(value) {
            if (value == getState().combineAttNodes) {
                return
            }
            loadState(
                getState().copy(
                    combineAttNodes = value
                )
            )
        }

    var replicateAttsToDuplicateSprites: Boolean?
        get() = getState().replicateAttsToDuplicateSprites
        set(value) {
            if (value == getState().replicateAttsToDuplicateSprites) {
                return
            }
            loadState(
                getState().copy(
                    replicateAttsToDuplicateSprites = value != false
                )
            )
        }


    var isAutoPoseEnabled: Boolean
        get() = getState().isAutoPoseEnabled
        set(value) {
            if (getState().isAutoPoseEnabled == value) {
                return
            }
            loadState(getState().copy(isAutoPoseEnabled = value))
        }

    override var ignoredCatalogueTags: List<String>
        get() = getState().ignoredCatalogueTags
        set(ignoredTags) {
            val distinct = ignoredTags.distinct()
            if (getState().ignoredCatalogueTags.containsAll(distinct) && distinct.containsAll(getState().ignoredCatalogueTags)) {
                return
            }
            loadState(
                getState().copy(
                    ignoredCatalogueTags = ignoredTags.distinct()
                )
            )
        }

    companion object {

        /**
         * Extension method to get settings from a project
         */
        @JvmStatic
        fun getInstance(): CaosApplicationSettingsService {
            return ApplicationManager.getApplication().getService(CaosApplicationSettingsService::class.java)
        }

        private fun onUpdate(oldState: CaosApplicationSettings?, newState: CaosApplicationSettings) {
            if (ApplicationManager.getApplication().isDisposed) {
                return
            }
            ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC).onChange(oldState, newState)
        }

        val TOPIC = Topic.create(
            "CAOSApplicationSettingsChangedListener",
            CaosApplicationSettingsChangeListener::class.java
        )

        /**
         * Adds a listener to track and change to the settings component
         */
        @Suppress("unused")
        fun addSettingsChangedListener(listener: (oldState: CaosApplicationSettings?, newState: CaosApplicationSettings) -> Unit) {
            addSettingsChangedListener(object : CaosApplicationSettingsChangeListener {
                override fun onChange(oldState: CaosApplicationSettings?, newState: CaosApplicationSettings) {
                    listener(oldState, newState)
                }
            })
        }

        /**
         * Adds a listener to track and change to the settings component
         * Listener should be released automatically when parent is disposed
         */
        fun addSettingsChangedListener(
            disposable: Disposable,
            listener: (oldState: CaosApplicationSettings?, newState: CaosApplicationSettings) -> Unit
        ) {
            try {
                addSettingsChangedListener(disposable, object : CaosApplicationSettingsChangeListener {
                    override fun onChange(
                        oldState: CaosApplicationSettings?,
                        newState: CaosApplicationSettings
                    ) {
                        listener(oldState, newState)
                    }
                })
            } catch (ignored: Exception) {
            }
        }

        /**
         * Adds a project settings change listener called whenever the settings are changed
         */
        @JvmStatic
        @Suppress("MemberVisibilityCanBePrivate")
        fun addSettingsChangedListener(listener: CaosApplicationSettingsChangeListener) {
            if (ApplicationManager.getApplication().isDisposed) {
                return
            }
            ApplicationManager.getApplication().messageBus.connect().subscribe(TOPIC, listener)
        }

        /**
         * Adds a project settings change listener tied to a disposable component
         * Listener should be released automatically when parent is disposed
         */
        @JvmStatic
        @Suppress("MemberVisibilityCanBePrivate")
        fun addSettingsChangedListener(disposable: Disposable, listener: CaosApplicationSettingsChangeListener) {
            if (ApplicationManager.getApplication().isDisposed) {
                return
            }
            try {
                ApplicationManager.getApplication().messageBus.connect(disposable).subscribe(TOPIC, listener)
            } catch (ignored: Exception) {

            }
        }

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CaosApplicationSettingsService

        return myState == other.myState
    }

    override fun hashCode(): Int {
        return myState?.hashCode() ?: 0
    }

}


interface CaosApplicationSettingsChangeListener : EventListener {
    fun onChange(oldState: CaosApplicationSettings?, newState: CaosApplicationSettings)
}

