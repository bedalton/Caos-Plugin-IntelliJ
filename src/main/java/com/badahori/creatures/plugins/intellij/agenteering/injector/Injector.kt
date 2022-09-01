package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.caos.action.GameInterfaceName
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.GameInterfaceType
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.GameInterfaceType.*
import com.badahori.creatures.plugins.intellij.agenteering.caos.action.JectScriptType
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.nullIfUnknown
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.api.CaosScriptScriptElement
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.settings
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.useJectByDefault
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.invokeLater
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

/**
 * Class responsible for Injecting CAOS into the various Creatures games.
 */
object Injector {

    internal const val REMOVAL_SCRIPT_FLAG = 1
    internal const val EVENT_SCRIPT_FLAG = 2
    internal const val INSTALL_SCRIPT_FLAG = 4

    /**
     * Gets the actual version of a C1e game.
     * Perhaps too heavy-handed, but trying to assert that the correct game is connected
     * Sends a DDE version check request before each user CAOS injection
     */
    private suspend fun getActualVersion(
        project: Project,
        fallbackVariant: CaosVariant,
        gameInterfaceName: GameInterfaceName,
    ): CaosVariant? {
        if (fallbackVariant.isNotOld) {
            return fallbackVariant
        }

        val rawCode = "dde: putv vrsn"
        val response = injectPrivate(project, fallbackVariant, gameInterfaceName) { connection ->
            connection.inject(rawCode)
        }
        if (response !is InjectionStatus.Ok)
            return fallbackVariant
        return try {
            if (response.response.toInt() < 6) {
                CaosVariant.C1
            } else {
                CaosVariant.C2
            }
        } catch (e: Exception) {
            fallbackVariant
        }
    }

    /**
     * Checks version info before injection
     */
    internal suspend fun inject(
        project: Project,
        fallbackVariant: CaosVariant,
        gameInterfaceName: GameInterfaceName,
        caosFile: CaosScriptFile,
        jectFlags: Int,
        tryJect: Boolean = project.settings.useJectByDefault,
    ): Boolean {
        val variant = gameInterfaceName.variant ?: fallbackVariant
        if (!isValidVariant(project, variant, gameInterfaceName))
            return false
        val response = injectPrivate(project, fallbackVariant, gameInterfaceName) { connection ->
            try {
                FileInjectorUtil.inject(
                    project,
                    connection,
                    caosFile,
                    jectFlags,
                    useJect = connection.supportsJect && tryJect
                )
            } catch (e: CaosInjectorExceptionWithStatus) {
                e.injectionStatus
            } catch (e: Exception) {
                InjectionStatus.Bad("Injection failed with plugin based error: ${e.message}")
                null
            }
        }
        onCaosResponse(project, response)
        return response is InjectionStatus.Ok
    }

    /**
     * Checks version info before injection
     */
    internal suspend fun inject(
        project: Project,
        fallbackVariant: CaosVariant,
        gameInterfaceName: GameInterfaceName,
        scripts: Map<JectScriptType, List<CaosScriptScriptElement>>,
    ): Boolean {
        val variant = gameInterfaceName.variant ?: fallbackVariant
        if (!isValidVariant(project, variant, gameInterfaceName))
            return false
        val response = injectPrivate(project, fallbackVariant, gameInterfaceName) { connection ->
            try {
                FileInjectorUtil.inject(
                    project,
                    connection,
                    scripts
                )
            } catch (e: CaosInjectorExceptionWithStatus) {
                e.injectionStatus
            } catch (e: Exception) {
                InjectionStatus.Bad("Injection failed with plugin based error: ${e.message}")
                null
            }
        }
        onCaosResponse(project, response)
        return response is InjectionStatus.Ok
    }

    /**
     * Ensures that variant is supported, and if C1e, that the correct game is running
     */
    private suspend fun isValidVariant(
        project: Project,
        variant: CaosVariant,
        gameInterfaceName: GameInterfaceName,
    ): Boolean {
        if (!canConnectToVariant(variant)) {
            val error = "Injection to ${variant.fullName} is not yet implemented"
            invokeLater {
                CaosInjectorNotifications.show(project, "ConnectionException", error, NotificationType.ERROR)
            }
            return false
        }
        if (variant.isOld) {
            val actualVersion = getActualVersion(project, variant, gameInterfaceName)
            if (actualVersion != null && actualVersion != variant) {
                postError(
                    project,
                    "Connection Error",
                    "Grammar set to variant [${variant}], but ide is connected to ${actualVersion.fullName}"
                )
                return false
            }
        }
        return true
    }


