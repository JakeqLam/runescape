rootProject.name = "auto-bots"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

include("common")
include("mining")
include("cooking")
include("fishing")
include("agility")
include("combat")

project(":common").projectDir = file("scripts/common")
project(":mining").projectDir = file("scripts/mining")
project(":cooking").projectDir = file("scripts/cooking")
project(":agility").projectDir = file("scripts/agility")
project(":fishing").projectDir = file("scripts/fishing")
project(":combat").projectDir = file("scripts/combat")

