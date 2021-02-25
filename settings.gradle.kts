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
        id("io.franzbecker.gradle-lombok") version "2.1"
    }
}

rootProject.name = "learning-dynamodb"

include(":market-api")
project(":market-api").projectDir = file("java-lambda/market-api")

include(":cdk")
project(":cdk").projectDir = file("cdk")

