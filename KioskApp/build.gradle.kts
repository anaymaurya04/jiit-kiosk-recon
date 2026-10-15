plugins {
    id("com.android.application") version "9.4.1" apply false
    // AGP 9 bundles Kotlin, so we do NOT apply org.jetbrains.kotlin.android.
    // Only the compose compiler plugin is needed, matched to AGP's Kotlin (2.3.x).
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    // the vendored portal client's models are @Serializable
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.21" apply false
}
