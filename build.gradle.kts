plugins {
    alias(libs.plugins.kotlin.jvm)
    application
    id("com.gradleup.shadow") version "9.0.0"
}

repositories {
    mavenCentral()
    mavenLocal()
    google()
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:+")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")
    implementation("org.aspectj:aspectjrt:1.9.23")
    testImplementation("io.kotest:kotest-runner-junit5:+")
    testImplementation("io.kotest:kotest-assertions-core:+")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

application {
    // Define the main class for the application.
    mainClass = "org.ldemetrios.kscript.MainKt"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}