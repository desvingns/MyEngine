plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}

subprojects {
    group = "dev.myengine"
    version = "0.0.1"
}
