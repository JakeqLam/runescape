rootProject.name = "auto-bots"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

include("mining")
include("cooking")
include("agility")

project(":mining").projectDir = file("scripts/mining")
project(":cooking").projectDir = file("scripts/cooking")
project(":agility").projectDir = file("scripts/agility")

