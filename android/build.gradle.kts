plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "dev.myengine.android"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.myengine.android"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = 1
        versionName = "0.0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    // Keep the external sandbox content pack, including maps.json, in the APK without pulling an
    // Android API into engine-content. An Android asset adapter can load this tree when the shell
    // moves beyond its current startup smoke surface.
    sourceSets {
        getByName("main") {
            assets.srcDir("../games/sandbox/content")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(dependencies.project(":games:sandbox"))
    implementation(libs.gdx)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
