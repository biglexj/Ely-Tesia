plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

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
        versionCode = 4
        versionName = "1.0.3"
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
            packageVersion = "1.0.3"
            vendor = "biglexj"

            windows {
                iconFile.set(project.file("src/desktopMain/resources/elytesia.ico"))
                msiPackageVersion = "1.0.3"
                exePackageVersion = "1.0.3"
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
