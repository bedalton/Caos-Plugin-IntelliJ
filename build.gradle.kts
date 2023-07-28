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
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij")
    kotlin("plugin.serialization")
}

val projectVersion: String by project
group = "com.badahori.creatures.plugins.intellij.agenteering"
version = projectVersion


val ideaVersionStart: String by project
val psiViewerVersion: String by project
val javaVersion: String by project

// Kotlin / Kotlinx
val kotlinxCoroutinesVersion: String by project
val kotlinxSerializationVersion: String by project

// Other
val korImagesVersion: String by project

// Creatures
val creaturesAgentUtilVersion: String by project
val creaturesSpriteUtilVersion: String by project
val creaturesCommonCliVersion: String by project
val creaturesCommonVersion: String by project

// Common Libs
val commonCoreVersion: String by project
val localFilesVersion: String by project
val byteUtilVersion: String by project
val commonLogVersion: String by project

repositories {
    mavenLocal()
    mavenCentral()
    // used to download antlr-kotlin-runtime
    maven("https://jitpack.io")
}

dependencies {

    //Kotlin / Kotlinx
    implementationExcludingKotlin("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")

    // Other Libs
    implementationExcludingKotlin("com.soywiz.korlibs.korim:korim:$korImagesVersion")
    implementationExcludingKotlin("org.apache.commons:commons-imaging:1.0-alpha2")
    testImplementation("junit", "junit", "4.13") {
        excludeKotlin()
    }

    // Creatures libs
    implementationExcludingKotlin("com.bedalton.creatures:agent-util:$creaturesAgentUtilVersion")
    implementationExcludingKotlin("com.bedalton.creatures:common-sprite:$creaturesSpriteUtilVersion")
    implementationExcludingKotlin("com.bedalton.creatures:creatures-common:$creaturesCommonVersion")

    // Common Libs
    implementationExcludingKotlin("com.bedalton:common-core:$commonCoreVersion")
    implementationExcludingKotlin("com.bedalton:local-files:$localFilesVersion")
    implementationExcludingKotlin("com.bedalton:common-byte:$byteUtilVersion")
    implementationExcludingKotlin("com.bedalton:common-log:$commonLogVersion")



}
tasks.test {
    useJUnit()
}

sourceSets.main {
    java.srcDirs("src/main/java", "gen", "src/main/gen")
}

java {
    val javaSdkString = "VERSION_" + (if (javaVersion == "9") "1_" else "") + javaVersion.replace('.', '_')
    val javaSdkVersion = JavaVersion.valueOf(javaSdkString)
    sourceCompatibility = javaSdkVersion
    targetCompatibility = javaSdkVersion

}

kotlin {

    tasks.withType<KotlinCompile>().all {
        kotlinOptions {
            this.jvmTarget = javaVersion
            this.apiVersion = "1.7"
            this.languageVersion = "1.7"
            this.freeCompilerArgs += listOf(
                "-Xjvm-default=all-compatibility",
            )
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
            languageSettings.optIn("kotlin.ExperimentalMultiplatform")
            languageSettings.optIn("kotlin.ExperimentalUnsignedTypes")
            languageSettings.optIn("kotlin.contracts.ExperimentalContracts")
            languageSettings.optIn("kotlin.ExperimentalStdlibApi")
            languageSettings.optIn("kotlinx.coroutines.DelicateCoroutinesApi")
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")

            //languageSettings.useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
        }
    }

}


// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
//    version.set(ideaVersionStart)
    version.set(ideaVersionStart)
    updateSinceUntilBuild.set(false)
    sandboxDir.set("/Users/daniel/Projects/AppsAndDevelopment/Intellij Plugins/Plugin Sandbox")
//    plugins.set(listOf("PsiViewer:$psiViewerVersion"))//, "com.mallowigi.idea:10.0"))

}

tasks.register<CaosDefGeneratorTask>("generateCaosDef") {
    this.targetFolder = File(buildDir, "resources/main/lib")
    this.createFolder = true
    this.generateCaosDef()
}

tasks.getByName<org.jetbrains.intellij.tasks.RunIdeTask>("runIde") {
    dependsOn("generateCaosDef")
}

tasks.getByName("jar") {
    dependsOn("generateCaosDef")
}


tasks.getByName("buildPlugin") {
    dependsOn("generateCaosDef")
}

tasks.getByName<org.jetbrains.intellij.tasks.RunPluginVerifierTask>("runPluginVerifier") {
    this.ideVersions.set(
        listOf(
            "IU-223.4884.69",
            "IU-201.8743.12",
            "IU-201.8743.12",
            "IC-212.5080.55"
        )
    )
}

tasks.withType<org.jetbrains.intellij.tasks.RunIdeTask>().all {
    maxHeapSize = "4g"
    autoReloadPlugins.set(true)
}

tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {

}


fun DependencyHandler.implementationExcludingKotlin(dependencyNotation: String) {
    implementation(dependencyNotation) {
        excludeKotlin()
    }
}

fun ExternalModuleDependency.excludeKotlin() {
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
}