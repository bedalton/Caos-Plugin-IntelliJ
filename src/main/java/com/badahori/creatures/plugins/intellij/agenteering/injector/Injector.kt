@file:Suppress("unused")

package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.caos.action.JectScriptType
import com.badahori.creatures.plugins.intellij.agenteering.caos.exceptions.messageOrNoneText
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosBundle.message
import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosScriptFile
import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.settings.settings
import com.badahori.creatures.plugins.intellij.agenteering.utils.orFalse
import com.bedalton.common.util.className
import com.bedalton.common.util.toListOf
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer

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

        val response =
            injectPrivateSafe(project, fallbackVariant, gameInterfaceName, "$$\$private$$$", false) { connection ->
                connection.inject(project, "$$\$private$$$", null, rawCode)
            } ?: return fallbackVariant

        if (response !is InjectionStatus.Ok) {
            return fallbackVariant
        }
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
        variant: CaosVariant,
        gameInterfaceName: GameInterfaceName,
        caosFile: CaosScriptFile,
        totalFiles: Int,
        jectFlags: Int,
        tryJect: Boolean = project.settings.useJectByDefault,
    ) {
        if (!isValidVariant(project, variant, gameInterfaceName))
            return
        val onResult = { response: InjectionStatus, _: String? ->
            invokeLater {
                onCaosResponse(project, response)
            }
        }
        val result = injectPrivate(project, variant, gameInterfaceName, caosFile.name) { connection ->
            try {
                FileInjectorUtil.inject(
                    project = project,
                    connection = connection,
                    caosFile = caosFile,
                    totalFiles = totalFiles,
                    flags = jectFlags,
                    useJect = connection.supportsJect && tryJect
                )
            } catch (e: CaosInjectorExceptionWithStatus) {
                e.injectionStatus ?: InjectionStatus.Bad(
                    caosFile.name,
                    null,
                    e.message ?: message("caos.errors.unknown-plugin-error")
                )
            } catch (e: Exception) {
                InjectionStatus.Bad(
                    caosFile.name,
                    null,
                    message(
                        "caos.injector.errors.internal-plugin-error",
                        e.className + (e.message?.let { ": $it" } ?: "")))
            }
        }
        if (result is InjectionStatus.Pending) {
            result.setCallback(onResult)
        } else {
            onResult(result, null)
        }
    }

    /**
     * Checks version info before injection
     */
    internal suspend fun inject(
        project: Project,
        variant: CaosVariant,
        gameInterfaceName: GameInterfaceName,
        fileName: String,
        scripts: Map<JectScriptType, List<CaosScriptStruct>>,
    ) {
        if (!isValidVariant(project, variant, gameInterfaceName))
            return
        val onResult = { response: InjectionStatus, _: String? ->
            invokeLater {
                onCaosResponse(project, response)
            }
        }
        val response = injectPrivate(project, variant, gameInterfaceName, fileName) { connection ->
            try {
                FileInjectorUtil.injectScripts(
                    project,
                    variant,
                    connection,
                    "editor",
                    1,
                    scripts,
                )
            } catch (e: CaosInjectorExceptionWithStatus) {
                e.injectionStatus ?: InjectionStatus.Bad(
                    fileName,
                    null,
                    message("caos.injector.errors.internal-plugin-error", e.messageOrNoneText())
                )
            } catch (e: Exception) {
                InjectionStatus.Bad(
                    fileName,
                    null,
                    message(
                        "caos.injector.errors.internal-plugin-error",
                        e.className + (e.message?.let { ": $it" } ?: "")))
            }
        }
        if (response is InjectionStatus.Pending) {
            response.setCallback(onResult)
        } else {
            onCaosResponse(project, response)
        }
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
            val error = message("caos.injector.errors.game-not-supported", variant.fullName)
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
                    message(
                        "caos.injector.errors.grammar-connection-variant-mismatch",
                        variant.code,
                        actualVersion.fullName
                    ),
                    emptyList()
                )
                return false
            }
        }
        return true
    }


    private suspend fun injectPrivateSafe(
        project: Project,
        fallbackVariant: CaosVariant,
        gameInterfaceName: GameInterfaceName,
        fileName: String,
        returnExceptionAsInjectionStatusAndNotAsNullValue: Boolean,
        run: suspend (connection: CaosConnection) -> InjectionStatus,
    ): InjectionStatus? {
        return try {
            injectPrivate(project, fallbackVariant, gameInterfaceName, fileName, run)
        } catch (e: Exception) {
            if (returnExceptionAsInjectionStatusAndNotAsNullValue) {
                InjectionStatus.Bad(fileName, null, (e.message ?: "Plugin threw unhandled exception"))
            } else {
                null
            }
        }
    }

    /**
     * Responsible for actually injecting the CAOS code.
     */
    private suspend fun injectPrivate(
        project: Project,
        variant: CaosVariant,
        gameInterfaceName: GameInterfaceName,
        fileName: String,
        run: suspend (connection: CaosConnection) -> InjectionStatus,
    ): InjectionStatus {
        val connection = connection(variant, gameInterfaceName)
            ?: return InjectionStatus.BadConnection(
                fileName,
                null,
                message("caos.injector.errors.connect-failed-ensure-running", variant.fullName),
                variant
            )

        if (!creditsCalled[variant].orFalse()) {
            creditsCalled[variant] = true
            connection.showAttribution(project, variant)
        }

        if (!connection.isConnected() && !connection.connect(false)) {
            return InjectionStatus.BadConnection(
                fileName,
                null,
                message("caos.injector.errors.failed-to-connect", gameInterfaceName.name),
                variant
            )
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
    private fun getConnectionObject(theVariant: CaosVariant, gameInterfaceName: GameInterfaceName): CaosConnection {

        return when (gameInterfaceName) {
            is NativeInjectorInterface -> {
                getNativeConnection(theVariant, gameInterfaceName)
            }

            is WineInjectorInterface -> {
                WineConnection(theVariant, gameInterfaceName)
            }

            is PostInjectorInterface -> {
                PostConnection(theVariant, gameInterfaceName)
            }

            is TCPInjectorInterface -> {
                TCPConnection(theVariant, gameInterfaceName)
            }

            is CorruptInjectorInterface -> {
                throw CaosConnectionException(message("caos.injector.interface-data-invalid"))
            }
            is NoneInjectorInterface -> {
                throw CaosConnectionException(message("caos.injector.interface-data-invalid"))
            }
        }
    }

    private fun getNativeConnection(variant: CaosVariant, gameInterfaceName: GameInterfaceName): CaosConnection {
        return if (variant.isOld) {
            DDEConnection(variant, gameInterfaceName)
        } else {
            C3Connection(variant, gameInterfaceName)
        }
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

    // Connection variant
    val variant: CaosVariant

    /**
     * The maximum allowed length of a CAOS script for this injector
     */
    val maxCaosLength: Int

    // Whether this injector supports JECT based injecting from bootstrap
    val supportsJect: Boolean

    /**
     * Inject a raw CAOS macro
     */
    fun inject(project: Project, fileName: String, descriptor: String?, caos: String): InjectionStatus

    /**
     * Inject using JECT
     * This copies a file to C2e bootstrap, then calls `ject "{file}" {flags}`
     */
    fun injectWithJect(project: Project, caos: CaosScriptFile, flags: Int): InjectionStatus

    /**
     * Injects an event script into the games Scriptorium
     */
    fun injectEventScript(
        project: Project,
        fileName: String,
        family: Int,
        genus: Int,
        species: Int,
        eventNumber: Int,
        caos: String,
    ): InjectionStatus

    /**
     * Disconnect from this connection
     */
    fun disconnect(): Boolean

    /**
     * Test if is connected
     */
    fun isConnected(): Boolean

    /**
     * Connect to game
     */
    fun connect(silent: Boolean = false): Boolean

    /**
     * Show the attribution for the injector code used in this connection
     */
    fun showAttribution(project: Project, variant: CaosVariant)
}


/**
 * The status for an injection request
 */
@Suppress("MemberVisibilityCanBePrivate")
internal sealed class InjectionStatus(open val fileName: String?, open val descriptor: String?) {

    /**
     * Called when no errors occur, and when no detectable error message is returned
     */
    data class Ok(override val fileName: String, override val descriptor: String?, val response: String) :
        InjectionStatus(fileName, descriptor)

    /**
     * Called to aggregate status from multiple injections in a set
     */
    data class MultiResponse(
        private val results: List<InjectionStatus>,
    ) : InjectionStatus(null, null) {

        val allOK: Boolean by lazy {
            results.all { response -> response is Ok || (response as? MultiResponse)?.allOK == true }
        }

        val all: List<InjectionStatus> by lazy {
            results.flatMap { response ->
                when (response) {
                    is MultiResponse -> response.all
                    else -> response.toListOf()
                }
            }
        }
        val okResults: List<Ok> by lazy {
            results.flatMap { response ->
                when (response) {
                    is Ok -> response.toListOf()
                    is MultiResponse -> response.okResults
                    else -> emptyList()
                }
            }
        }

        val errors: List<Bad> by lazy {
            results.flatMap { response ->
                when (response) {
                    is Bad -> response.toListOf()
                    is MultiResponse -> response.errors
                    else -> emptyList()
                }
            }
        }

        val connectionErrors: List<BadConnection> by lazy {
            results.flatMap { response ->
                when (response) {
                    is BadConnection -> response.toListOf()
                    is MultiResponse -> response.connectionErrors
                    else -> emptyList()
                }
            }
        }
    }

    /**
     * Response from an injection failure, or when a known error format is encountered
     */
    data class Bad(
        override val fileName: String,
        override val descriptor: String?,
        val error: String,
        internal val positions: List<SmartPsiElementPointer<out PsiElement>>? = null,
    ) :
        InjectionStatus(fileName, descriptor)


    /**
     * Response when a connection failed to be established with game or injector
     */
    data class BadConnection(
        override val fileName: String,
        override val descriptor: String?,
        val error: String,
        private val variant: CaosVariant,
    ) :
        InjectionStatus(fileName, descriptor) {
        fun formattedError(game: String = "game"): String {
            return if (error.trim().lowercase().endsWith("connect timed out")) {
                message("caos.injector.errors.connect-failed-ensure-running", game)
            } else {
                error
            }
        }
    }

    /**
     * Response when an injection method was called which is not supported
     */
    data class ActionNotSupported(override val fileName: String?, override val descriptor: String?, val error: String) :
        InjectionStatus(fileName, descriptor)

    /**
     * Status marking an injection result as pending.
     * Will alert listeners when injection result returns
     */
    data class Pending(override val fileName: String?, override val descriptor: String?, val serial: String) :
        InjectionStatus(fileName, descriptor) {
        private var result: InjectionStatus? = null
        var onResult: ((result: InjectionStatus, serial: String) -> Unit)? = null
        val pending: Boolean get() = result != null

        fun setResult(result: InjectionStatus) {
            this.result = result
            onResult?.invoke(result, serial)
            onResult = null
        }

        fun setCallback(callback: (result: InjectionStatus, serial: String) -> Unit) {
            result?.let {
                callback(it, serial)
                return
            }
            onResult = callback
        }

        fun resultOrNull(): InjectionStatus? {
            return result
        }
    }
}


internal val NOT_WINDOWS_STATUS = InjectionStatus.ActionNotSupported(
    null,
    null,
    message("caos.injector.errors.only-windows")
)

internal val NOT_NIX_STATUS = InjectionStatus.ActionNotSupported(
    null,
    null,
    message("caos.injector.errors.only-nix")
)

internal const val DEBUG_INJECTOR = "bedalton.creatures.intellij.log.DEBUG_INJECTOR"