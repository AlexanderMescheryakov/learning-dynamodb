import com.moowork.gradle.node.npm.NpmTask

plugins {
    id("com.github.node-gradle.node") version "2.2.4"
}

node {
    version = "12.19.1"
    npmVersion = "6.14.9"
    download = true
}

var build = tasks.register<NpmTask>("build") {
    group = "build"
    description = "Npm build"
    dependsOn(tasks.npmInstall)
    setArgs(listOf("run", "build"))
}

tasks.register<NpmTask>("check") {
    group = "check"
    description = "Runs eslint"
    dependsOn(tasks.npmInstall)
    setArgs(listOf("run", "lint"))
}

tasks.register<NpmTask>("test") {
    group = "test"
    description = "Runs Unit Tests"
    dependsOn(build)
    setArgs(listOf("run", "test"))
}

tasks.register<NpmTask>("clean") {
    group = "build"
    description = "Cleanup"
    delete("node_modules", "cdk.out")
}

val deploymentConfig: String by project
val stackEnv: String by project
val jvmRuntime = project.properties["quarkus.package.type"];
val deploymentConfigStream = java.io.FileInputStream(deploymentConfig)
val configPropDefaults = java.util.Properties()
configPropDefaults.setProperty("region", "us-east-1")
val configProp = java.util.Properties(configPropDefaults)
configProp.load(deploymentConfigStream)
val awsRegion = configProp.getProperty("region")
val cdkContextOptions = configProp.flatMap {
    (key, value) -> listOf("-c", "${key}=${value}")} + listOf("-c", "env=$stackEnv") + listOf("-c", "jvmRuntime=$jvmRuntime")

tasks.register<NpmTask>("synth") {
    description = "Synth the stack"
    dependsOn(build, ":market-api:quarkusBuild")
    setArgs(listOf("run", "cdk", "--", "synth") + cdkContextOptions)
}

tasks.register<NpmTask>("deploy") {
    description = "Deploy the stack"
    dependsOn(build, ":market-api:quarkusBuild")
    setArgs(listOf("run", "cdk", "--", "deploy", "--require-approval=never") + cdkContextOptions)
}

tasks.register<NpmTask>("seed") {
    description = "Import test data"
    dependsOn(build)
    setArgs(listOf("run", "dynamodb-import", "--", "--jsonFile", "$projectDir/seed-data.json",
        "--tableName", "LearningDynamoDb-$stackEnv", "--regionName", awsRegion))
}
