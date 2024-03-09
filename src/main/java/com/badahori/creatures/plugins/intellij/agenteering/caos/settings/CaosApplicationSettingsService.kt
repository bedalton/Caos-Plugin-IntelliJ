@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.utils.ApplicationSettingsConverter
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
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
    storages = [Storage("CAOS_v2.xml")],
)
class CaosApplicationSettingsService :
    PersistentStateComponent<CaosApplicationSettingsService.CaosApplicationSettings>,
    HasIgnoredCatalogueTags {

    @Serializable
    data class CaosApplicationSettings(
        @Attribute
        val isAutoPoseEnabled: Boolean = false,
        @Attribute
        val combineAttNodes: Boolean = false,
        @Attribute
        val combineAttNodesBySlot: Boolean = false,
        @Attribute
        val replicateAttsToDuplicateSprites: Boolean? = null,
        @Attribute
        val ignoredCatalogueTags: List<String> = emptyList(),
        @Attribute
        val ignoredCharacterEscapes: Set<Char> = emptySet(),
        @Attribute
        val noSpellcheckCommands: Set<String> = emptySet()
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
    }


    var combineAttNodes: Boolean
        get() = myState.combineAttNodes
        set(value) {
            if (value == myState.combineAttNodes) {
                return
            }
            loadState(
                myState.copy(
                    combineAttNodes = value
                )
            )
        }

    var combineAttNodesBySlot: Boolean
        get() = myState.combineAttNodesBySlot
        set(value) {
            if (value == myState.combineAttNodesBySlot) {
                return
            }
            loadState(
                myState.copy(
                    combineAttNodesBySlot = value
                )
            )
        }

    var replicateAttsToDuplicateSprites: Boolean?
        get() = myState.replicateAttsToDuplicateSprites
        set(value) {
            if (value == myState.replicateAttsToDuplicateSprites) {
                return
            }
            loadState(
                myState.copy(
                    replicateAttsToDuplicateSprites = value != false
                )
            )
        }


    var isAutoPoseEnabled: Boolean
        get() = myState.isAutoPoseEnabled
        set(value) {
            if (myState.isAutoPoseEnabled == value) {
                return
            }
            loadState(myState.copy(isAutoPoseEnabled = value))
        }

    override var ignoredCatalogueTags: List<String>
        get() = myState.ignoredCatalogueTags
        set(ignoredTags) {
            val distinct = ignoredTags.distinct()
            if (myState.ignoredCatalogueTags.containsAll(distinct) && distinct.containsAll(myState.ignoredCatalogueTags)) {
                return
            }
            loadState(
                myState.copy(
                    ignoredCatalogueTags = ignoredTags.distinct()
                )
            )
        }

    var ignoredCharacterEscapes: Set<Char>
        get() = myState.ignoredCharacterEscapes
        set(value) {
            if (value == myState.ignoredCharacterEscapes) {
                return
            }
            loadState(
                myState.copy(
                    ignoredCharacterEscapes = value
                )
            )
        }

    var noSpellcheckCommands: Set<String>
        get() = myState.noSpellcheckCommands
        set(value) {
            if (value == myState.noSpellcheckCommands) {
                return
            }
            loadState(
                myState.copy(
                    noSpellcheckCommands = noSpellcheckCommands
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

        private val TOPIC = Topic.create(
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

}


interface CaosApplicationSettingsChangeListener : EventListener {
    fun onChange(oldState: CaosApplicationSettingsService.CaosApplicationSettings?, newState: CaosApplicationSettingsService.CaosApplicationSettings)
}

