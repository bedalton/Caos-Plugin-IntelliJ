package com.badahori.creatures.plugins.intellij.agenteering.sfc

import com.badahori.creatures.plugins.intellij.agenteering.sfc.lang.SFC_DECOMPILED_DATA_KEY
import com.badahori.creatures.plugins.intellij.agenteering.sfc.lang.SfcDecompiledFilePropertyPusher
import com.badahori.creatures.plugins.intellij.agenteering.sfc.reader.SfcReader
import com.badahori.creatures.plugins.intellij.agenteering.utils.rethrowAnyCancellationException
import com.google.gson.JsonObject
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.runBlocking

internal fun decompileSFCToJson(virtualFile: VirtualFile): String {
    // Check if file was already decompiled, and json data written.
    // If it was, return it
    virtualFile.getUserData(SFC_JSON_KEY)?.let {
        return it
    }
    // Deserialize virtual data if present.
    val holder = virtualFile.getUserData(SFC_DECOMPILED_DATA_KEY)
        ?: try {
            SfcDecompiledFilePropertyPusher.readFromStorage(virtualFile)
        } catch (e: Exception) {
            e.rethrowAnyCancellationException()
            null
        }
    // Check if file was decompiled before, and if it was
    // return the json result
    holder?.let {
        return it.data?.toString()
            ?: generateErrorJson(virtualFile, "SFC Decompile failed")
    }

    // Read and create json response object
    val json: String = runBlocking {
        try {
            SfcReader.readFile(virtualFile).let {
                it.data?.toString()
                    ?: generateErrorJson(
                        virtualFile,
                        "SFC decompile failed ${it.error ?: "without error message."}"
                    )
            }
        } catch (e: Exception) {
            e.rethrowAnyCancellationException()
            generateErrorJson(virtualFile, "SFC Decompile failed. ${e.message}")
        }
    }
    // Write the json response.
    virtualFile.putUserData(SFC_JSON_KEY, json)
    // finally, return resutl
    return json
}


private val SFC_JSON_KEY = Key<String>("creatures.caos.sfc.decompiled.JSON")

private fun generateErrorJson(
    virtualFile: VirtualFile,
    errorMessageIn: String?,
    status: String = "DECOMPILE_FAILED",
): String {
    val json = JsonObject()
    json.addProperty("file", virtualFile.path)
    json.addProperty("status", status)
    json.addProperty("error", errorMessageIn ?: DECOMPILE_FAILED_DEFAULT_ERROR_MESSAGE)
    return json.asString
}

private const val DECOMPILE_FAILED_DEFAULT_ERROR_MESSAGE = "Only Eden.sfc files can be decoded at this time"