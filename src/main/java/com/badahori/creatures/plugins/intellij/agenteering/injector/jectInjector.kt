package com.badahori.creatures.plugins.intellij.agenteering.injector

import bedalton.creatures.io.CREATURES_CHARSET
import bedalton.creatures.util.psuedoRandomUUID
import com.badahori.creatures.plugins.intellij.agenteering.utils.LOGGER
import com.badahori.creatures.plugins.intellij.agenteering.utils.className
import java.io.File

internal fun injectWithJect(connection: CaosConnection, creaturesDirectory: File, caos: String, flags: Int): InjectionStatus {
    if (!creaturesDirectory.exists()) {
        return InjectionStatus.BadConnection("Creatures path is invalid")
    }
    val bootstrap = File(creaturesDirectory, "Bootstrap")
    if (!bootstrap.exists()) {
        return InjectionStatus.BadConnection("Directory must be base install directory with \"Bootstrap\" folder")
    }
    val subFolder = File(bootstrap, "CaosPlugin-Ject")
    if (!subFolder.exists()) {
        try {
            subFolder.mkdir()
        } catch (e: Exception) {
            LOGGER.severe("Failed to create plugin bootstrap directory. ${e.className}:${e.message}")
            e.printStackTrace()
            return InjectionStatus.BadConnection("Failed to create plugin's bootstrap directory")
        }
    }
    val file = File.createTempFile("caos", "cos-ject", subFolder)
    try {
        file.writeText(caos.replace("\r?\n".toRegex(), "\r\n"), CREATURES_CHARSET)
    } catch (e: Exception) {
        LOGGER.severe("Failed to write temp file to bootstrap for injection. ${e.className}:${e.message}")
        e.printStackTrace()
        try {
            file.delete()
        } catch (_: Exception) {}
        return InjectionStatus.BadConnection("Failed to write CAOS ject contents to temp file in Bootstrap for injection")
    }
    val fileName = file.name
    val okay = psuedoRandomUUID()
    val caosString = """
        ject "$fileName" $flags
        outs "$okay"
    """.trimIndent()
    val status = try {
        connection.inject(caosString)
    } catch (e: Exception) {
        LOGGER.severe("Inject on associated connection failed with exception. ${e.className}:${e.message}")
        e.printStackTrace()
        InjectionStatus.BadConnection("JECT call failed without plugin exception")
    }

    // Delete the temp file
    try {
        file.delete()
    } catch (_: Exception) {}

    return when {
        status is InjectionStatus.Ok && status.response == okay -> {
            InjectionStatus.Ok("")
        }
        status is InjectionStatus.BadConnection -> status
        status is InjectionStatus.Bad -> formatBadJectResponse(status, caosString)
        else -> InjectionStatus.BadConnection("JECT call failed without error")
    }
}

private fun formatBadJectResponse(status: InjectionStatus.Bad, caosString: String): InjectionStatus {
    val response = status.error.trim()
    return if (response.contains('@')) {
        val temp = response.replace("[@\\s]+".toRegex(), " ").trim()
        val singleSpaced = caosString.replace("\\s+".toRegex(), " ")
        if (singleSpaced.contains(temp)) {
            InjectionStatus.Bad("JECT call failed plugin wrote BAD CAOS to string. CAOS: \"$singleSpaced\"")
        } else {
            InjectionStatus.Bad("JECT call failed. $response")
        }
    } else {
        InjectionStatus.BadConnection("JECT call failed. $response")
    }
}