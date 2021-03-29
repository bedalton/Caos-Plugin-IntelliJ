package com.badahori.creatures.plugins.intellij.agenteering.injector

import com.badahori.creatures.plugins.intellij.agenteering.caos.libs.CaosVariant
import com.badahori.creatures.plugins.intellij.agenteering.caos.psi.util.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.substringFromEnd
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.pretty_tools.dde.client.DDEClientConversation
import com.pretty_tools.dde.client.DDEClientEventListener


/**
 * Class for managing a C1/C2 DDE CAOS connection
 */
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
            var response = conn.request("Macro")
            if (response == "0000") {
                InjectionStatus.Bad("Silent exception raised during injected script execution")
            } else if (response.length > 1) {
                if (response.last() == 0.toChar()) {
                    response = response.substringFromEnd(0, 1)
                }
            }
            InjectionStatus.Ok(response)
        } catch(e:Exception) {
            LOGGER.severe("Request failed after poke with dde error: ${e.message}")
            InjectionStatus.Bad("Do request failed with error: ${e.message}")
        }
    }

    override fun injectEventScript(family: Int, genus: Int, species: Int, eventNumber:Int, caos: String): InjectionStatus {
        val expectedHeader = "scrp $family $genus $species $eventNumber"
        val removalRegex = "^scrp\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s*".toRegex()
        val caosFormatted = if (!caos.trim().toLowerCase().startsWith(expectedHeader)) {
             if (removalRegex.matches(caos)) {
                 caos.replace(removalRegex, "")
             } else
                 "$expectedHeader $caos"
        } else
            caos
        return inject(caosFormatted)
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

    override fun showAttribution(project: Project, variant: CaosVariant) {
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
        return try {
            conn.connect(server, topic)
            connection = conn
            conn
        } catch (e: Exception) {
            e.printStackTrace()
            LOGGER.severe("Connection to the vivarium failed. Ensure ${variant.fullName} is running. Error: " + e.message)
            null
        }
    }

    companion object {
        private const val server: String = "Vivarium"
        private const val topic: String = "IntelliJCaosInjector"
        private var connection: DDEClientConversation? = null
    }

}
