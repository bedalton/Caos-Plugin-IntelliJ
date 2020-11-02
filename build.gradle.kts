import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.intellij") version "0.6.1"
    kotlin("jvm") version "1.4.10"
    kotlin("plugin.serialization") version "1.4.10"
}

group = "com.badahori.creatures.plugins.intellij.agenteering"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.0")
    testCompile("junit", "junit", "4.12")

}
kotlin {

    tasks.withType<KotlinCompile>().all {
        kotlinOptions.freeCompilerArgs += listOf(
                "-Xopt-in=kotlin.RequiresOptIn",
                "-Xopt-in=kotlin.OptIn",
                "-Xopt-in=kotlin.ExperimentalMultiplatform",
                "-Xopt-in=kotlin.ExperimentalUnsignedTypes",
                "-Xopt-in=kotlin.contracts.ExperimentalContracts",
                "-Xopt-in=ExperimentalJsExport"
        )
    }
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version = "2019.2"
}
tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {

}