    /**
     * Responsible for actually injecting the CAOS code.
     */
    private suspend fun injectPrivate(
        project: Project,
        fallbackVariant: CaosVariant,
        gameInterfaceName: GameInterfaceName,
        run: suspend (connection: CaosConnection) -> InjectionStatus?,
    ): InjectionStatus? {
        val variant = gameInterfaceName.variant?.nullIfUnknown()
            ?: fallbackVariant.nullIfUnknown()
            ?: return InjectionStatus.BadConnection("Variant is undefined in injector")
        val connection = connection(variant, gameInterfaceName)
            ?: return InjectionStatus.BadConnection("Failed to initiate CAOS connection. Ensure ${variant.fullName} is running and try again")

        if (!creditsCalled[variant].orFalse()) {
            creditsCalled[variant] = true
            connection.showAttribution(project, variant)
        }

        if (!connection.isConnected() && !connection.connect(false)) {
            return InjectionStatus.BadConnection("Failed to connect to ${gameInterfaceName.name}")
        }
        return run(connection)
    }

    /**
     * Creates the actual connection to the game
     * If connection fails or is unsupported, returns null
     */
    private fun connection(variant: CaosVariant, gameInterfaceName: GameInterfaceName): CaosConnection? {
        val conn = getConnectionObject(variant, gameInterfaceName)
        if (!conn.connect()) {
            return null
        }
        return conn
    }

    @Suppress("unused")
    fun canJect(variant: CaosVariant, gameInterfaceName: GameInterfaceName): Boolean {
        // TODO support C2e if it supports JECT in all variants
        if (!variant.isNotOld)
            return false
        return try {
            getConnectionObject(variant, gameInterfaceName).supportsJect
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gets the raw connection object without testing connection or actually connecting
     */
    private fun getConnectionObject(variantIn: CaosVariant, gameInterfaceName: GameInterfaceName): CaosConnection {
        val variant = gameInterfaceName.variant
            ?: variantIn
        return when (gameInterfaceName.type) {

            // A simple POST connection which sends CAOS in the body
            HTTP_POST -> PostConnection(gameInterfaceName.path, variant)

            // LisDude's TCP Injection
            HTTP_SOCKET -> TcpConnection(gameInterfaceName.interfaceName, gameInterfaceName.path)

            // Windows native injection
            NATIVE -> if (variant.isOld) {
                DDEConnection(
                    gameInterfaceName.path,
                    variant,
                    gameInterfaceName.name
                )
            } else {
                C3Connection(
                    gameInterfaceName.name,
                    gameInterfaceName.path
                )
            }

            // Sends CAOS through an EXE added to a wine prefix
            WINE -> WineConnection(
                variant,
                gameInterfaceName.path
            )

            // Creates a connection from an old definition
            DEPRECATED -> getConnectionFromOldDefinition(
                variant,
                gameInterfaceName
            )
        }
    }

    private fun getConnectionFromOldDefinition(
        variant: CaosVariant,
        gameInterfaceName: GameInterfaceName,
    ): CaosConnection {

        val injectUrl = gameInterfaceName.path.let {
            if (it.startsWith("http")) {
                it
            } else {
                null
            }
        }

        if (injectUrl != null) {
            return PostConnection(injectUrl, variant)
        }

        var gameUrl = gameInterfaceName.url

        if (variant.isOld) {
            if (!gameUrl.startsWith("dde:")) {
                gameUrl = "dde:$gameUrl"
            }
        }
        if (gameUrl.startsWith("dde:")) {
            gameUrl = gameUrl.substring(4).trim()
            return DDEConnection(gameUrl, variant, gameInterfaceName.name)
        }

        LOGGER.info("Creating C3 Injector connection")
        return C3Connection("@${gameInterfaceName.url}")
    }

    /**
     * Checks if it is possible to connect to variant
     */
    fun canConnectToVariant(variant: CaosVariant): Boolean {
        return when (variant) {
            CaosVariant.C1 -> true
            CaosVariant.C2 -> true
            CaosVariant.CV -> true
            CaosVariant.C3 -> true
            CaosVariant.DS -> true
            CaosVariant.SM -> true
            else -> false
        }
    }


    /**
     * Holds whether the credits for the Connection were called yet this run of the IDE
     * All code to inject CAOS was written by other people. I need to credit them
     */
    private val creditsCalled = mutableMapOf(
        CaosVariant.C1 to false,
        CaosVariant.C2 to false,
        CaosVariant.CV to false,
        CaosVariant.C3 to false,
        CaosVariant.DS to false,
        CaosVariant.SM to false
    )

}

/**
 * Defines a CAOS connection
 * Plugin supports multiple game connections such as DDE (C1e), MemoryMapped(C2e), HTTP(Any)
 */
internal interface CaosConnection {
    val supportsJect: Boolean
    fun inject(caos: String): InjectionStatus
    fun injectWithJect(caos: CaosScriptFile, flags: Int): InjectionStatus
    fun injectEventScript(family: Int, genus: Int, species: Int, eventNumber: Int, caos: String): InjectionStatus
    fun disconnect(): Boolean
    fun isConnected(): Boolean
    fun connect(silent: Boolean = false): Boolean
    fun showAttribution(project: Project, variant: CaosVariant)
}

/**
 * The status for an injection request
 */
internal sealed class InjectionStatus {
    data class Ok(val response: String) : InjectionStatus()
    data class Bad(val error: String) : InjectionStatus()
    data class BadConnection(val error: String) : InjectionStatus()
}