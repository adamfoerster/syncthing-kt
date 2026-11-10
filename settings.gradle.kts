rootProject.name = "syncthing-kt"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":androidApp")
include(":desktopApp")
include(":app")
include(":syncthing-native")

include(":core:domain")
include(":core:data")
include(":core:presentation")
include(":core:design-system")

include(":feature:onboarding:presentation")
include(":feature:home:presentation")
include(":feature:sharing:presentation")
include(":feature:folders:presentation")
include(":feature:devices:presentation")
include(":feature:settings:presentation")

include(":feature:files:domain")
include(":feature:files:data")
include(":feature:files:presentation")
include(":feature:conflicts:domain")
include(":feature:conflicts:data")
include(":feature:conflicts:presentation")
