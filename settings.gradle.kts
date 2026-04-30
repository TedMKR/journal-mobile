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

rootProject.name = "journal-android"

include(
    ":app",
    ":core:common",
    ":core:model",
    ":core:network",
    ":core:ui",
    ":shared:navigation",
    ":features:auth",
    ":features:teacher:home",
    ":features:teacher:journal",
    ":features:teacher:dashboard",
    ":features:teacher:ved",
    ":features:teacher:studentcard",
    ":features:student:home",
    ":features:methodist:templates",
    ":features:admin:imports"
)
