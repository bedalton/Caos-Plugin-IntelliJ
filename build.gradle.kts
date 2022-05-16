import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.badahori.creatures.plugins.intellij.agenteering.caos.generator.CaosDefGeneratorTask

configurations {
    compileClasspath
        .get()
        .resolutionStrategy
        .sortArtifacts(ResolutionStrategy.SortOrder.DEPENDENCY_FIRST)

    testCompileClasspath
        .get()
        .resolutionStrategy
        .sortArtifacts(ResolutionStrategy.SortOrder.DEPENDENCY_FIRST)
}

plugins {
//    id("java")
    id("org.jetbrains.intellij") version "1.3.0"
    kotlin("plugin.serialization") version "1.5.30"
    id("org.jetbrains.kotlin.jvm") version "1.5.30"
}

group = "com.badahori.creatures.plugins.intellij.agenteering"
version = "2022.02.01"


val korImagesVersion: String by project

repositories {
    mavenLocal()
    mavenCentral()
    // used to download antlr-kotlin-runtime
    maven("https://jitpack.io")
}

dependencies {

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2") {
        excludeKotlin()
    }

//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")

//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")

    implementation("org.apache.commons:commons-imaging:1.0-alpha2") {
        excludeKotlin()
    }

    testImplementation("junit", "junit", "4.12") {
        excludeKotlin()
    }

    implementation("bedalton.creatures:PrayUtil:0.03") {
        excludeKotlin()
    }

    implementation("bedalton.creatures:SpriteUtil:0.02") {
        excludeKotlin()
    }

    implementation("com.soywiz.korlibs.korim:korim:$korImagesVersion") {
        excludeKotlin()
    }

    implementation("bedalton.creatures:CommonCore:0.02") {
        excludeKotlin()
    }


}
sourceSets.main {
    java.srcDirs("src/main/java", "gen", "src/main/gen")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
}

kotlin {

    tasks.withType<KotlinCompile>().all {
        kotlinOptions {
            this.jvmTarget = "11"
            this.apiVersion = "1.5"
            this.languageVersion = "1.5"
            this.freeCompilerArgs += listOf(
                "-Xjvm-default=all-compatibility"
            )
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
            languageSettings.optIn("kotlin.ExperimentalMultiplatform")
            languageSettings.optIn("kotlin.ExperimentalUnsignedTypes")
            languageSettings.optIn("kotlin.contracts.ExperimentalContracts")
            languageSettings.optIn("kotlin.js.ExperimentalJsExport")
            languageSettings.optIn("kotlin.ExperimentalJsExport")
            languageSettings.optIn("org.jetbrains.annotations.ApiStatus.Experimental")
            //languageSettings.useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
        }
    }

}


// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version.set("2020.1")
    updateSinceUntilBuild.set(false)
    sameSinceUntilBuild.set(true)
    sandboxDir.set("/Users/daniel/Projects/AppsAndDevelopment/Intellij Plugins/Plugin Sandbox")
    plugins.set(listOf("PsiViewer:201-SNAPSHOT"))//, "com.mallowigi.idea:10.0"))

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
            "IU-201.8743.12",
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


fun ExternalModuleDependency.excludeKotlin() {
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
}