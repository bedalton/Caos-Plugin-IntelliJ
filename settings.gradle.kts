rootProject.name = "CaosPlugin"


pluginManagement {
    val ideaGradlePluginVersion: String by settings
    val kotlinVersion: String by settings

    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlinVersion
        id("org.jetbrains.intellij") version ideaGradlePluginVersion
        kotlin("plugin.serialization") version kotlinVersion
    }
}