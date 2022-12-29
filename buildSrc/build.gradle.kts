plugins {
    kotlin("plugin.serialization") version "1.7.20"
    kotlin("jvm") version "1.7.20"
}

val javaVersion: String = "1.8"

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.0")
}
sourceSets.main {
    java.srcDirs("src/main/kotlin")
}

java {
    val javaSdkString = "VERSION_" + javaVersion.replace('.', '_')
    val javaSdkVersion = JavaVersion.valueOf(javaSdkString)
    sourceCompatibility = javaSdkVersion
    targetCompatibility = javaSdkVersion
}
kotlin {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
        kotlinOptions.languageVersion = "1.7"
        kotlinOptions.apiVersion = "1.7"
        kotlinOptions.jvmTarget = javaVersion
    }
}
