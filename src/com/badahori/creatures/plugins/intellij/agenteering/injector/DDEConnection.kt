package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.caos.lang.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.pretty_tools.dde.client.DDEClientConversation
import com.pretty_tools.dde.client.DDEClientEventListener


internal class DDEConnection(private val variant: CaosVariant) : CaosConnection {

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

    override fun showAttribution(project: Project, variant:CaosVariant) {
        CaosInjectorNotifications.show(project, "Attribution", "${variant.code} caos injector is based off of information found @ http://sheeslostknowledge.blogspot.com/2014/02/connecting-to-creatures-dde-interface.html by LoneShee", NotificationType.INFORMATION)
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
