import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

// Signing credentials live in android/key.properties, which is gitignored.
// CI writes it from GitHub secrets before building; it does not exist on a
// developer machine, so every lookup below is guarded by exists().
val keystorePropertiesFile = rootProject.file("key.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.mypet360.app"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // AGP 8+ disables generated resource values by default. The flavors below
    // use resValue() to set app_name per environment, so it must be enabled.
    buildFeatures {
        resValues = true
    }

    defaultConfig {
        applicationId = "com.mypet360.app"
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    // A flavor dimension is a category of variants. We only need one: which
    // environment the build targets.
    flavorDimensions += "env"

    productFlavors {
        create("dev") {
            dimension = "env"
            // Flavor is named "dev" because AGP forbids flavor names starting
            // with "test". The suffix stays ".test" so the applicationId is
            // com.mypet360.app.test — the package registered in the
            // mypet360-test Firebase project. Different applicationIds are what
            // let TEST and PROD sit side by side on the same device.
            applicationIdSuffix = ".test"
            resValue("string", "app_name", "MyPet360 TEST")
        }
        create("prod") {
            dimension = "env"
            resValue("string", "app_name", "MyPet360")
        }
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }

    buildTypes {
        release {
            // Use the real signing key when CI has provided one, otherwise fall
            // back to debug keys so `flutter run --release` works locally.
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

flutter {
    source = "../.."
}
