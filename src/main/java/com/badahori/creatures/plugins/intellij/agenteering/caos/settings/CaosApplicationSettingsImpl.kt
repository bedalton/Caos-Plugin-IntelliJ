@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosApplicationSettingsImpl.CaosApplicationSettingsState
import com.badahori.creatures.plugins.intellij.agenteering.injector.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.injector.NativeInjectorInterface
import com.badahori.creatures.plugins.intellij.agenteering.utils.GameInterfaceListConverter
import com.badahori.creatures.plugins.intellij.agenteering.utils.StringListConverter
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
    name = "com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosApplicationSettingsComponent",
    storages = [Storage(value = "CAOS.xml")]
)
@Storage("CAOS.xml")
class CaosApplicationSettingsImpl : CaosApplicationSettingsService,
    PersistentStateComponent<CaosApplicationSettingsState>, Disposable {

    data class CaosApplicationSettingsState(
        @Attribute(converter = GameInterfaceListConverter::class)
        val gameInterfaceNames: List<GameInterfaceName> = listOf(),
        val isAutoPoseEnabled: Boolean = false,
        val lastWineDirectory: String? = null,
        val combineAttNodes: Boolean = false,
        val replicateAttsToDuplicateSprites: Boolean? = null,
        val winePath: String? = null,
        val wine32Path: String? = null,
        val wine64Path: String? = null,
        @Attribute(converter = StringListConverter::class)
        val ignoredCatalogueTags: List<String> = emptyList()
    )

    private var mState: CaosApplicationSettingsState = CaosApplicationSettingsState()

    private var loading = false

    override fun getState(): CaosApplicationSettingsState {
        return mState
    }

    override fun loadState(state: CaosApplicationSettingsState) {
        if (loading) {
            return
        }
        // Prevent accessors from calling loadState
        loading = true

        // Copy old state for use in listeners
        val oldState = mState

        // Ensure we do not serialize default game interface names
        val actualState = state.copy(gameInterfaceNames = state.gameInterfaceNames
            .filter { it != null && (it !is NativeInjectorInterface || !it.isDefault) })

        // Only load state if changed
        if (state == oldState) {
            // allow calling loadState again
            loading = false
            return
        }

        loading = true

        // Copy state to this state object
//        XmlSerializerUtil.copyBean(actualState, this)

        // Notify listeners of update
        onUpdate(oldState, actualState)
        mState = actualState

        // Clear loading flag to allow calling loadState
        loading = false
    }


    override var combineAttNodes: Boolean
        get() = state.combineAttNodes
        set(value) {
            if (value == state.combineAttNodes) {
                return
            }
            loadState(
                mState.copy(
                    combineAttNodes = value
                )
            )
        }

    override var replicateAttsToDuplicateSprites: Boolean?
        get() = mState.replicateAttsToDuplicateSprites
        set(value) {
            if (value == mState.replicateAttsToDuplicateSprites) {
                return
            }
            loadState(mState.copy(
                replicateAttsToDuplicateSprites =  value != false
            ))
        }


    override var isAutoPoseEnabled: Boolean
        get() = state.isAutoPoseEnabled
        set(value) {
            if (mState.isAutoPoseEnabled == value) {
                return
            }
            loadState(mState.copy(isAutoPoseEnabled = value))
        }

    override var ignoredCatalogueTags: List<String>
        get() = mState.ignoredCatalogueTags
        set(ignoredTags) {
            val distinct = ignoredTags.distinct()
            if (mState.ignoredCatalogueTags.containsAll(distinct) && distinct.containsAll(mState.ignoredCatalogueTags)) {
                return
            }
            loadState(
                mState.copy(
                    ignoredCatalogueTags = ignoredTags.distinct()
                )
            )
        }

    override var lastWineDirectory: String?
        get() = mState.lastWineDirectory
        set(value) {
            if (mState.lastWineDirectory == value) {
                return
            }
            loadState(
                mState.copy(
                    lastWineDirectory = value
                )
            )
        }

    override var gameInterfaceNames: List<GameInterfaceName>
        get() = mState.gameInterfaceNames
        set(value) {
            loadState(
                mState.copy(
                    gameInterfaceNames = value
                )
            )
        }

    override var winePath: String?
        get() = mState.winePath ?: wine32Path ?: wine64Path
        set(value) {
            loadState(
                mState.copy(
                    winePath = value,
                    wine64Path = mState.wine64Path ?: value,
                    wine32Path = mState.wine32Path ?: value
                )
            )
        }

    override var wine32Path: String?
        get() = mState.wine32Path
        set(value) {
            if (mState.wine32Path == value) {
                return
            }
            loadState(
                mState.copy(
                    wine32Path = value
                )
            )
        }

    override var wine64Path: String?
        get() = mState.wine64Path
        set(value) {
            if (wine64Path == value) {
                return
            }
            loadState(
                mState.copy(
                    wine64Path = value
                )
            )
        }

    private fun onUpdate(oldState: CaosApplicationSettingsState, newState: CaosApplicationSettingsState) {
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
        fun addSettingsChangedListener(listener: (oldState: CaosApplicationSettingsState, newState: CaosApplicationSettingsState) -> Unit) {
            addSettingsChangedListener(object : CaosApplicationSettingsChangeListener {
                override fun onChange(oldState: CaosApplicationSettingsState, newState: CaosApplicationSettingsState) {
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
            listener: (oldState: CaosApplicationSettingsState, newState: CaosApplicationSettingsState) -> Unit
        ) {
            try {
                addSettingsChangedListener(disposable, object : CaosApplicationSettingsChangeListener {
                    override fun onChange(
                        oldState: CaosApplicationSettingsState,
                        newState: CaosApplicationSettingsState
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

    override fun dispose() {
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CaosApplicationSettingsImpl

        return mState == other.mState
    }

    override fun hashCode(): Int {
        return mState.hashCode()
    }
}


interface CaosApplicationSettingsChangeListener : EventListener {
    fun onChange(oldState: CaosApplicationSettingsState, newState: CaosApplicationSettingsState)
}

