plugins {
    kotlin("jvm") version KOTLIN_VERSION
    kotlin("plugin.serialization") version KOTLIN_VERSION
    id("org.jetbrains.dokka") version DOKKA_VERSION
}

apply(from = "./gradle/publishing.gradle.kts")

repositories {
    mavenCentral()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    sourceCompatibility = "11"
    targetCompatibility = "11"
    kotlinOptions.jvmTarget = "11"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.ktor:ktor-server-core:$KTOR_VERSION")
    implementation("io.ktor:ktor-server-sessions:$KTOR_VERSION")
    implementation("io.ktor:ktor-auth:$KTOR_VERSION")
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("io.ktor:ktor-server-tests:$KTOR_VERSION")
    testImplementation("io.ktor:ktor-serialization:$KTOR_VERSION")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$SERIALIZATION_VERSION")
}
