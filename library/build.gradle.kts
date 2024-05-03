plugins {
    id(libs.plugins.commonMppLib.get().pluginId)
    id(libs.plugins.commonMppPublish.get().pluginId)
}

publishConfig {
    url = "https://maven.pkg.github.com/KryptonReborn/kotlin-bech32"
    groupId = "kotlin-bech32"
    artifactId = "library"
}

version = "0.0.1"

android {
    namespace = "dev.kryptonreborn.bech32"
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinStdLib)
                implementation(libs.kotlinxIo)
            }
        }
    }
}
