package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.now
import com.badahori.creatures.plugins.intellij.agenteering.caos.utils.nullIfEmpty
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.pretty_tools.dde.client.DDEClientConversation
import com.pretty_tools.dde.client.DDEClientEventListener

object Injector {

    private fun getActualVersion(project: Project, variant: CaosVariant) : CaosVariant {
        if (variant.isNotOld) {
            return variant;
        }
        val code = "dde: putv vrsn"
        val response = injectPrivate(project, variant, code)
        if (response !is InjectionStatus.Ok)
            return variant
        return try {
            if (response.response.toInt() < 6) {
                CaosVariant.C1
            } else {
                CaosVariant.C2
            }
        } catch(e:Exception) {
            variant
        }
    }

    fun inject(project: Project, variant: CaosVariant, caosIn:String) : Boolean {
        if (!canConnectToVariant(variant)) {
            val error = "Injection to ${variant.fullName} is not yet implemented"
            CaosInjectorNotifications.show(project, "ConnectionException", error, NotificationType.ERROR)
            return false
        }
        if (variant.isOld) {
            val actualVersion = getActualVersion(project, variant)
            if (actualVersion != variant) {
                postError(project, "Connection Error", "Grammar set to variant [${variant}], but ide is connected to ${actualVersion.fullName}")
                return false
            }
        }
        val response = injectPrivate(project, variant, caosIn)
        when (response) {
            is InjectionStatus.Ok -> postOk(project, response)
            is InjectionStatus.BadConnection -> postError(project, "Connection Failed", response.error)
            is InjectionStatus.Bad -> postError(project, "Injection Failed", response.error)
        }
        return response is InjectionStatus.Ok
    }
    private fun injectPrivate(project: Project, variant: CaosVariant, caosIn:String) : InjectionStatus? {
        val connection = connection(project, variant)
                ?: return null
        val caos = sanitize(caosIn)
        if (!connection.isConnected() && !connection.connect(false)) {
            return null
        }
        return connection.inject(caos)
    }

    private fun connection(project: Project, variant: CaosVariant): Connection? {
        val conn = getConnection(variant)
        if (conn == null || !conn.connect()) {
            CaosInjectorNotifications.show(project, "Connection Failed", "Failed to initiate CAOS connection. Ensure ${variant.fullName} is running and try again", NotificationType.ERROR)
            return null
        }
        return conn
    }

    private fun postOk(project: Project, response:InjectionStatus.Ok) {
        val responseText = response.response.nullIfEmpty()?.let {
            "\n\tOutput: $it"
        } ?: ""
        val message = "status: OK$responseText"
        CaosInjectorNotifications.show(project, "Injection Success", message, NotificationType.INFORMATION)
    }

    fun postInfo(project: Project, title:String, message: String) {
        CaosInjectorNotifications.show(project, title, message, NotificationType.INFORMATION)
    }

    fun postError(project: Project, title:String, message: String) {
        CaosInjectorNotifications.show(project, title, message, NotificationType.ERROR)
    }

    fun postWarning(project: Project, title:String, message: String) {
        CaosInjectorNotifications.show(project, title, message, NotificationType.WARNING)
    }

    private fun getConnection(variant: CaosVariant) : Connection? {
        return when (variant) {
            CaosVariant.C1 -> DDEConnection(variant)
            CaosVariant.C2 -> DDEConnection(variant)
            else -> null
        }
    }

    private fun sanitize(caos:String) : String {
        var out = caos
        out = out.replace("/[ ]+".toRegex(), " ")
        out.replace("[ ]*,[ ]".toRegex(), ",")
        return out.trim()
    }

    fun canConnectToVariant(variant: CaosVariant): Boolean {
        return when (variant) {
            CaosVariant.C1 -> true
            CaosVariant.C2 -> true
            CaosVariant.CV -> false
            CaosVariant.C3 -> false
            CaosVariant.DS -> false
            else -> false
        }
    }

}

internal interface Connection {
    fun inject(caos: String): InjectionStatus
    fun disconnect(): Boolean
    fun isConnected(): Boolean
    fun connect(silent: Boolean = false): Boolean
}

private class DDEConnection(private val variant: CaosVariant) : Connection {

    override fun inject(caos: String): InjectionStatus {
        val conn = getConnection()
                ?: return InjectionStatus.BadConnection("Failed to fetch connection to Vivarium")
        try {
            conn.poke("Macro", caos+0.toChar())
        } catch(e:Exception) {
            return InjectionStatus.Bad("Poke macro failed with error: ${e.message}")
        }
        return try {
            val response = conn.request("Macro")
            InjectionStatus.Ok(response)
        } catch(e:Exception) {
            LOGGER.severe("Request failed after poke with dde error: ${e.message}")
            InjectionStatus.Bad("Do request failed with error: ${e.message}")
        }
    }

    override fun disconnect(): Boolean {
        return try {
            connection?.disconnect()
            connection = null
            true
        } catch (e: Exception) {
            LOGGER.severe("Failed to disconnect from DDE Vivarium with error: ${e.message}")
            false
        }
    }

    override fun isConnected(): Boolean {
        return connection != null
    }

    override fun connect(silent: Boolean): Boolean {
        if (connection != null)
            return true
        return getConnection() != null
    }

    private fun getConnection(): DDEClientConversation? {
        var conn = connection
        if (conn != null)
            return connection
        conn = DDEClientConversation()
        conn.eventListener = object : DDEClientEventListener {
            override fun onItemChanged(p0: String?, p1: String?, p2: String?) {

            }

            override fun onDisconnect() {
                connection?.eventListener = null
                connection = null
            }
        }
        try {
            conn.connect(server, topic)
            connection = conn
            return conn
        } catch (e: Exception) {
            LOGGER.severe("Connection to the vivarium failed. Ensure ${variant.fullName} is running")
            return null
        }
    }

    companion object {
        private const val server: String = "Vivarium"
        private const val topic: String = "IntelliJCaosInjector"
        private var connection: DDEClientConversation? = null
    }

}

private data class CaosResponse(val caos:String, val success:Boolean, val response:String? = null, val error:String? = null, val time:Long = now)

internal sealed class InjectionStatus {
    data class Ok(val response:String): InjectionStatus()
    data class Bad(val error:String) : InjectionStatus()
    data class BadConnection(val error:String) : InjectionStatus()
}