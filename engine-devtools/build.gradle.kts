plugins {
    application
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":engine-core"))
    implementation(project(":engine-content"))
    implementation(project(":games:sandbox"))
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.serialization.json)
}

application {
    mainClass.set("dev.myengine.devtools.DevtoolsMainKt")
}

tasks.test {
    useJUnitPlatform()
}
