import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.badahori.creatures.plugins.intellij.agenteering.caos.generator.CaosDefGeneratorTask
import org.jetbrains.intellij.releaseType

plugins {
    id("org.jetbrains.intellij") version "1.3.0"
    kotlin("plugin.serialization") version "1.6.10"
    kotlin("jvm") version "1.6.10"
}

group = "com.badahori.creatures.plugins.intellij.agenteering"
version = "2021.12.01"


repositories {
    mavenLocal()
    mavenCentral()
    // used to download antlr-kotlin-runtime
    maven("https://jitpack.io")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.apache.commons:commons-imaging:1.0-alpha2")
    testImplementation("junit", "junit", "4.12")
    implementation("bedalton.creatures:PrayUtil:0.01.0")
    implementation("bedalton.creatures:CommonCore:0.01")

}
sourceSets.main {
    java.srcDirs("src/main/java", "gen", "src/main/gen")
}

java {
    releaseType("1.8")
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {

    tasks.withType<KotlinCompile>().all {
        kotlinOptions.jvmTarget = "1.8"
        this.kotlinOptions.apiVersion = "1.4"
        this.targetCompatibility = "1.8"
        this.sourceCompatibility = "1.8"
        kotlinOptions.freeCompilerArgs += listOf(
            "-Xjvm-default=enable",
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlin.OptIn",
            "-Xopt-in=kotlin.ExperimentalMultiplatform",
            "-Xopt-in=kotlin.ExperimentalUnsignedTypes",
            "-Xopt-in=kotlin.contracts.ExperimentalContracts",
            "-Xopt-in=ExperimentalJsExport",
            "-Xopt-in=org.jetbrains.annotations.ApiStatus.Experimental"
        )
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
            languageSettings.optIn("kotlin.ExperimentalMultiplatform")
            //languageSettings.useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
        }
    }

}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version.set("2019.3")
    updateSinceUntilBuild.set(false)
    sameSinceUntilBuild.set(true)
    sandboxDir.set("/Users/daniel/Projects/AppsAndDevelopment/Intellij Plugins/Plugin Sandbox")
    plugins.set(listOf("PsiViewer:193-SNAPSHOT", "com.mallowigi.idea:8.0"))
}

tasks.register<CaosDefGeneratorTask>("generateCaosDef") {
    this.targetFolder = File(buildDir, "resources/main/lib")
    this.createFolder = true
    this.generateCaosDef()
}

tasks.getByName<org.jetbrains.intellij.tasks.RunIdeTask>("runIde") {
    dependsOn("generateCaosDef")
}
tasks.getByName("buildPlugin") {
    dependsOn("generateCaosDef")
}

tasks.getByName<org.jetbrains.intellij.tasks.RunPluginVerifierTask>("runPluginVerifier") {
    this.ideVersions.set(
        listOf(
            "IU-193.7288.26",
            "IU-201.8743.12",
            "IC-212.5080.55"
        )
    )
}

tasks.withType<org.jetbrains.intellij.tasks.RunIdeTask>().all {
    maxHeapSize = "2g"
    autoReloadPlugins.set(true)
}

tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {

}