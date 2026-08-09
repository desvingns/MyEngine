import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val releaseKeystorePropertiesPath = providers.gradleProperty("myengine.keystore.properties")
    .orElse(providers.environmentVariable("MYENGINE_KEYSTORE_PROPERTIES"))
    .orElse("keystore.properties")
    .get()
val releaseKeystorePropertiesFile = rootProject.file(releaseKeystorePropertiesPath)
val releaseKeystoreProperties = Properties()
if (releaseKeystorePropertiesFile.isFile) {
    releaseKeystorePropertiesFile.inputStream().use(releaseKeystoreProperties::load)
}

fun resolveReleasePath(value: String): File {
    val path = File(value)
    return if (path.isAbsolute) path else File(releaseKeystorePropertiesFile.parentFile, value)
}

val releaseStorePath = releaseKeystoreProperties.getProperty("storeFile").orEmpty().trim()
val releaseStoreFile = releaseStorePath.takeIf { it.isNotEmpty() }?.let(::resolveReleasePath)
val releaseStorePassword = releaseKeystoreProperties.getProperty("storePassword").orEmpty()
val releaseKeyAlias = releaseKeystoreProperties.getProperty("keyAlias").orEmpty().trim()
val releaseKeyPassword = releaseKeystoreProperties.getProperty("keyPassword").orEmpty()
val releaseSigningConfigured = releaseStoreFile != null &&
    releaseStorePassword.isNotEmpty() &&
    releaseKeyAlias.isNotEmpty() &&
    releaseKeyPassword.isNotEmpty()

val applicationIdValue = providers.gradleProperty("myengine.applicationId")
    .orElse(providers.environmentVariable("MYENGINE_APPLICATION_ID"))
    .orElse("dev.myengine.android")
    .get()
require(Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$").matches(applicationIdValue)) {
    "myengine.applicationId must be a valid dotted Android application id."
}

val versionCodeValue = providers.gradleProperty("myengine.versionCode")
    .orElse(providers.environmentVariable("MYENGINE_VERSION_CODE"))
    .orElse("1")
    .get()
    .toIntOrNull()
    ?.also { require(it > 0) { "myengine.versionCode must be positive." } }
    ?: error("myengine.versionCode must be a positive integer.")
val versionNameValue = providers.gradleProperty("myengine.versionName")
    .orElse(providers.environmentVariable("MYENGINE_VERSION_NAME"))
    .orElse("0.0.1")
    .get()
    .trim()
    .also { require(it.isNotEmpty()) { "myengine.versionName must not be blank." } }

android {
    namespace = "dev.myengine.android"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        applicationId = applicationIdValue
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = versionCodeValue
        versionName = versionNameValue
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

    signingConfigs {
        create("release") {
            if (releaseSigningConfigured) {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

val validateReleaseSigning = tasks.register("validateReleaseSigning") {
    group = "verification"
    description = "Validates the untracked keystore properties required by release artifacts."
    doLast {
        val missing = buildList {
            if (!releaseKeystorePropertiesFile.isFile) add("keystore_properties_file")
            if (releaseStorePath.isEmpty()) add("storeFile")
            if (releaseStorePassword.isEmpty()) add("storePassword")
            if (releaseKeyAlias.isEmpty()) add("keyAlias")
            if (releaseKeyPassword.isEmpty()) add("keyPassword")
            if (releaseStoreFile == null || !releaseStoreFile.isFile) add("storeFile_exists")
        }
        check(missing.isEmpty()) {
            "Release signing is not configured. Add an untracked keystore properties file " +
                "(or pass -Pmyengine.keystore.properties=...) with keys storeFile, storePassword, " +
                "keyAlias, and keyPassword. Missing: ${missing.joinToString(", ")}."
        }
    }
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(validateReleaseSigning)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(dependencies.project(":games:sandbox"))
    implementation(libs.gdx)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.junit4)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
