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
    ":tests",
    ":core:common",
    ":core:model",
    ":core:network",
    ":core:database",
    ":core:data",
    ":core:ui",
    ":shared:navigation",
    ":features:auth",
    ":features:teacher:home",
    ":features:teacher:journal",
    ":features:teacher:studentcard",
    ":features:teacher:dashboard",
    ":features:teacher:ved",
    ":features:student:home",
    ":features:student:journal",
    ":features:methodist:dashboard",
    ":features:methodist:journals",
    ":features:methodist:templates",
    ":features:methodist:journalcreate",
    ":features:admin:dashboard",
    ":features:admin:users",
    ":features:admin:audit",
    ":features:admin:journals",
    ":features:admin:periods",
    ":features:admin:access",
    ":features:admin:problemstudents"
)
