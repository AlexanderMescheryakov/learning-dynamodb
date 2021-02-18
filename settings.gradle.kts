pluginManagement {
    val quarkusVersion: String by settings
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        id("io.quarkus") version quarkusVersion
        id("org.kordamp.gradle.jandex") version "0.8.0"
    }
}

rootProject.name = "learning-dynamodb"

include(":cdk")
project(":cdk").projectDir = file("cdk")

