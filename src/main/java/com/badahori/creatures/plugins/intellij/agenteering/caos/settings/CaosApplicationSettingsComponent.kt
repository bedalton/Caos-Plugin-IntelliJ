@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.injector.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.injector.NativeInjectorInterface
import com.badahori.creatures.plugins.intellij.agenteering.utils.GameInterfaceListConverter
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.annotations.Attribute
import java.util.*
import com.intellij.openapi.components.Storage



/**
 * State container responsible for getting/setting project state
 */
@com.intellij.openapi.components.State(
    name = "CaosApplicationSettingsComponent",
    storages = [ Storage(value = "CAOS.xml") ]
)
class CaosApplicationSettingsComponent : CaosApplicationSettingsService,
    PersistentStateComponent<CaosApplicationSettingsComponent.State> {

    private var state: State = State()

    override fun getState(): State {
        return this.state
    }

    override fun loadState(state: State) {
        val oldState = this.state
        this.state = state.copy(
            // Ensure we do not serialize default game interface names
            gameInterfaceNames = state.gameInterfaceNames
                .filter { it !is NativeInjectorInterface || !it.isDefault }
        )
        onUpdate(oldState, state)
    }


    override var combineAttNodes: Boolean
        get() = state.combineAttNodes
        set(value) {
            if (value == state.combineAttNodes) {
                return
            }
            loadState(state.copy(
                combineAttNodes = value
            ))
        }

    override var replicateAttsToDuplicateSprites: Boolean?
        get() = state.replicateAttToDuplicateSprite
        set(value) {
            if (value == state.replicateAttToDuplicateSprite) {
                return
            }
            loadState(state.copy(
                replicateAttToDuplicateSprite = value != false
            ))
        }


    override var isAutoPoseEnabled: Boolean
        get() = state.isAutoPoseEnabled
        set(value) {
            loadState(state.copy(
               isAutoPoseEnabled = value
            ))
        }

    override var lastWineDirectory: String?
        get() = state.lastWineDirectory
        set(value) {
            if (value != state.lastWineDirectory) {
                loadState(state.copy(
                    lastWineDirectory = value
                ))
            }
        }

    /**
     * Project state object used to store various properties at the project level
     */
    data class State(
        @Attribute(converter = GameInterfaceListConverter::class)
        val gameInterfaceNames: List<GameInterfaceName> = listOf<GameInterfaceName>(),
        val isAutoPoseEnabled: Boolean = false,
        val lastWineDirectory: String? = null,
        val combineAttNodes: Boolean = false,
        val replicateAttToDuplicateSprite: Boolean? = null
    )

    private fun onUpdate(oldState: State, newState: State) {
        ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC).onChange(oldState, newState)
    }

    companion object {
        val TOPIC = Topic.create(
            "CAOSApplicationSettingsChangedListener",
            CaosApplicationSettingsChangeListener::class.java
        )

        /**
         * Adds a listener to track and change to the settings component
         */
        @Suppress("unused")
        fun addSettingsChangedListener(listener: (oldState: State, newState: State) -> Unit) {
            addSettingsChangedListener(object : CaosApplicationSettingsChangeListener {
                override fun onChange(oldState: State, newState: State) {
                    listener(oldState, newState)
                }
            })
        }

        /**
         * Adds a listener to track and change to the settings component
         * Listener should be released automatically when parent is disposed
         */
        fun addSettingsChangedListener(disposable: Disposable, listener: (oldState: State, newState: State) -> Unit) {
            try {
                addSettingsChangedListener(disposable, object : CaosApplicationSettingsChangeListener {
                    override fun onChange(oldState: State, newState: State) {
                        listener(oldState, newState)
                    }
                })
            } catch (ignored: Exception) {}
        }

        /**
         * Adds a project settings change listener called whenever the settings are changed
         */
        @JvmStatic
        @Suppress("MemberVisibilityCanBePrivate")
        fun addSettingsChangedListener(listener: CaosApplicationSettingsChangeListener) {
            ApplicationManager.getApplication().messageBus.connect().subscribe(TOPIC, listener)
        }

        /**
         * Adds a project settings change listener tied to a disposable component
         * Listener should be released automatically when parent is disposed
         */
        @JvmStatic
        @Suppress("MemberVisibilityCanBePrivate")
        fun addSettingsChangedListener(disposable: Disposable, listener: CaosApplicationSettingsChangeListener) {
            try {
                ApplicationManager.getApplication().messageBus.connect(disposable).subscribe(TOPIC, listener)
            } catch (ignored: Exception) {

            }
        }
    }
}


interface CaosApplicationSettingsChangeListener : EventListener {
    fun onChange(oldState: CaosApplicationSettingsComponent.State, newState: CaosApplicationSettingsComponent.State)
}

