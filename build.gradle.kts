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
    id("org.jetbrains.kotlin.jvm") version "1.7.20"
    id("org.jetbrains.intellij") version "1.10.2"
    kotlin("plugin.serialization") version "1.7.20"
}



group = "com.badahori.creatures.plugins.intellij.agenteering"
version = "2022.03.02"


val ideaVersionStart: String by project
val psiViewerVersion: String by project
val javaVersion: String by project

val korImagesVersion: String by project
val creaturesAgentUtilVersion: String by project
val creaturesSpriteUtilVersion: String by project
val creaturesCommonCliVersion: String by project
val creaturesCommonVersion: String by project
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

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1") {
        excludeKotlin()
    }

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    implementation("org.apache.commons:commons-imaging:1.0-alpha2") {
        excludeKotlin()
    }

    testImplementation("junit", "junit", "4.12") {
        excludeKotlin()
    }

    implementation("bedalton.creatures:agent-util:$creaturesAgentUtilVersion") {
        excludeKotlin()
    }

    implementation("bedalton.creatures:common-sprite:$creaturesSpriteUtilVersion") {
        excludeKotlin()
    }

    implementation("com.soywiz.korlibs.korim:korim:$korImagesVersion") {
        excludeKotlin()
    }

    implementation("com.bedalton:common-core:$commonCoreVersion") {
        excludeKotlin()
    }

    implementation("bedalton.creatures:creatures-common:$creaturesCommonVersion") {
        excludeKotlin()
    }

    implementation("bedalton.creatures:creatures-common-cli:$creaturesCommonCliVersion") {
        excludeKotlin()
    }

    implementation("com.bedalton:local-files:$localFilesVersion") {
        excludeKotlin()
    }

    implementation("com.bedalton:common-byte:$byteUtilVersion") {
        excludeKotlin()
    }

    implementation("com.bedalton:common-log:$commonLogVersion") {
        excludeKotlin()
    }

    testImplementation("junit:junit:4.13.2")


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
    plugins.set(listOf("PsiViewer:$psiViewerVersion"))//, "com.mallowigi.idea:10.0"))

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


fun ExternalModuleDependency.excludeKotlin() {
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
}