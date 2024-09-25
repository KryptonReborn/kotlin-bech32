plugins {
    // trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.kotlinPluginSerialization).apply(false)
    alias(libs.plugins.kotlinTestingResource).apply(false)
    alias(libs.plugins.ktlint).apply(true)
    alias(libs.plugins.dokka).apply(false)
    alias(libs.plugins.kover).apply(false)
}
