plugins {
    `java-library`
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
    api(project(":engine-core"))
    api(project(":engine-world"))
    api(project(":engine-content"))
    api(project(":engine-entities"))
    api(project(":engine-logistics"))
    api(project(":engine-defense"))
    api(project(":engine-render"))
    implementation(project(":engine-ai"))
    implementation(project(":engine-storyteller"))
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}
