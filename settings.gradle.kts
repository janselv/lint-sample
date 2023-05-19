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

rootProject.name = "lint-sample"
include(":app")
include(":platform:lint")

include(":platform:annotations")
include(":platform:internallib")
include(":platform:runtime")

