import com.runemate.game.api.bot.data.Category
import com.runemate.game.api.bot.data.FeatureType
import com.runemate.gradle.RuneMatePlugin

plugins {
    id("java")
    id("com.runemate") version "1.5.1"

}

subprojects {
    apply<JavaPlugin>()
    apply<RuneMatePlugin>()

    tasks.runClient {
        enabled = true
    }
}

group = "com.runemate.party"
version = "1.0.0"

runemate {
    devMode = true
    autoLogin = true

    manifests {
        create("Auto Bots") {
            mainClass = "java.Test"
            tagline = "test"
            description = "Testing"
            version = "1.0.0"
            internalId = "test-bot"

            obfuscation {
                +"obfuscation rule"
                exclude("obfuscation rule")
                exclude { "obfuscation rule" }
            }
            features {
                required(FeatureType.DIRECT_INPUT)
            }
            resources {
                +"resource rule"
                include("resource rule")
                include { "resource rule" }
            }
            variants {
                variant(name = "My Bot", price = BigDecimal.ZERO)
            }

        }

    }
}

