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

rootProject.name = "OpenScanner"

include(":app")
include(":core:model")
include(":core:domain")
include(":core:export")
include(":core:privacy")
include(":data:wifi-android")
include(":data:settings")
