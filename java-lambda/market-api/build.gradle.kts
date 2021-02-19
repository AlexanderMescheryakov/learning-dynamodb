val quarkusVersion: String by project

plugins {
    java
    id("io.quarkus")
    id("org.kordamp.gradle.jandex")
}

repositories {
    mavenLocal()
    jcenter()
}

dependencies {
    implementation(enforcedPlatform("io.quarkus:quarkus-universe-bom:${quarkusVersion}"))
    implementation("io.quarkus:quarkus-amazon-lambda")
    implementation("io.quarkus:quarkus-arc")
    implementation(platform("software.amazon.awssdk:bom:2.15.53"))
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:apigateway")
    implementation("commons-logging:commons-logging:1.2")
    implementation("joda-time:joda-time:2.10.10")
    compileOnly("org.projectlombok:lombok:1.18.16")
    annotationProcessor("org.projectlombok:lombok:1.18.16")

    testCompileOnly("org.projectlombok:lombok:1.18.16")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.16")
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.quarkus:quarkus-junit5-mockito")
    testImplementation("io.quarkus:quarkus-test-amazon-lambda")
    testImplementation("org.mockito:mockito-inline:3.6.28")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
