@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.caos.settings

import com.badahori.creatures.plugins.intellij.agenteering.injector.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.injector.NativeInjectorInterface
import com.badahori.creatures.plugins.intellij.agenteering.utils.GameInterfaceListConverter
import com.badahori.creatures.plugins.intellij.agenteering.utils.StringListConverter
import com.bedalton.log.Log
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.annotations.Attribute
import java.util.*
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil


/**
 * State container responsible for getting/setting project state
 */
@com.intellij.openapi.components.State(
    name = "com.badahori.creatures.plugins.intellij.agenteering.caos.settings.CaosApplicationSettingsComponent",
    storages = [Storage(value = "CAOS.xml")]
)
@Storage("CAOS.xml")
class CaosApplicationSettingsState : CaosApplicationSettingsService,
    PersistentStateComponent<CaosApplicationSettingsState>, Disposable {

    @Attribute(converter = GameInterfaceListConverter::class)
    private var mGameInterfaceNames: List<GameInterfaceName> = listOf()
    private var mIsAutoPoseEnabled: Boolean = false
    private var mLastWineDirectory: String? = null
    private var mCombineAttNodes: Boolean = false
    private var mReplicateAttToDuplicateSprite: Boolean? = null

    @Attribute(converter = StringListConverter::class)
    private var mIgnoredCatalogueTags: List<String> = emptyList()

    private var loading = false

    override fun getState(): CaosApplicationSettingsState {
        return this
    }

    fun copy(work: CaosApplicationSettingsState.() -> Unit): CaosApplicationSettingsState {
        val oldState = copy()
        return oldState.apply(work)
    }

    fun copy(): CaosApplicationSettingsState {
        val oldState = CaosApplicationSettingsState().apply { loading = true }
        XmlSerializerUtil.copyBean(this, oldState)
        return oldState
    }

    override fun loadState(state: CaosApplicationSettingsState) {
        if (loading) {
            return
        }
        // Prevent accessors from calling loadState
        loading = true

        // Copy old state for use in listeners
        val oldState = copy()

        // Ensure we do not serialize default game interface names
        mGameInterfaceNames = state.gameInterfaceNames
            .filter { it != null && (it !is NativeInjectorInterface || !it.isDefault) }

        // Only load state if changed
        if (state == oldState) {
            // allow calling loadState again
            loading = false
            return
        }

        loading = true

        // Copy state to this state object
        XmlSerializerUtil.copyBean(state, this)

        // Notify listeners of update
        onUpdate(oldState, state)

        // Clear loading flag to allow calling loadState
        loading = false
    }


    override var combineAttNodes: Boolean
        get() = state.mCombineAttNodes
        set(value) {
            if (value == state.mCombineAttNodes) {
                return
            }
            mCombineAttNodes = value
            loadState(this)
        }

    override var replicateAttsToDuplicateSprites: Boolean?
        get() = mReplicateAttToDuplicateSprite
        set(value) {
            if (value == mReplicateAttToDuplicateSprite) {
                return
            }
            mReplicateAttToDuplicateSprite = value != false
            loadState(this)
        }


    override var isAutoPoseEnabled: Boolean
        get() = state.mIsAutoPoseEnabled
        set(value) {
            if (mIsAutoPoseEnabled == value) {
                return
            }
            mIsAutoPoseEnabled = value
            loadState(this)
        }

    override var ignoredCatalogueTags: List<String>
        get() = state.mIgnoredCatalogueTags
        set(ignoredTags) {
            val distinct = ignoredTags.distinct()
            if (mIgnoredCatalogueTags.containsAll(distinct) && distinct.containsAll(mIgnoredCatalogueTags)) {
                return
            }
            mIgnoredCatalogueTags = ignoredTags.distinct()
            loadState(this)
        }

    override var lastWineDirectory: String?
        get() = state.mLastWineDirectory
        set(value) {
            if (mLastWineDirectory == value) {
                return
            }
            mLastWineDirectory = value
            loadState(this)
        }

    override var gameInterfaceNames: List<GameInterfaceName>
        get() = mGameInterfaceNames.filterNotNull()
        set(value) {
            mGameInterfaceNames = value.filterNotNull()
            loadState(this)
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
        fun addSettingsChangedListener(disposable: Disposable, listener: (oldState: CaosApplicationSettingsState, newState: CaosApplicationSettingsState) -> Unit) {
            try {
                addSettingsChangedListener(disposable, object : CaosApplicationSettingsChangeListener {
                    override fun onChange(oldState: CaosApplicationSettingsState, newState: CaosApplicationSettingsState) {
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

        other as CaosApplicationSettingsState

        if (mGameInterfaceNames != other.mGameInterfaceNames) return false
        if (mIsAutoPoseEnabled != other.mIsAutoPoseEnabled) return false
        if (mLastWineDirectory != other.mLastWineDirectory) return false
        if (mCombineAttNodes != other.mCombineAttNodes) return false
        if (mReplicateAttToDuplicateSprite != other.mReplicateAttToDuplicateSprite) return false
        if (mIgnoredCatalogueTags != other.mIgnoredCatalogueTags) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mGameInterfaceNames.hashCode()
        result = 31 * result + mIsAutoPoseEnabled.hashCode()
        result = 31 * result + (mLastWineDirectory?.hashCode() ?: 0)
        result = 31 * result + mCombineAttNodes.hashCode()
        result = 31 * result + (mReplicateAttToDuplicateSprite?.hashCode() ?: 0)
        result = 31 * result + mIgnoredCatalogueTags.hashCode()
        return result
    }
}


interface CaosApplicationSettingsChangeListener : EventListener {
    fun onChange(oldState: CaosApplicationSettingsState, newState: CaosApplicationSettingsState)
}

