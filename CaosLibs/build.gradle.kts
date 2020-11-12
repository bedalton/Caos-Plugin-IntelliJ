plugins {
    kotlin("plugin.serialization")
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

group = "com.badahori.creatures.plugins.intellij.agenteering.caos.libs.compiler"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.0")
}
sourceSets.main {
    java.srcDirs("src/main/kotlin")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

