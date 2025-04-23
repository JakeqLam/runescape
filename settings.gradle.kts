rootProject.name = "auto-bots"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

include("common")
include("mining")
include("cooking")
include("agility")

project(":common").projectDir = file("scripts/common")
project(":mining").projectDir = file("scripts/mining")
project(":cooking").projectDir = file("scripts/cooking")
project(":agility").projectDir = file("scripts/agility")

