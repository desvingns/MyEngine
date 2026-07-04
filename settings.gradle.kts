pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MyEngine"

include(":engine-core")
include(":engine-world")
include(":engine-content")
include(":engine-entities")
include(":engine-ai")
include(":engine-logistics")
include(":engine-defense")
include(":engine-storyteller")
include(":engine-render")
include(":engine-devtools")
include(":engine-testkit")
include(":desktop")
include(":android")
include(":games:sandbox")

project(":games:sandbox").projectDir = file("games/sandbox")
