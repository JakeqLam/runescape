import com.runemate.game.api.bot.data.Category

dependencies {
    implementation(project(":common"))
}

runemate {
    //This is useful when you are working on a bot that you are not ready to publish yet
    excludeFromSubmission = false
    /*
     * The submission token is used by Gradle to authenticate with RuneMate servers when publishing bots to the store.
     * You can get a token from the RuneMate developer panel, and store it in your root 'gradle.properties' file.
     * - On Windows, that will be in %userprofile%\.gradle (make one if it doesn't exist)
     * - On Mac/Linux, that will be in ~/.gradle
     *
     * Do not specify it in this file, it will be detected automatically if declared in your gradle.properties under the key
     * 'runemateSubmissionToken', I have only included it here so that you are aware of it
     */
    submissionToken = ""

    manifests {
        create("Simple Crafter") {
            mainClass = "com.runemate.party.crafting.GenericCrafter"
            tagline = "My simple Crafter Trainer!"
            description = "A simple Crafter Trainer that does it all"
            version = "1.0.0"
            internalId = "example-Crafter"

            categories(Category.CRAFTING)
        }

    }
}


