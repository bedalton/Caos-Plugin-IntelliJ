package com.badahori.creatures.plugins.intellij.agenteering.caos.libs

import kotlinx.serialization.json.Json

private object LibLoader {
    val classLoader by lazy {
        javaClass.classLoader
                ?: throw NullPointerException("Failed to find JavaClass class loader")
    }
    fun getFileText(fileName:String) : String {
        val resource = classLoader.getResource(fileName)
                ?: throw NullPointerException("Failed to get resource: '$fileName'")
        return resource.readText(Charsets.UTF_8)
    }
}

val universalLib: CaosLibDefinitions by lazy {
    val libJsonText = LibLoader.getFileText("lib/caos.universal.lib.json")
    Json { isLenient = true; ignoreUnknownKeys = true }
            .decodeFromString(CaosLibDefinitions.serializer(), libJsonText)
}