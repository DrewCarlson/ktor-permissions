plugins {
    kotlin("multiplatform") version KOTLIN_VERSION
    kotlin("plugin.serialization") version KOTLIN_VERSION
    id("org.jetbrains.dokka") version DOKKA_VERSION
}

apply(from = "./gradle/publishing.gradle.kts")

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    sourceSets {
        val jvmMain by getting {
            kotlin.srcDir("src/main/kotlin")
            dependencies {
                implementation(kotlin("stdlib"))
                implementation("io.ktor:ktor-server-core:$KTOR_VERSION")
                implementation("io.ktor:ktor-server-sessions:$KTOR_VERSION")
                implementation("io.ktor:ktor-auth:$KTOR_VERSION")
            }
        }
        val jvmTest by getting {
            kotlin.srcDir("src/test/kotlin")
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation("io.ktor:ktor-server-tests:$KTOR_VERSION")
                implementation("io.ktor:ktor-serialization:$KTOR_VERSION")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$SERIALIZATION_VERSION")
            }
        }
    }
}
