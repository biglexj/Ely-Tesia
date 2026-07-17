plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

val elytesiaVersionName = providers.gradleProperty("elytesia.versionName").get()
val elytesiaVersionCode = providers.gradleProperty("elytesia.versionCode").get().toInt()
val signingProperties = rootProject.file("keystore.properties")
    .takeIf { it.isFile }
    ?.readLines()
    ?.mapNotNull { line ->
        line.takeUnless { it.isBlank() || it.trimStart().startsWith("#") }
            ?.split('=', limit = 2)
            ?.takeIf { it.size == 2 }
            ?.let { it[0].trim() to it[1].trim() }
    }
    ?.toMap()
    .orEmpty()
fun signingValue(propertyName: String, environmentName: String): String? =
    signingProperties[propertyName]?.takeIf(String::isNotBlank)
        ?: System.getenv(environmentName)?.takeIf(String::isNotBlank)

val releaseStorePath = signingValue("storeFile", "ELYTESIA_KEYSTORE_FILE")
val releaseStorePassword = signingValue("storePassword", "ELYTESIA_KEYSTORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "ELYTESIA_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "ELYTESIA_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseStorePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    
    jvm("desktop")
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
            }
        }
    }
}

android {
    namespace = "com.biglexj.elytesia"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.biglexj.elytesia"
        minSdk = 24
        targetSdk = 34
        versionCode = elytesiaVersionCode
        versionName = elytesiaVersionName
    }

    val permanentReleaseSigning = if (hasReleaseSigning) {
        signingConfigs.create("permanentRelease") {
            storeFile = rootProject.file(requireNotNull(releaseStorePath))
            storePassword = requireNotNull(releaseStorePassword)
            keyAlias = requireNotNull(releaseKeyAlias)
            keyPassword = requireNotNull(releaseKeyPassword)
        }
    } else {
        null
    }

    buildTypes.getByName("release") {
        permanentReleaseSigning?.let { signingConfig = it }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.desktop {
    application {
        mainClass = "com.biglexj.elytesia.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
            )
            packageName = "ElyTesia"
            packageVersion = elytesiaVersionName
            vendor = "biglexj"

            windows {
                iconFile.set(project.file("src/desktopMain/resources/elytesia.ico"))
                msiPackageVersion = elytesiaVersionName
                exePackageVersion = elytesiaVersionName
                upgradeUuid = "28da0bdd-0ec9-3096-9fe8-2c59b53ec0ab"
                shortcut = true
                menu = true
                menuGroup = "Ely-Tesia"
                dirChooser = true
                perUserInstall = true
            }
        }
    }
}

val copyMidiDemosTask = tasks.register<Copy>("copyMidiDemos") {
    from(project.rootDir.resolve("midi_demos"))
    into(project.projectDir.resolve("src/commonMain/composeResources/files"))
    include("**/*")
}

tasks.configureEach {
    if (name != "copyMidiDemos") {
        dependsOn(copyMidiDemosTask)
    }
}
